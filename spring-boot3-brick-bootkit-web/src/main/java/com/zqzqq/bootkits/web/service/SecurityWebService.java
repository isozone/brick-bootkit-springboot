package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.sandbox.PluginSandbox;
import com.zqzqq.bootkits.core.security.PluginPermission;
import com.zqzqq.bootkits.core.security.PluginSecurityManager;
import com.zqzqq.bootkits.core.security.PluginSecurityPolicy;
import com.zqzqq.bootkits.core.security.PluginSecurityValidationResult;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * 插件安全中心 Web 服务。
 * 基于主框架注册的 {@link PluginSecurityManager} 与 {@link PluginSandbox}，
 * 提供插件安全扫描、安全策略与权限管理能力。
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class SecurityWebService {

    private final ObjectProvider<PluginSecurityManager> securityManagerProvider;
    private final ObjectProvider<PluginSandbox> sandboxProvider;
    private final ObjectProvider<PluginManager> pluginManagerProvider;
    private final BrickWebProperties properties;

    public SecurityWebService(ObjectProvider<PluginSecurityManager> securityManagerProvider,
                              ObjectProvider<PluginSandbox> sandboxProvider,
                              ObjectProvider<PluginManager> pluginManagerProvider,
                              BrickWebProperties properties) {
        this.securityManagerProvider = securityManagerProvider;
        this.sandboxProvider = sandboxProvider;
        this.pluginManagerProvider = pluginManagerProvider;
        this.properties = properties;
    }

    private PluginSecurityManager getSecurityManager() {
        PluginSecurityManager securityManager = securityManagerProvider.getIfAvailable();
        if (securityManager == null) {
            throw new PluginException("插件安全服务未启用");
        }
        return securityManager;
    }

    /**
     * 扫描插件安全（按插件 ID 解析路径）
     */
    public PluginSecurityValidationResult scanPluginById(String pluginId) {
        if (!StringUtils.hasText(pluginId)) {
            throw new PluginException("插件 ID 不能为空");
        }
        PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
        if (pluginManager == null) {
            throw new PluginException("插件管理器不可用");
        }
        PluginInfo pluginInfo = pluginManager.getPlugin(pluginId);
        if (pluginInfo == null || pluginInfo.getPluginPath() == null) {
            throw new PluginException("插件不存在: " + pluginId);
        }
        Path pluginPath = resolveScanPath(pluginInfo.getPluginPath());
        return getSecurityManager().validatePluginSecurity(pluginId, pluginPath);
    }

    /**
     * 扫描插件安全（按文件路径）
     */
    public PluginSecurityValidationResult scanPluginByPath(String path) {
        if (!StringUtils.hasText(path)) {
            throw new PluginException("插件路径不能为空");
        }
        Path pluginPath = resolveScanPath(path);
        String pluginId = pluginPath.getFileName() == null
                ? path : pluginPath.getFileName().toString();
        return getSecurityManager().validatePluginSecurity(pluginId, pluginPath);
    }

    /**
     * 获取插件安全策略
     */
    public PluginSecurityPolicy getPolicy(String pluginId) {
        if (!StringUtils.hasText(pluginId)) {
            throw new PluginException("插件 ID 不能为空");
        }
        return getSecurityManager().getSecurityPolicy(pluginId);
    }

    /**
     * 设置插件安全策略
     */
    public void setPolicy(String pluginId, PluginSecurityPolicy policy) {
        if (!StringUtils.hasText(pluginId)) {
            throw new PluginException("插件 ID 不能为空");
        }
        if (policy == null) {
            throw new PluginException("安全策略不能为空");
        }
        getSecurityManager().setSecurityPolicy(pluginId, policy);
        // 同步创建沙箱策略
        PluginSandbox sandbox = sandboxProvider.getIfAvailable();
        if (sandbox != null) {
            sandbox.createPolicy(pluginId, policy.isAllowFileSystemAccess(),
                    policy.isAllowNetworkAccess());
        }
    }

    /**
     * 授予插件权限
     */
    public void grantPermission(String pluginId, PluginPermission permission) {
        if (!StringUtils.hasText(pluginId)) {
            throw new PluginException("插件 ID 不能为空");
        }
        if (permission == null) {
            throw new PluginException("权限不能为空");
        }
        getSecurityManager().grantPermission(pluginId, permission);
    }

    /**
     * 撤销插件权限
     */
    public void revokePermission(String pluginId, PluginPermission permission) {
        if (!StringUtils.hasText(pluginId)) {
            throw new PluginException("插件 ID 不能为空");
        }
        if (permission == null) {
            throw new PluginException("权限不能为空");
        }
        getSecurityManager().revokePermission(pluginId, permission);
    }

    /**
     * 获取插件已授予的权限
     */
    public Set<PluginPermission> getPermissions(String pluginId) {
        if (!StringUtils.hasText(pluginId)) {
            throw new PluginException("插件 ID 不能为空");
        }
        return getSecurityManager().getPluginPermissions(pluginId);
    }

    /**
     * 解析并校验扫描路径：仅允许访问插件上传目录或插件根目录中的文件
     */
    private Path resolveScanPath(String path) {
        try {
            Path normalized = Paths.get(path).toAbsolutePath().normalize();
            boolean allowed = false;
            if (StringUtils.hasText(properties.getUploadTempPath())) {
                Path uploadTemp = Paths.get(properties.getUploadTempPath()).toAbsolutePath().normalize();
                if (normalized.startsWith(uploadTemp)) {
                    allowed = true;
                }
            }
            if (!allowed && properties.getPluginPaths() != null) {
                for (String pluginRoot : properties.getPluginPaths()) {
                    if (StringUtils.hasText(pluginRoot)) {
                        Path root = Paths.get(pluginRoot).toAbsolutePath().normalize();
                        if (normalized.startsWith(root)) {
                            allowed = true;
                            break;
                        }
                    }
                }
            }
            if (!allowed) {
                throw new PluginException("仅允许访问插件上传目录或插件根目录中的文件");
            }
            if (!Files.exists(normalized)) {
                throw new PluginException("插件文件不存在: " + normalized);
            }
            return normalized;
        } catch (InvalidPathException e) {
            throw new PluginException("插件路径不合法: " + e.getMessage(), e);
        }
    }
}
