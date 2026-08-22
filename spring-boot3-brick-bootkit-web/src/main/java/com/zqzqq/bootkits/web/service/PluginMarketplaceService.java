/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.zqzqq.bootkits.web.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import com.zqzqq.bootkits.web.dto.MarketplacePluginDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件市场 Web 服务。
 * <p>
 * 从配置的索引（本地 JSON 文件或远程 URL）读取可用插件清单，
 * 提供下载到上传临时目录并复用现有安装流程的能力。
 * <p>
 * 索引格式（JSON 数组）：
 * <pre>
 * [ { "pluginId": "demo-plugin", "name": "Demo", "version": "1.0.0",
 *     "description": "...", "downloadUrl": "https://.../demo.jar" } ]
 * </pre>
 * 索引来源：{@code plugin.marketplace.indexUrl}（远程 URL），
 * 未配置时回退到 classpath 下 {@code marketplace/index.json}。
 */
@Slf4j
@Service
public class PluginMarketplaceService {

    private static final String DEFAULT_INDEX_RESOURCE = "marketplace/index.json";

    private final ObjectProvider<PluginManager> pluginManagerProvider;
    private final ObjectProvider<PluginWebService> pluginWebServiceProvider;
    private final BrickWebProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile List<MarketplacePluginDTO> indexCache;

    public PluginMarketplaceService(ObjectProvider<PluginManager> pluginManagerProvider,
                                    ObjectProvider<PluginWebService> pluginWebServiceProvider,
                                    BrickWebProperties properties) {
        this.pluginManagerProvider = pluginManagerProvider;
        this.pluginWebServiceProvider = pluginWebServiceProvider;
        this.properties = properties;
    }

    /**
     * 获取插件市场清单（含已安装状态标记）
     */
    public List<MarketplacePluginDTO> listMarketplace() {
        List<MarketplacePluginDTO> plugins = loadIndex();
        if (plugins.isEmpty()) {
            return plugins;
        }

        PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
        Map<String, PluginInfo> installed = new LinkedHashMap<>();
        if (pluginManager != null) {
            for (PluginInfo info : pluginManager.getPlugins()) {
                installed.put(info.getPluginId(), info);
            }
        }

        for (MarketplacePluginDTO plugin : plugins) {
            PluginInfo info = installed.get(plugin.getPluginId());
            if (info != null) {
                plugin.setInstalled(true);
                plugin.setState(info.getPluginState() == null ? "UNKNOWN" : info.getPluginState().name());
            } else {
                plugin.setInstalled(false);
                plugin.setState("未安装");
            }
        }
        return plugins;
    }

    /**
     * 下载并安装市场插件
     *
     * @param pluginId 插件 ID
     * @param autoStart 安装后是否自动启动
     * @return 安装结果消息
     */
    public String installFromMarketplace(String pluginId, boolean autoStart) {
        MarketplacePluginDTO target = null;
        for (MarketplacePluginDTO plugin : loadIndex()) {
            if (pluginId.equals(plugin.getPluginId())) {
                target = plugin;
                break;
            }
        }
        if (target == null) {
            throw new PluginException("插件市场不存在该插件: " + pluginId);
        }
        if (!StringUtils.hasText(target.getDownloadUrl())) {
            throw new PluginException("插件缺少下载地址: " + pluginId);
        }

        Path downloaded = downloadPlugin(target);
        log.info("插件市场下载完成: {} -> {}", pluginId, downloaded);

        PluginWebService pluginWebService = pluginWebServiceProvider.getIfAvailable();
        if (pluginWebService != null) {
            pluginWebService.installPlugin(downloaded);
            if (autoStart) {
                PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
                if (pluginManager != null) {
                    try {
                        pluginManager.start(pluginId);
                    } catch (Exception e) {
                        log.warn("市场插件自动启动失败: {}", pluginId, e);
                    }
                }
            }
            return "插件安装成功: " + pluginId;
        }

        // 无 PluginWebService 时退化为仅下载到临时目录
        return "插件已下载: " + downloaded;
    }

    /**
     * 加载市场索引
     */
    private List<MarketplacePluginDTO> loadIndex() {
        if (indexCache != null) {
            return indexCache;
        }
        synchronized (this) {
            if (indexCache != null) {
                return indexCache;
            }
            List<MarketplacePluginDTO> plugins = doLoadIndex();
            indexCache = plugins;
            return plugins;
        }
    }

    private List<MarketplacePluginDTO> doLoadIndex() {
        String indexUrl = properties.getMarketplaceIndexUrl();
        if (StringUtils.hasText(indexUrl)) {
            return loadFromUrl(indexUrl);
        }
        return loadFromClasspath();
    }

    private List<MarketplacePluginDTO> loadFromUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new PluginException("插件市场索引请求失败: HTTP " + response.statusCode());
            }
            try (InputStream in = response.body()) {
                return objectMapper.readValue(in, new TypeReference<List<MarketplacePluginDTO>>() {});
            }
        } catch (IOException e) {
            throw new PluginException("插件市场索引读取失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PluginException("插件市场索引请求被中断", e);
        }
    }

    private List<MarketplacePluginDTO> loadFromClasspath() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(DEFAULT_INDEX_RESOURCE)) {
            if (in == null) {
                log.info("未配置插件市场索引，且 classpath 无 {}，返回空清单", DEFAULT_INDEX_RESOURCE);
                return new ArrayList<>();
            }
            return objectMapper.readValue(in, new TypeReference<List<MarketplacePluginDTO>>() {});
        } catch (IOException e) {
            log.warn("读取内置插件市场索引失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 下载插件 jar 到上传临时目录
     */
    private Path downloadPlugin(MarketplacePluginDTO plugin) {
        try {
            String tempPathStr = properties.getUploadTempPath();
            Path uploadTempPath = StringUtils.hasText(tempPathStr)
                    ? Paths.get(tempPathStr) : Paths.get("upload_temp");
            Files.createDirectories(uploadTempPath);

            String filename = plugin.getPluginId() + "-" + plugin.getVersion() + ".jar";
            Path target = uploadTempPath.resolve(filename).normalize();
            if (!target.startsWith(uploadTempPath)) {
                throw new PluginException("插件市场下载路径不合法");
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(plugin.getDownloadUrl()))
                    .timeout(Duration.ofMinutes(2))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new PluginException("插件下载失败: HTTP " + response.statusCode());
            }
            try (InputStream in = response.body()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException e) {
            throw new PluginException("插件下载失败: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PluginException("插件下载被中断", e);
        }
    }
}
