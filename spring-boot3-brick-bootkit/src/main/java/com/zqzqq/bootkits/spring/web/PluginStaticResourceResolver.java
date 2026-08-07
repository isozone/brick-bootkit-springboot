/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.spring.web;

import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import com.zqzqq.bootkits.spring.WebConfig;
import com.zqzqq.bootkits.utils.MsgUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.UrlUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 插件web静态资源Resolver
 * @author starBlues
 * @version 3.0.2
 */
public class PluginStaticResourceResolver extends AbstractResourceResolver {

    private final static Logger logger = LoggerFactory.getLogger(PluginStaticResourceResolver.class);

    private final static String RESOLVED_RESOURCE_CACHE_KEY_PREFIX = "resolvedPluginResource:";

    private final static Map<String, PluginStaticResource> PLUGIN_RESOURCE_MAP = new ConcurrentHashMap<>();

    private final PluginStaticResourceConfig config;
    private final PluginResourcePathParser pathParser;

    public PluginStaticResourceResolver(PluginStaticResourceConfig config) {
        this.config = config;
        this.pathParser = new PluginResourcePathParser(config);
    }


    @Override
    protected Resource resolveResourceInternal(HttpServletRequest request,
                                               String requestPath, List<? extends Resource> locations,
                                               ResourceResolverChain chain) {
        // 入参 requestPath 是 Spring 解析后的路径, 已去掉 context-path, 格式形如
        // "plugins/plugin1/index.html"。直接基于该入参去除 pathPrefix 前缀即可,
        // 不应再通过 request.getRequestURI() 重新解析, 否则在主服务配置了
        // server.servlet.context-path 时会得到错误的 requestPath。
        // fix https://gitee.com/starblues/springboot-plugin-framework-parent/issues/I53T9W
        PluginResourcePathParser.ParseResult parsed = pathParser.parse(requestPath);
        String pluginId = parsed.getPluginId();
        String partialPath = parsed.getPartialPath();

        // 空路径或无法解析出 pluginId: 直接交由 chain 兜底, 不污染缓存
        if (pluginId == null || pluginId.isEmpty()) {
            return chain.resolveResource(request, requestPath, locations);
        }

        if (logger.isDebugEnabled()) {
            logger.debug("插件静态资源解析: requestPath=[{}], pluginId=[{}], partialPath=[{}]",
                    requestPath, pluginId, partialPath);
        }

        PluginStaticResource pluginResource = PLUGIN_RESOURCE_MAP.get(pluginId);

        if(pluginResource == null){
            if (logger.isDebugEnabled()) {
                logger.debug("未找到插件[{}]的静态资源配置, 交由 chain 处理: requestPath=[{}]",
                        pluginId, requestPath);
            }
            return chain.resolveResource(request, requestPath, locations);
        }

        // 缓存 key 用归一化后的 pluginId/partialPath, 保证 put/get 一致
        String normalizedPath = pluginId + UrlUtils.PATH_SEPARATOR + partialPath;
        String key = computeKey(request, normalizedPath);
        // null-sentinel: 缓存命中直接返回; 命中 null-sentinel 返回 null 避免 404 反复扫描
        Resource resource = pluginResource.getCacheResource(key);
        if(PluginStaticResource.isNullSentinel(resource)){
            return null;
        }
        if(resource != null){
            return resource;
        }
        resource = findResource(pluginResource, partialPath);
        if(resource != null){
            pluginResource.putCacheResource(key, resource);
            return resource;
        } else {
            // 尝试获取首页页面
            String indexPageName = config.getIndexPageName();
            if(ObjectUtils.isEmpty(indexPageName)){
                indexPageName = PluginStaticResourceConfig.DEFAULT_INDEX_PAGE_NAME;
            }
            if(partialPath.lastIndexOf(".") > -1){
                // 存在后缀, 缓存 null 标记
                pluginResource.putCacheResource(key, null);
                return null;
            }

            // 寻找第一个节点, 找不到则读取根目录index.html
            if(partialPath.contains(UrlUtils.PATH_SEPARATOR)){
                partialPath = partialPath.substring(0, partialPath.indexOf(UrlUtils.PATH_SEPARATOR));
            }
            // 第一级节点?
            resource = findResource(pluginResource, UrlUtils.joiningUrlPath(partialPath, indexPageName));
            if(resource != null){
                return resource;
            }
            // 根节点?
            return findResource(pluginResource, UrlUtils.joiningUrlPath(UrlUtils.PATH_SEPARATOR, indexPageName));
        }
    }

