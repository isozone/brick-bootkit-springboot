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



package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.checker.ComposePluginBasicChecker;
import com.zqzqq.bootkits.core.checker.PluginBasicChecker;
import com.zqzqq.bootkits.core.descriptor.ComposeDescriptorLoader;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptorLoader;
import com.zqzqq.bootkits.core.scanner.BasePluginScanner;
import com.zqzqq.bootkits.core.scanner.DevPathResolve;
import com.zqzqq.bootkits.core.scanner.PathResolve;
import com.zqzqq.bootkits.core.scanner.PluginScanner;
import com.zqzqq.bootkits.core.scanner.ProdPathResolve;
import com.zqzqq.bootkits.core.version.SemverVersionInspector;
import com.zqzqq.bootkits.core.version.VersionInspector;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.utils.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.net.URI;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * 默认的RealizeProvider实现
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.1
 */
public class DefaultRealizeProvider implements RealizeProvider {

    private static final Logger log = LoggerFactory.getLogger(DefaultRealizeProvider.class);

    private PluginScanner pluginScanner;
    private PluginBasicChecker pluginBasicChecker;
    private PluginDescriptorLoader pluginDescriptorLoader;
    private VersionInspector versionInspector;

    protected final IntegrationConfiguration configuration;
    protected final ApplicationContext applicationContext;

    /**
     * 获取 jar 所在目录（缓存结果，避免重复计算）
     */
    private String cachedRootPath;

    public DefaultRealizeProvider(IntegrationConfiguration configuration,
                                  ApplicationContext applicationContext){
        this.configuration = Assert.isNotNull(configuration, "参数 configuration 不能为空");
        this.applicationContext = Assert.isNotNull(applicationContext, "参数 configuration 不能为空");
    }

    @Override
    public void init() {
        PathResolve pathResolve = configuration.environment() == RuntimeMode.DEV
                ? new DevPathResolve()
                : new ProdPathResolve();
        BasePluginScanner basePluginScanner = new BasePluginScanner(pathResolve, getRootPath());
        setPluginScanner(basePluginScanner);
        setPluginBasicChecker(new ComposePluginBasicChecker(applicationContext));
        setPluginDescriptorLoader(new ComposeDescriptorLoader(applicationContext, pluginBasicChecker));
        setVersionInspector(new SemverVersionInspector());
    }

    /**
     * 获取 jar 所在目录
     * @return jar 所在目录绝对路径
     */
    protected String getRootPath() {
        if (cachedRootPath != null) {
            return cachedRootPath;
        }
        try {
            ProtectionDomain protectionDomain = getClass().getProtectionDomain();
            if (protectionDomain != null) {
                CodeSource codeSource = protectionDomain.getCodeSource();
                if (codeSource != null) {
                    URI location = codeSource.getLocation().toURI();
                    String path = location.getSchemeSpecificPart();
                    if (path != null) {
                        File root = new File(path);
                        if (root.exists()) {
                            cachedRootPath = root.isFile() ? root.getParent() : root.getAbsolutePath();
                            log.debug("解析 jar 所在目录: {}", cachedRootPath);
                            return cachedRootPath;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取 jar 所在目录失败，使用当前工作目录: {}", e.getMessage());
        }
        cachedRootPath = System.getProperty("user.dir");
        log.debug("使用当前工作目录作为插件根目录: {}", cachedRootPath);
        return cachedRootPath;
    }

    public void setPluginScanner(PluginScanner pluginScanner) {
        this.pluginScanner = Assert.isNotNull(pluginScanner, "pluginScanner 不能为空");
    }

    public void setPluginBasicChecker(PluginBasicChecker pluginBasicChecker) {
        this.pluginBasicChecker =  Assert.isNotNull(pluginBasicChecker,
                "pluginBasicChecker 不能为空");
    }

    public void setPluginDescriptorLoader(PluginDescriptorLoader pluginDescriptorLoader) {
        this.pluginDescriptorLoader = Assert.isNotNull(pluginDescriptorLoader,
                "pluginDescriptorLoader 不能为空");
    }

    public void setVersionInspector(VersionInspector versionInspector) {
        this.versionInspector = Assert.isNotNull(versionInspector, "versionInspector 不能为空");
    }

    @Override
    public RuntimeMode getRuntimeMode() {
        return configuration.environment();
    }

    @Override
    public PluginScanner getPluginScanner() {
        return Assert.isNotNull(pluginScanner, "PluginScanner 实现不能为空");
    }

    @Override
    public PluginBasicChecker getPluginBasicChecker() {
        return Assert.isNotNull(pluginBasicChecker, "pluginBasicChecker 实现不能为空");
    }

    @Override
    public PluginDescriptorLoader getPluginDescriptorLoader() {
        return Assert.isNotNull(pluginDescriptorLoader, "PluginDescriptorLoader 实现不能为空");
    }

    @Override
    public VersionInspector getVersionInspector() {
        return Assert.isNotNull(versionInspector, "VersionInspector 实现不能为空");
    }
}