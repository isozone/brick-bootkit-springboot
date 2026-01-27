package com.zqzqq.bootkits.core.plugin;

import com.zqzqq.bootkits.core.exception.PluginErrorCode;
import com.zqzqq.bootkits.core.exception.PluginLoadException;
import com.zqzqq.bootkits.core.logging.PluginLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 插件加载器
 * 负责从JAR文件加载插件
 */
public class PluginLoader {

    private static final PluginLogger logger = PluginLogger.getLogger(PluginLoader.class);
    
    /**
     * 类加载器缓存最大数量
     */
    private static final int MAX_CLASS_LOADER_CACHE_SIZE = 100;
    
    // 插件类加载器缓存
    private final ConcurrentHashMap<String, URLClassLoader> classLoaders = new ConcurrentHashMap<>();
    
    // 用于跟踪访问顺序（简单实现，不使用LinkedHashMap以避免额外依赖）
    private final java.util.List<String> accessOrder = new java.util.ArrayList<>();

    /**
     * 加载插件
     */
    public Plugin loadPlugin(File pluginFile) {
        logger.info("开始加载插件: {}", pluginFile.getName());
        
        try {
            // 验证文件存在性
            if (!pluginFile.exists()) {
                throw new PluginLoadException(
                    PluginErrorCode.PLUGIN_FILE_NOT_FOUND,
                    "插件文件不存在: " + pluginFile.getAbsolutePath()
                );
            }
            
            // 验证文件不为空
            if (pluginFile.length() == 0) {
                throw new PluginLoadException(
                    PluginErrorCode.PLUGIN_FILE_EMPTY,
                    "插件文件为空: " + pluginFile.getName()
                );
            }
            
            // 验证JAR文件格式
            try (JarFile jarFile = new JarFile(pluginFile)) {
                Manifest manifest = jarFile.getManifest();
                if (manifest == null) {
                    throw new PluginLoadException(
                        PluginErrorCode.PLUGIN_INVALID_FORMAT,
                        "插件文件缺少清单文件: " + pluginFile.getName()
                    );
                }
                
                // 解析插件元数据
                PluginMetadata metadata = parsePluginMetadata(manifest);
                
                // 创建类加载器
                URLClassLoader classLoader = createPluginClassLoader(pluginFile);
                
                // 加载插件主类
                Class<?> pluginClass = loadPluginMainClass(classLoader, metadata.getMainClass());
                
                // 创建插件实例
                Plugin plugin = createPluginInstance(pluginClass, metadata);
                
                // 缓存类加载器（带LRU淘汰机制）
                cacheClassLoader(metadata.getId(), classLoader);
                
                logger.info("插件加载成功: {} ({})", metadata.getName(), metadata.getId());
                return plugin;
                
            } catch (IOException e) {
                throw new PluginLoadException(
                    PluginErrorCode.PLUGIN_INVALID_FORMAT,
                    "无效的JAR文件: " + pluginFile.getName(),
                    e
                );
            }
            
        } catch (PluginLoadException e) {
            throw e;
        } catch (Exception e) {
            logger.error("插件加载失败: {} - {}", pluginFile.getName(), e.getMessage(), e);
            throw new PluginLoadException(
                PluginErrorCode.PLUGIN_LOAD_FAILED,
                "插件加载失败: " + pluginFile.getName(),
                e
            );
        }
    }

    /**
     * 重新加载插件
     */
    public Plugin reloadPlugin(String pluginId, File pluginFile) {
        logger.info("重新加载插件: {}", pluginId);
        
        // 从访问顺序中移除
        accessOrder.remove(pluginId);
        
        // 关闭旧的类加载器
        URLClassLoader oldClassLoader = classLoaders.remove(pluginId);
        if (oldClassLoader != null) {
            try {
                oldClassLoader.close();
            } catch (IOException e) {
                logger.warn("关闭旧类加载器失败: {}", e.getMessage());
            }
        }
        
        // 重新加载插件
        return loadPlugin(pluginFile);
    }

    /**
     * 卸载插件类加载器
     */
    public void unloadPlugin(String pluginId) {
        // 从访问顺序中移除
        accessOrder.remove(pluginId);
        
        URLClassLoader classLoader = classLoaders.remove(pluginId);
        if (classLoader != null) {
            try {
                classLoader.close();
                logger.info("插件类加载器已卸载: {}", pluginId);
            } catch (IOException e) {
                logger.error("卸载插件类加载器失败: {} - {}", pluginId, e.getMessage(), e);
            }
        }
    }

