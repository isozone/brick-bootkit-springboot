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

    public PluginStaticResourceResolver(PluginStaticResourceConfig config) {
        this.config = config;
    }


    @Override
    protected Resource resolveResourceInternal(HttpServletRequest request,
                                               String requestPath, List<? extends Resource> locations,
                                               ResourceResolverChain chain) {
        if(request != null){
            String requestUri = request.getRequestURI();
            String formatUri = UrlUtils.format(requestUri);
            // fix https://gitee.com/starblues/springboot-plugin-framework-parent/issues/I53T9W
            requestPath = UrlUtils.format(formatUri.replaceFirst(config.getPathPrefix(), ""));
        }
        int startOffset = requestPath.indexOf("/");
        String pluginId = null;
        String partialPath = null;
        if (startOffset == -1) {
            pluginId = requestPath;
            partialPath = config.getIndexPageName();
        } else {
            pluginId = requestPath.substring(0, startOffset);
            partialPath = requestPath.substring(startOffset + 1);
        }

        PluginStaticResource pluginResource = PLUGIN_RESOURCE_MAP.get(pluginId);

        if(pluginResource == null){
            return chain.resolveResource(request, requestPath, locations);
        }

        String key = computeKey(request, requestPath);
        // 先不垽鏂紦瀛樹腑鏄惁存在锟?
        Resource resource = pluginResource.getCacheResource(key);
        if(resource != null){
            return resource;
        }
        resource = findResource(pluginResource, partialPath);
        if(resource != null){
            pluginResource.putCacheResource(key, resource);
            return resource;
        } else {
            // 尝试获取棣栭〉椤甸潰
            String indexPageName = config.getIndexPageName();
            if(ObjectUtils.isEmpty(indexPageName)){
                indexPageName = PluginStaticResourceConfig.DEFAULT_INDEX_PAGE_NAME;
            }
            if(partialPath.lastIndexOf(".") > -1){
                // 存在鍚庣紑
                return null;
            }

            // 鏌ユ壘绗竴绾ц妭鐐癸紝鎵句笉鍒板垯璇诲彇鏍筰ndex.html
            if(partialPath.contains(UrlUtils.PATH_SEPARATOR)){
                partialPath = partialPath.substring(0, partialPath.indexOf(UrlUtils.PATH_SEPARATOR));
            }
            // 绗竴绾ц妭锟?
            resource = findResource(pluginResource, UrlUtils.joiningUrlPath(partialPath, indexPageName));
            if(resource != null){
                return resource;
            }
            // 鏍硅妭锟?
            return findResource(pluginResource, UrlUtils.joiningUrlPath(UrlUtils.PATH_SEPARATOR, indexPageName));
        }
    }

    private Resource findResource(PluginStaticResource pluginResource, String partialPath){
        // 浠巆lasspath 获取资源
        Resource resource = resolveClassPath(pluginResource, partialPath);
        if(resource != null){
            return resource;
        }

        // 浠庡缃枃浠惰矾寰勮幏鍙栬祫锟?
        resource = resolveFilePath(pluginResource, partialPath);
        if(resource != null){
            return resource;
        }
        return resource;
    }

    /**
     * 瑙ｅ喅 ClassPath 的资源婧愭枃浠躲€備篃就是插件涓畾涔夌殑  classpath:/xx/xx/ 配置
     * @param pluginResource 插件资源配置Bean
     * @param partialPath 閮ㄥ垎璺緞
     * @return 资源銆傛病鏈夊彂鐜板垯杩斿洖null
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
                    // 纭繚资源涓烘枃锟?
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
     * 瑙ｅ喅插件涓厤缃殑缁濆文件璺緞鐨勬枃浠惰祫婧愩€備篃就是插件涓畾涔夌殑  file:D://xx/xx/ 配置
     * @param pluginResource 插件资源配置Bean
     * @param partialPath 閮ㄥ垎璺緞
     * @return 资源銆傛病鏈夊彂鐜板垯杩斿洖null
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
     * 璁＄畻key
     * @param request request
     * @param requestPath 璇锋眰璺緞
     * @return 杩斿洖key
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
     * 根据璇锋眰获取内容code key
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
     * 姣忔柊澧炰竴涓彃锟? 閮介渶瑕佽皟鐢ㄨ方法锛屾潵解析璇ユ彃浠剁殑 StaticResourceConfig 配置銆傚苟灏嗗叾淇濆瓨锟?StaticResourceConfig bean 涓拷?
     * @param pluginDescriptor 插件淇℃伅
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
                logger.warn("插件[{}]配置鐨勯潤鎬佽祫婧愭牸寮忛敊锟? {}",
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
                logger.warn("插件[{}]配置鐨勯潤鎬佽祫婧愮被鍨嬩笉鑳借瘑锟? {}", MsgUtils.getPluginUnique(pluginDescriptor), type);
            }
        }

        PluginStaticResource pluginResource = new PluginStaticResource();
        pluginResource.setClassPaths(classPaths);
        pluginResource.setFilePaths(filePaths);
        pluginResource.setPluginDescriptor(pluginDescriptor);
        pluginResource.setPluginClassLoader(pluginClassLoader);

        logger.info("插件[{}]配置鐨勯潤鎬佽祫锟? classpath[{}], file[{}]", MsgUtils.getPluginUnique(pluginDescriptor),
                classPaths, filePaths);

        if(PLUGIN_RESOURCE_MAP.containsKey(pluginId)){
            // 如果存在该插件ID的插件资源信息, 则先移除
            remove(pluginId);
        }
        PLUGIN_RESOURCE_MAP.put(pluginId, pluginResource);
    }



    /**
     * 卸载插件鏃躲€傝皟鐢ㄨ方法移除插件的资源婧愪俊锟?
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
     * 插件资源解析鍚庣殑淇℃伅
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
         * 瀹氫箟鐨刢lasspath集合
         */
        private Set<String> classPaths;

        /**
         * 瀹氫箟鐨勬枃浠惰矾寰勯泦锟?
         */
        private Set<String> filePaths;

        /**
         * 缓存的资源婧愩€俴ey 涓鸿祫婧愮殑鍙互銆傚€间负资源
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


        Resource getCacheResource(String key){
            return cacheResourceMaps.get(key);
        }

        void putCacheResource(String key, Resource resource){
            if(StringUtils.isEmpty(key) || resource == null){
                return;
            }
            cacheResourceMaps.put(key, resource);
        }
    }

}




