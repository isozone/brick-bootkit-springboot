package com.zqzqq.bootkits.web.demo.config;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * Demo 环境的简化 PluginManager 配置
 * 支持基本的上传和安装功能
 * 
 * @author brick-bootkit
 */
@Slf4j
@Configuration
public class DemoPluginManagerConfig {

    /**
     * Demo 插件状态枚举（兼容 PluginInfo.PluginState）
     */
    public enum PluginState {
        STARTED, STOPPED
    }

    /**
     * Demo 插件信息
     */
    public static class DemoPlugin implements PluginInfo {
        private final String pluginId;
        private final String name;
        private final String version;
        private final String author;
        private final String description;
        private final String pluginPath;
        private PluginState state = PluginState.STOPPED;
        private long startTime = 0;

        public DemoPlugin(String pluginId, String name, String version, String author, 
                         String description, String pluginPath) {
            this.pluginId = pluginId;
            this.name = name;
            this.version = version;
            this.author = author;
            this.description = description;
            this.pluginPath = pluginPath;
        }

        @Override
        public String getPluginId() {
            return pluginId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getVersion() {
            return version;
        }

        @Override
        public String getAuthor() {
            return author;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public PluginState getPluginState() {
            return state;
        }

        @Override
        public long getStartTime() {
            return startTime;
        }

        public void setState(PluginState state) {
            this.state = state;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        @Override
        public String getPluginPath() {
            return pluginPath;
        }

        @Override
        public PluginDescriptor getPluginDescriptor() {
            return null;
        }
    }

    /**
     * 简化的 PluginManager 实现
     */
    public static class DemoPluginManager implements PluginManager {
        private final Map<String, DemoPlugin> installedPlugins = new ConcurrentHashMap<>();
        private final Path pluginsDir;
        private final Path uploadTempPath;

        public DemoPluginManager() {
            this.pluginsDir = Paths.get(System.getProperty("java.io.tmpdir"), "brick-demo-plugins");
            this.uploadTempPath = Paths.get(System.getProperty("java.io.tmpdir"), "brick-upload-temp");
            
            try {
                Files.createDirectories(pluginsDir);
                Files.createDirectories(uploadTempPath);
            } catch (Exception e) {
                log.warn("创建插件目录失败: {}", e.getMessage());
            }
        }

        @Override
        public List<PluginInfo> getPlugins() {
            return new ArrayList<>(installedPlugins.values());
        }

        @Override
        public PluginInfo getPlugin(String pluginId) {
            return installedPlugins.get(pluginId);
        }

        /**
         * 安装插件
         */
        @Override
        public PluginInfo install(Path pluginPath) {
            File pluginFile = pluginPath.toFile();
            try {
                // 解析插件描述符
                PluginDescriptor descriptor = parsePluginDescriptor(pluginFile);
                
                String pluginId = descriptor.getPluginId();
                String name = descriptor.getName();
                String version = descriptor.getVersion();
                String author = descriptor.getAuthor();
                String description = descriptor.getDescription();

                // 复制插件文件到插件目录
                Path targetPath = pluginsDir.resolve(pluginFile.getName());
                Files.copy(pluginFile.toPath(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // 创建插件实例
                DemoPlugin plugin = new DemoPlugin(pluginId, name, version, author, description, targetPath.toString());
                installedPlugins.put(pluginId, plugin);

                log.info("插件安装成功: {} ({} v{})", name, pluginId, version);
                return plugin;

            } catch (Exception e) {
                log.error("插件安装失败: {}", e.getMessage(), e);
                throw new RuntimeException("插件安装失败: " + e.getMessage(), e);
            }
        }

        /**
         * 启动插件
         */
        @Override
        public void start(String pluginId) {
            DemoPlugin plugin = installedPlugins.get(pluginId);
            if (plugin == null) {
                throw new RuntimeException("插件不存在: " + pluginId);
            }

            plugin.setState(PluginState.STARTED);
            plugin.setStartTime(System.currentTimeMillis());
            log.info("插件启动成功: {}", pluginId);
        }

        /**
         * 停止插件
         */
        @Override
        public void stop(String pluginId) {
            DemoPlugin plugin = installedPlugins.get(pluginId);
            if (plugin == null) {
                throw new RuntimeException("插件不存在: " + pluginId);
            }

            plugin.setState(PluginState.STOPPED);
            log.info("插件停止成功: {}", pluginId);
        }

        /**
         * 重启插件（重新加载）
         */
        @Override
        public void reload(String pluginId) {
            stop(pluginId);
            start(pluginId);
            log.info("插件重启成功: {}", pluginId);
        }

        /**
         * 卸载插件
         */
        @Override
        public void uninstall(String pluginId) {
            DemoPlugin plugin = installedPlugins.remove(pluginId);
            if (plugin != null) {
                try {
                    Path pluginPath = Paths.get(plugin.getPluginPath());
                    Files.deleteIfExists(pluginPath);
                } catch (Exception e) {
                    log.warn("删除插件文件失败: {}", e.getMessage());
                }
            }
            log.info("插件卸载成功: {}", pluginId);
        }

        /**
         * 验证插件
         */
        @Override
        public boolean verify(Path pluginPath) {
            try {
                parsePluginDescriptor(pluginPath.toFile());
                return true;
            } catch (Exception e) {
                log.warn("插件验证失败: {}", e.getMessage());
                return false;
            }
        }

        /**
         * 获取上传临时目录
         */
        public Path getUploadTempPath() {
            return uploadTempPath;
        }

        /**
         * 解析插件描述符
         */
        private PluginDescriptor parsePluginDescriptor(File pluginFile) throws Exception {
            Properties props = new Properties();
            
            try (JarFile jar = new JarFile(pluginFile)) {
                JarEntry entry = jar.getJarEntry("plugin.properties");
                if (entry == null) {
                    entry = jar.getJarEntry("META-INF/plugin.properties");
                }
                
                if (entry == null) {
                    throw new RuntimeException("未找到插件描述符文件 (plugin.properties)");
                }

                try (var inputStream = jar.getInputStream(entry)) {
                    props.load(inputStream);
                }
            }

            String pluginId = props.getProperty("plugin.id");
            String name = props.getProperty("plugin.name", pluginId);
            String version = props.getProperty("plugin.version", "1.0.0");
            String author = props.getProperty("plugin.author", "未知");
            String description = props.getProperty("plugin.description", "");

            if (pluginId == null || pluginId.isEmpty()) {
                throw new RuntimeException("插件描述符缺少必要的 plugin.id 属性");
            }

            return new PluginDescriptor() {
                @Override
                public String getPluginId() {
                    return pluginId;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getVersion() {
                    return version;
                }

                @Override
                public String getAuthor() {
                    return author;
                }

                @Override
                public String getDescription() {
                    return description;
                }
            };
        }

        /**
         * 简单的插件描述符接口
         */
        public interface PluginDescriptor {
            String getPluginId();
            String getName();
            String getVersion();
            String getAuthor();
            String getDescription();
        }
    }

    /**
     * 提供 Demo PluginManager Bean
     */
    @Bean
    @Primary
    public PluginManager demoPluginManager() {
        return new DemoPluginManager();
    }
}