    private PluginMetadata parsePluginMetadata(Manifest manifest) {
        String id = manifest.getMainAttributes().getValue("Plugin-Id");
        String name = manifest.getMainAttributes().getValue("Plugin-Name");
        String version = manifest.getMainAttributes().getValue("Plugin-Version");
        String mainClass = manifest.getMainAttributes().getValue("Plugin-Main-Class");
        String description = manifest.getMainAttributes().getValue("Plugin-Description");
        
        if (id == null || id.trim().isEmpty()) {
            throw new PluginLoadException(
                PluginErrorCode.PLUGIN_METADATA_INVALID,
                "插件ID不能为空"
            );
        }
        
        if (mainClass == null || mainClass.trim().isEmpty()) {
            throw new PluginLoadException(
                PluginErrorCode.PLUGIN_METADATA_INVALID,
                "插件主类不能为空"
            );
        }
        
        return new PluginMetadata(
            id,
            name != null ? name : id,
            version != null ? version : "1.0.0",
            mainClass,
            description != null ? description : ""
        );
    }

    private URLClassLoader createPluginClassLoader(File pluginFile) throws IOException {
        URL pluginUrl = pluginFile.toURI().toURL();
        return new URLClassLoader(
            new URL[]{pluginUrl},
            this.getClass().getClassLoader()
        );
    }

    private Class<?> loadPluginMainClass(URLClassLoader classLoader, String mainClassName) {
        try {
            return classLoader.loadClass(mainClassName);
        } catch (ClassNotFoundException e) {
            throw new PluginLoadException(
                PluginErrorCode.PLUGIN_CLASS_NOT_FOUND,
                "找不到插件主类: " + mainClassName,
                e
            );
        }
    }

    private Plugin createPluginInstance(Class<?> pluginClass, PluginMetadata metadata) {
        try {
            // 检查是否实现了Plugin接口
            if (!Plugin.class.isAssignableFrom(pluginClass)) {
                throw new PluginLoadException(
                    PluginErrorCode.PLUGIN_INTERFACE_NOT_IMPLEMENTED,
                    "插件主类必须实现Plugin接口: " + pluginClass.getName()
                );
            }
            
            // 创建插件实例
            Plugin plugin = (Plugin) pluginClass.getDeclaredConstructor().newInstance();
            
            // 设置插件元数据
            if (plugin instanceof AbstractPlugin) {
                ((AbstractPlugin) plugin).setMetadata(metadata);
            }
            
            return plugin;
            
        } catch (Exception e) {
            throw new PluginLoadException(
                PluginErrorCode.PLUGIN_INSTANTIATION_FAILED,
                "创建插件实例失败: " + pluginClass.getName(),
                e
            );
        }
    }
    
    /**
     * 缓存类加载器，带LRU淘汰机制
     */
    private void cacheClassLoader(String pluginId, URLClassLoader classLoader) {
        // 检查缓存大小，必要时淘汰旧的类加载器
        if (classLoaders.size() >= MAX_CLASS_LOADER_CACHE_SIZE) {
            evictOldestClassLoader();
        }
        
        classLoaders.put(pluginId, classLoader);
        accessOrder.add(pluginId);
    }
    
    /**
     * 淘汰最旧的类加载器
     */
    private void evictOldestClassLoader() {
        if (accessOrder.isEmpty()) {
            return;
        }
        
        String oldestId = accessOrder.remove(0);
        URLClassLoader removed = classLoaders.remove(oldestId);
        
        if (removed != null) {
            try {
                removed.close();
                logger.info("system", "已淘汰最旧的类加载器: {}", oldestId);
            } catch (IOException e) {
                logger.warn("system", "关闭淘汰的类加载器失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 插件元数据内部类
     */
    public static class PluginMetadata {
        private final String id;
        private final String name;
        private final String version;
        private final String mainClass;
        private final String description;

        public PluginMetadata(String id, String name, String version, String mainClass, String description) {
            this.id = id;
            this.name = name;
            this.version = version;
            this.mainClass = mainClass;
            this.description = description;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getMainClass() { return mainClass; }
        public String getDescription() { return description; }
    }
    
    /**
     * 关闭插件加载器，清理所有类加载器
     * 
     * 注意：这是一个破坏性操作，将关闭所有已缓存的类加载器
     */
    public void shutdown() {
        logger.info("system", "开始关闭插件加载器");
        
        try {
            classLoaders.values().forEach(classLoader -> {
                try {
                    classLoader.close();
                    logger.debug("system", "已关闭类加载器", classLoader.toString());
                } catch (IOException e) {
                    logger.warn("system", "关闭类加载器失败", e.getMessage());
                }
            });
            
            classLoaders.clear();
            accessOrder.clear();
            logger.info("system", "插件加载器关闭完成");
            
        } catch (Exception e) {
            logger.error("system", "关闭插件加载器时发生错误", e.getMessage(), e);
            throw new RuntimeException("插件加载器关闭失败", e);
        }
    }
}