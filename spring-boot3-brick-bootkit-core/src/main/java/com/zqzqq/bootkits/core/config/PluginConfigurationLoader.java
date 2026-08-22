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
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * 插件配置加载器
 * 
 * @author zqzqq
 * @since 4.1.0
 */
@Slf4j
public class PluginConfigurationLoader {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PluginConfigurationLoader.class);
    
    private static final Yaml yaml = new Yaml();
    
    /**
     * 从文件加载配置
     */
    public static PluginConfiguration load(Path configFile) throws IOException {
        if (!Files.exists(configFile)) {
            throw new IOException("Configuration file not found: " + configFile);
        }
        
        String fileName = configFile.getFileName().toString();
        String pluginId = fileName.substring(0, fileName.lastIndexOf('.'));
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        
        PluginConfiguration config = new PluginConfiguration(pluginId);
        
        try {
            switch (extension) {
                case "yml":
                case "yaml":
                    loadYamlConfiguration(config, configFile);
                    break;
                case "properties":
                    loadPropertiesConfiguration(config, configFile);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported configuration file format: " + extension);
            }
            
            // 设置元数据
            config.getMetadata().setSource(configFile.toString());
            config.getMetadata().setFormat(extension);
            
            log.debug("Loaded configuration for plugin: {} from {}", pluginId, configFile);
            return config;
            
        } catch (Exception e) {
            log.error("Failed to load configuration from file: " + configFile, e);
            throw new IOException("Failed to load configuration", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private static void loadYamlConfiguration(PluginConfiguration config, Path configFile) throws IOException {
        String content = Files.readString(configFile);
        Map<String, Object> yamlData = yaml.load(content);
        
        if (yamlData != null) {
            // 处理插件基本信息
            if (yamlData.containsKey("plugin")) {
                Map<String, Object> pluginInfo = (Map<String, Object>) yamlData.get("plugin");
                if (pluginInfo.containsKey("version")) {
                    config.setVersion(pluginInfo.get("version").toString());
                }
                if (pluginInfo.containsKey("description")) {
                    config.setDescription(pluginInfo.get("description").toString());
                }
                if (pluginInfo.containsKey("enabled")) {
                    config.setEnabled(Boolean.parseBoolean(pluginInfo.get("enabled").toString()));
                }
                if (pluginInfo.containsKey("environment")) {
                    config.setEnvironment(pluginInfo.get("environment").toString());
                }
            }
            
            // 处理配置属性
            if (yamlData.containsKey("properties")) {
                Map<String, Object> properties = (Map<String, Object>) yamlData.get("properties");
                config.getProperties().putAll(properties);
            }
            
            // 处理元数据
            if (yamlData.containsKey("metadata")) {
                Map<String, Object> metadata = (Map<String, Object>) yamlData.get("metadata");
                if (metadata.containsKey("tags")) {
                    Map<String, String> tags = (Map<String, String>) metadata.get("tags");
                    config.getMetadata().getTags().putAll(tags);
                }
                if (metadata.containsKey("sensitiveProperties")) {
                    java.util.List<String> sensitiveProps = (java.util.List<String>) metadata.get("sensitiveProperties");
                    config.getMetadata().getSensitiveProperties().addAll(sensitiveProps);
                }
            }
        }
    }
    
    private static void loadPropertiesConfiguration(PluginConfiguration config, Path configFile) throws IOException {
        Properties props = new Properties();
        props.load(Files.newInputStream(configFile));
        
        // 处理插件基本信息
        if (props.containsKey("plugin.version")) {
            config.setVersion(props.getProperty("plugin.version"));
        }
        if (props.containsKey("plugin.description")) {
            config.setDescription(props.getProperty("plugin.description"));
        }
        if (props.containsKey("plugin.enabled")) {
            config.setEnabled(Boolean.parseBoolean(props.getProperty("plugin.enabled")));
        }
        if (props.containsKey("plugin.environment")) {
            config.setEnvironment(props.getProperty("plugin.environment"));
        }
        
        // 处理配置属性（以properties.开头的属性）
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("properties.")) {
                String propertyKey = key.substring("properties.".length());
                config.getProperties().put(propertyKey, props.getProperty(key));
            }
        }
        
        // 处理标签（以metadata.tags.开头的属性）
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith("metadata.tags.")) {
                String tagKey = key.substring("metadata.tags.".length());
                config.getMetadata().getTags().put(tagKey, props.getProperty(key));
            }
        }
    }
}