    private Resource findResource(PluginStaticResource pluginResource, String partialPath){
        // 从classpath 获取资源
        Resource resource = resolveClassPath(pluginResource, partialPath);
        if(resource != null){
            return resource;
        }
        // 从外部文件路径获取资源?
        return resolveFilePath(pluginResource, partialPath);
    }

    /**
     * 解决 ClassPath 的资源路径文件。也就是插件中定义的  classpath:/xx/xx/ 配置
     * @param pluginResource 插件资源配置Bean
     * @param partialPath 部分路径
     * @return 资源。没有发现则返回null
     */
    private Resource resolveClassPath(PluginStaticResource pluginResource, String partialPath){
        Set<String> classPaths = pluginResource.getClassPaths();
        if(classPaths == null || classPaths.isEmpty()){
            return null;
        }

        ClassLoader pluginClassLoader = pluginResource.getPluginClassLoader();
        for (String classPath : classPaths) {
            try {
                PluginResource resource = new PluginResource(classPath + partialPath, pluginResource.getPluginDescriptor());
                resource.setClassLoader(pluginClassLoader);
                if(resource.exists()){
                    // 确保资源为文件?
                    File file = resource.getFile();
                    if(file != null && file.isFile()){
                        return resource;
                    }
                }
            } catch (Exception e){
                logger.debug("Get static resources of classpath '{}' error.", classPath, e);
            }
        }
        return null;
    }

    /**
     * 解决插件中配置的绝对文件路径的文件资源。也就是插件中定义的  file:D://xx/xx/ 配置
     * @param pluginResource 插件资源配置Bean
     * @param partialPath 部分路径
     * @return 资源。没有发现则返回null
     */
    private Resource resolveFilePath(PluginStaticResource pluginResource, String partialPath) {
        Set<String> filePaths = pluginResource.getFilePaths();
        if(filePaths == null || filePaths.isEmpty()){
            return null;
        }

        for (String filePath : filePaths) {
            Path fullPath = Paths.get(filePath + partialPath);
            if(!Files.exists(fullPath)){
                continue;
            }
            try {
                FileUrlResource fileUrlResource = new FileUrlResource(fullPath.toString());
                if(fileUrlResource.exists()){
                    return fileUrlResource;
                }
            } catch (Exception e) {
                logger.debug("Get static resources of path '{}' error.", fullPath, e);
            }
        }
        return null;
    }


    @Override
    protected String resolveUrlPathInternal(String resourceUrlPath,
                                            List<? extends Resource> locations,
                                            ResourceResolverChain chain) {
        return null;
    }

    /**
     * 计算 key
     * @param request request
     * @param requestPath 请求路径
     * @return 返回key
     */
    protected String computeKey(HttpServletRequest request, String requestPath) {
        StringBuilder key = new StringBuilder(RESOLVED_RESOURCE_CACHE_KEY_PREFIX);
        key.append(requestPath);
        if (request != null) {
            String codingKey = getContentCodingKey(request);
            if (ObjectUtils.hasText(codingKey)) {
                key.append("+encoding=").append(codingKey);
            }
        }
        return key.toString();
    }

    /**
     * 根据请求获取内容编码key
     * @param request request
     * @return key
     */
    private String getContentCodingKey(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
        if (!ObjectUtils.hasText(header)) {
            return null;
        }
        return Arrays.stream(StringUtils.tokenizeToStringArray(header, ","))
                .map(token -> {
                    int index = token.indexOf(';');
                    return (index >= 0 ? token.substring(0, index) : token).trim().toLowerCase();
                })
                .sorted()
                .collect(Collectors.joining(","));
    }



