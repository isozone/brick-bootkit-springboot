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


package com.zqzqq.bootkits.core.config;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 插件配置持久化器
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@Slf4j
public class PluginConfigurationPersister {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginConfigurationPersister.class);
    
    private static final Yaml yaml;
    
    static {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        yaml = new Yaml(options);
    }
    
    /**
     * 保存配置到文件
     */
    public static void save(PluginConfiguration config, Path configFile) throws IOException {
        try {
            // 确保目录存在
            Path parent = configFile.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            
            String extension = getFileExtension(configFile);
            
            switch (extension.toLowerCase()) {
                case "yml":
                case "yaml":
                    saveAsYaml(config, configFile);
                    break;
                case "properties":
                    saveAsProperties(config, configFile);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported configuration file format: " + extension);
            }
            
            log.debug("Saved configuration for plugin: {} to {}", config.getPluginId(), configFile);
            
        } catch (Exception e) {
            log.error("Failed to save configuration to file: " + configFile, e);
            throw new IOException("Failed to save configuration", e);
        }
    }
    
    private static void saveAsYaml(PluginConfiguration config, Path configFile) throws IOException {
        Map<String, Object> yamlData = new LinkedHashMap<>();
        
        // 插件基本信息
        Map<String, Object> pluginInfo = new LinkedHashMap<>();
        if (config.getVersion() != null) {
            pluginInfo.put("version", config.getVersion());
        }
        if (config.getDescription() != null) {
            pluginInfo.put("description", config.getDescription());
        }
        pluginInfo.put("enabled", config.isEnabled());
        if (config.getEnvironment() != null) {
            pluginInfo.put("environment", config.getEnvironment());
        }
        yamlData.put("plugin", pluginInfo);
        
        // 配置属性
        if (!config.getProperties().isEmpty()) {
            yamlData.put("properties", config.getProperties());
        }
        
        // 元数据
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (!config.getMetadata().getTags().isEmpty()) {
            metadata.put("tags", config.getMetadata().getTags());
        }
        if (!config.getMetadata().getSensitiveProperties().isEmpty()) {
            metadata.put("sensitiveProperties", config.getMetadata().getSensitiveProperties());
        }
        if (!metadata.isEmpty()) {
            yamlData.put("metadata", metadata);
        }
        
        String yamlContent = yaml.dump(yamlData);
        Files.writeString(configFile, yamlContent);
    }
    
    private static void saveAsProperties(PluginConfiguration config, Path configFile) throws IOException {
        java.util.Properties props = new java.util.Properties();
        
        // 插件基本信息
        if (config.getVersion() != null) {
            props.setProperty("plugin.version", config.getVersion());
        }
        if (config.getDescription() != null) {
            props.setProperty("plugin.description", config.getDescription());
        }
        props.setProperty("plugin.enabled", String.valueOf(config.isEnabled()));
        if (config.getEnvironment() != null) {
            props.setProperty("plugin.environment", config.getEnvironment());
        }
        
        // 配置属性
        for (Map.Entry<String, Object> entry : config.getProperties().entrySet()) {
            props.setProperty("properties." + entry.getKey(), entry.getValue().toString());
        }
        
        // 标签
        for (Map.Entry<String, String> entry : config.getMetadata().getTags().entrySet()) {
            props.setProperty("metadata.tags." + entry.getKey(), entry.getValue());
        }
        
        // 保存到文件
        try (var writer = Files.newBufferedWriter(configFile)) {
            props.store(writer, "Plugin Configuration for " + config.getPluginId());
        }
    }
    
    private static String getFileExtension(Path file) {
        String fileName = file.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
}