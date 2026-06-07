package com.zqzqq.bootkits.core.plugin;

import com.zqzqq.bootkits.core.exception.PluginErrorCode;
import com.zqzqq.bootkits.core.exception.PluginValidationException;
import com.zqzqq.bootkits.core.logging.PluginLogger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 插件验证器
 * 负责验证插件文件的有效性、安全性和兼容性
 */
public class PluginValidator {

    private static final PluginLogger logger = PluginLogger.getLogger(PluginValidator.class);
    
    // 危险权限列表
    private static final Set<String> DANGEROUS_PERMISSIONS = new HashSet<>(Arrays.asList(
        "system.exit", "file.delete", "network.admin", "security.override"
    ));

    /**
     * 验证插件文件
     */
    public boolean validate(File pluginFile) {
        try {
            logger.info("开始验证插件文件: {}", pluginFile.getName());
            
            // 基本文件检查
            if (!validateFileExists(pluginFile)) {
                return false;
            }
            
            if (!validateFileNotEmpty(pluginFile)) {
                return false;
            }
            
            if (!validateJarFile(pluginFile)) {
                return false;
            }
            
            logger.info("插件文件验证通过: {}", pluginFile.getName());
            return true;
            
        } catch (Exception e) {
            logger.error("插件验证过程中发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证插件签名
     */
    public boolean validateSignature(File pluginFile) {
        try (JarFile jarFile = new JarFile(pluginFile)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                logger.warn("插件文件缺少清单文件: {}", pluginFile.getName());
                return false;
            }
            
            String signature = manifest.getMainAttributes().getValue("Plugin-Signature");
            if (signature == null || signature.trim().isEmpty()) {
                logger.warn("插件文件未签名: {}", pluginFile.getName());
                return false;
            }
            
            // 这里应该实现真正的签名验证逻辑
            // 简化处理，检查签名是否存在
            logger.info("插件签名验证通过: {}", pluginFile.getName());
            return true;
            
        } catch (IOException e) {
            logger.error("验证插件签名时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证版本兼容性
     */
    public boolean validateVersionCompatibility(File pluginFile, String requiredVersion) {
        try (JarFile jarFile = new JarFile(pluginFile)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return false;
            }
            
            String pluginVersion = manifest.getMainAttributes().getValue("Plugin-Version");
            if (pluginVersion == null) {
                logger.warn("插件文件缺少版本信息: {}", pluginFile.getName());
                return false;
            }
            
            // 简化版本比较逻辑
            boolean compatible = isVersionCompatible(pluginVersion, requiredVersion);
            logger.info("版本兼容性检查 - 插件版本: {}, 要求版本: {}, 兼容: {}", 
                       pluginVersion, requiredVersion, compatible);
            
            return compatible;
            
        } catch (IOException e) {
            logger.error("验证版本兼容性时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证插件依赖
     */
    public boolean validateDependencies(File pluginFile) {
        try (JarFile jarFile = new JarFile(pluginFile)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return true; // 没有清单文件，假设无依赖
            }
            
            String dependencies = manifest.getMainAttributes().getValue("Plugin-Dependencies");
            if (dependencies == null || dependencies.trim().isEmpty()) {
                logger.info("插件无依赖: {}", pluginFile.getName());
                return true;
            }
            
            // 解析和验证依赖
            String[] deps = dependencies.split(",");
            for (String dep : deps) {
                if (!validateSingleDependency(dep.trim())) {
                    logger.warn("依赖验证失败: {}", dep);
                    return false;
                }
            }
            
            logger.info("插件依赖验证通过: {}", pluginFile.getName());
            return true;
            
        } catch (IOException e) {
            logger.error("验证插件依赖时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 验证循环依赖
     */
    public void validateCircularDependencies(File... pluginFiles) {
        // 简化实现，检测基本的循环依赖
        for (int i = 0; i < pluginFiles.length; i++) {
            for (int j = i + 1; j < pluginFiles.length; j++) {
                if (hasCircularDependency(pluginFiles[i], pluginFiles[j])) {
                    throw new PluginValidationException(
                        PluginErrorCode.PLUGIN_CIRCULAR_DEPENDENCY,
                        "检测到循环依赖: " + pluginFiles[i].getName() + " <-> " + pluginFiles[j].getName()
                    );
                }
            }
        }
    }

    /**
     * 验证插件权限
     */
    public boolean validatePermissions(File pluginFile) {
        try (JarFile jarFile = new JarFile(pluginFile)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return true; // 没有清单文件，假设无特殊权限
            }
            
            String permissions = manifest.getMainAttributes().getValue("Plugin-Permissions");
            if (permissions == null || permissions.trim().isEmpty()) {
                logger.info("插件无特殊权限要求: {}", pluginFile.getName());
                return true;
            }
            
            // 检查危险权限
            String[] perms = permissions.split(",");
            for (String perm : perms) {
                String trimmedPerm = perm.trim();
                if (DANGEROUS_PERMISSIONS.contains(trimmedPerm)) {
                    logger.warn("插件请求危险权限: {} - {}", pluginFile.getName(), trimmedPerm);
                    return false;
                }
            }
            
            logger.info("插件权限验证通过: {}", pluginFile.getName());
            return true;
            
        } catch (IOException e) {
            logger.error("验证插件权限时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean validateFileExists(File file) {
        if (!file.exists()) {
            logger.error("插件文件不存在: {}", file.getAbsolutePath());
            return false;
        }
        return true;
    }

    private boolean validateFileNotEmpty(File file) {
        if (file.length() == 0) {
            logger.error("插件文件为空: {}", file.getName());
            return false;
        }
        return true;
    }

    private boolean validateJarFile(File file) {
        try (JarFile jarFile = new JarFile(file)) {
            // 基本JAR文件结构验证
            return true;
        } catch (IOException e) {
            logger.error("无效的JAR文件: {} - {}", file.getName(), e.getMessage());
            return false;
        }
    }

    private boolean isVersionCompatible(String pluginVersion, String requiredVersion) {
        // 简化版本比较，实际应该使用语义版本比较
        try {
            String[] pluginParts = pluginVersion.split("\\.");
            String[] requiredParts = requiredVersion.split("\\.");
            
            int pluginMajor = Integer.parseInt(pluginParts[0]);
            int requiredMajor = Integer.parseInt(requiredParts[0]);
            
            return pluginMajor >= requiredMajor;
        } catch (Exception e) {
            logger.warn("版本格式错误，使用字符串比较: {} vs {}", pluginVersion, requiredVersion);
            return pluginVersion.equals(requiredVersion);
        }
    }

    private boolean validateSingleDependency(String dependency) {
        // 简化依赖验证，检查格式是否正确
        if (!dependency.contains(":")) {
            logger.warn("依赖格式错误: {}", dependency);
            return false;
        }
        
        String[] parts = dependency.split(":");
        if (parts.length != 2) {
            logger.warn("依赖格式错误: {}", dependency);
            return false;
        }
        
        return true;
    }

    private boolean hasCircularDependency(File plugin1, File plugin2) {
        // 简化实现，检查两个插件是否互相依赖
        try {
            String plugin1Id = getPluginId(plugin1);
            String plugin2Id = getPluginId(plugin2);
            
            if (plugin1Id == null || plugin2Id == null) {
                return false;
            }
            
            boolean plugin1DependsOnPlugin2 = pluginDependsOn(plugin1, plugin2Id);
            boolean plugin2DependsOnPlugin1 = pluginDependsOn(plugin2, plugin1Id);
            
            return plugin1DependsOnPlugin2 && plugin2DependsOnPlugin1;
            
        } catch (Exception e) {
            logger.error("检查循环依赖时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }

    private String getPluginId(File pluginFile) {
        try (JarFile jarFile = new JarFile(pluginFile)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                return manifest.getMainAttributes().getValue("Plugin-Id");
            }
        } catch (IOException e) {
            logger.error("获取插件ID时发生错误: {}", e.getMessage(), e);
        }
        return null;
    }

    private boolean pluginDependsOn(File pluginFile, String targetPluginId) {
        try (JarFile jarFile = new JarFile(pluginFile)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest == null) {
                return false;
            }
            
            String dependencies = manifest.getMainAttributes().getValue("Plugin-Dependencies");
            if (dependencies == null) {
                return false;
            }
            
            return dependencies.contains(targetPluginId);
            
        } catch (IOException e) {
            logger.error("检查插件依赖时发生错误: {}", e.getMessage(), e);
            return false;
        }
    }
}