    /**
     * 每当新增一个插件，都必须调用该方法解析 StaticResourceConfig，并保存到配置缓存中
     * @param pluginDescriptor 插件信息
     * @param pluginClassLoader 插件classloader
     * @param webConfig web配置
     */
    public static synchronized void parse(PluginDescriptor pluginDescriptor,
                                          ClassLoader pluginClassLoader,
                                          WebConfig webConfig){
        if(webConfig == null || !webConfig.isEnable()){
            return;
        }
        final Set<String> locations = webConfig.getResourceLocations();
        if(ObjectUtils.isEmpty(locations)){
            return;
        }

        Set<String> classPaths = new HashSet<>();
        Set<String> filePaths = new HashSet<>();

        String pluginId = pluginDescriptor.getPluginId();

        for (String location : locations) {
            if(ObjectUtils.isEmpty(location)){
                continue;
            }
            final int first = location.indexOf(":");
            if(first == -1){
                logger.warn("插件[{}]配置的静态资源格式错误: {}",
                        MsgUtils.getPluginUnique(pluginDescriptor), location);
                continue;
            }
            String type = location.substring(0, first);
            String path = location.substring(first+1);

            if("classpath".equalsIgnoreCase(type)){
                if(path.startsWith("/")){
                    path = path.substring(1);
                }
                if(!path.endsWith("/")){
                    path =  path + "/";
                }
                classPaths.add(path);
            } else if("file".equalsIgnoreCase(type)){
                if(!path.endsWith(File.separator)){
                    path = path + File.separator;
                }
                filePaths.add(path);
            } else {
                logger.warn("插件[{}]配置的静态资源类型不能识别: {}", MsgUtils.getPluginUnique(pluginDescriptor), type);
            }
        }

        PluginStaticResource pluginResource = new PluginStaticResource();
        pluginResource.setClassPaths(classPaths);
        pluginResource.setFilePaths(filePaths);
        pluginResource.setPluginDescriptor(pluginDescriptor);
        pluginResource.setPluginClassLoader(pluginClassLoader);

        logger.info("插件[{}]配置的静态资源: classpath[{}], file[{}]", MsgUtils.getPluginUnique(pluginDescriptor),
                classPaths, filePaths);

        if(PLUGIN_RESOURCE_MAP.containsKey(pluginId)){
            // 如果存在该插件ID的插件资源信息, 则先移除
            remove(pluginId);
        }
        PLUGIN_RESOURCE_MAP.put(pluginId, pluginResource);
    }



    /**
     * 卸载插件时。调用该方法移除插件的资源信息
     * @param pluginId 插件id
     */
    public static synchronized void remove(String pluginId){
        PluginStaticResource pluginResource = PLUGIN_RESOURCE_MAP.get(pluginId);
        if(pluginResource == null){
            return;
        }
        PLUGIN_RESOURCE_MAP.remove(pluginId);
    }

    /**
     * 插件资源解析后的信息
     */
    private static class PluginStaticResource {

        /**
         * basePlugin bean
         */
        private PluginDescriptor pluginDescriptor;

        /**
         * 插件classloader
         */
        private ClassLoader pluginClassLoader;

        /**
         * 定义的classpath集合
         */
        private Set<String> classPaths;

        /**
         * 定义的文件路径集合
         */
        private Set<String> filePaths;

        /**
         * 缓存的资源。key 为资源的可以。键值为资源
         */
        private final Map<String, Resource> cacheResourceMaps = new ConcurrentHashMap<>();

        PluginDescriptor getPluginDescriptor() {
            return pluginDescriptor;
        }

        void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
            this.pluginDescriptor = pluginDescriptor;
        }

        ClassLoader getPluginClassLoader() {
            return pluginClassLoader;
        }

        void setPluginClassLoader(ClassLoader pluginClassLoader) {
            this.pluginClassLoader = pluginClassLoader;
        }

        Set<String> getClassPaths() {
            return classPaths;
        }

        void setClassPaths(Set<String> classPaths) {
            this.classPaths = classPaths;
        }

        Set<String> getFilePaths() {
            return filePaths;
        }

        void setFilePaths(Set<String> filePaths) {
            this.filePaths = filePaths;
        }


        /**
         * null-sentinel: 标记某个 key 已经被解析过但未找到资源 (404),
         * 避免 404 反复扫描。与真正的 Resource 区分开。
         */
        private static final Resource NULL_SENTINEL =
                new org.springframework.core.io.ByteArrayResource(new byte[0], "plugin-null-sentinel");

        /**
         * 取缓存。返回值语义:
         *  - NULL_SENTINEL: 该 key 之前解析过但未找到 (404), 调用方应返回 null
         *  - 其他非 null: 命中真实资源
         *  - null: 该 key 从未解析过
         */
        Resource getCacheResource(String key){
            return cacheResourceMaps.get(key);
        }

        static boolean isNullSentinel(Resource resource) {
            return resource == NULL_SENTINEL;
        }

        void putCacheResource(String key, Resource resource){
            if(StringUtils.isEmpty(key)){
                return;
            }
            if(resource == null){
                // 缓存 null-sentinel, 防止 404 反复扫描
                cacheResourceMaps.put(key, NULL_SENTINEL);
            } else {
                cacheResourceMaps.put(key, resource);
            }
        }
    }

}




