package com.zqzqq.bootkits.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 插件安全管理器单元测试
 *
 * @author zqzqq
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PluginSecurityManagerTest {

    @Mock
    private PluginSecurityConfiguration configuration;

    private PluginSecurityManager securityManager;
    private Path testPluginPath;
    private PluginSecurityPolicy defaultPolicy;
    private TestSecurityListener securityListener;

    @BeforeEach
    void setUp() {
        // 设置基本配置
        when(configuration.getDefaultMaxMemoryUsage()).thenReturn(1024L * 1024 * 1024); // 1GB

        securityManager = new PluginSecurityManager(configuration);
        testPluginPath = Paths.get("test-plugin.jar");
        defaultPolicy = PluginSecurityPolicy.builder()
                .allowFileSystemAccess(false)
                .allowNetworkAccess(false)
                .allowSystemPropertyAccess(false)
                .allowReflectionAccess(true)
                .maxMemoryUsage(1024L * 1024 * 1024)
                .maxThreadCount(100)
                .build();

        securityListener = new TestSecurityListener();
        securityManager.addSecurityListener(securityListener);
    }

    @Test
    void testValidatePluginSecurity() {
        // 测试插件安全验证
        PluginSecurityValidationResult result = securityManager.validatePluginSecurity("test-plugin", testPluginPath);
        
        assertNotNull(result);
        assertEquals("test-plugin", result.getPluginId());
        
        // 验证审计日志记录
        // verify(auditLogger).logSecurityValidation(eq("test-plugin"), any(PluginSecurityValidationResult.class));
    }

    @Test
    void testCheckPermission() {
        // 创建权限
        PluginPermission filePermission = PluginPermission.fileSystem("/tmp", "read");
        PluginPermission networkPermission = PluginPermission.network("localhost", "connect");
        
        // 授予权限
        securityManager.grantPermission("test-plugin", filePermission);
        
        // 测试权限检查
        assertTrue(securityManager.checkPermission("test-plugin", filePermission));
        assertFalse(securityManager.checkPermission("test-plugin", networkPermission));
    }

    @Test
    void testGrantPermission() {
        PluginPermission permission = PluginPermission.fileSystem("/data", "read");
        
        assertDoesNotThrow(() -> {
            securityManager.grantPermission("test-plugin", permission);
        });
        
        // 验证权限已授予
        Set<PluginPermission> pluginPermissions = securityManager.getPluginPermissions("test-plugin");
        assertTrue(pluginPermissions.contains(permission));
        
        // 验证监听器通知
        assertTrue(securityListener.isPermissionGranted());
    }

    @Test
    void testRevokePermission() {
        PluginPermission permission = PluginPermission.fileSystem("/data", "read");
        
        // 先授予权限
        securityManager.grantPermission("test-plugin", permission);
        assertTrue(securityManager.checkPermission("test-plugin", permission));
        
        // 撤销权限
        securityManager.revokePermission("test-plugin", permission);
        
        // 验证权限已撤销
        assertFalse(securityManager.checkPermission("test-plugin", permission));
    }

    @Test
    void testSetSecurityPolicy() {
        PluginSecurityPolicy customPolicy = PluginSecurityPolicy.builder()
                .allowFileSystemAccess(true)
                .allowNetworkAccess(true)
                .maxMemoryUsage(512L * 1024 * 1024)
                .build();
        
        securityManager.setSecurityPolicy("test-plugin", customPolicy);
        
        PluginSecurityPolicy retrieved = securityManager.getSecurityPolicy("test-plugin");
        assertNotNull(retrieved);
        assertTrue(retrieved.isAllowFileSystemAccess());
        assertTrue(retrieved.isAllowNetworkAccess());
    }

    @Test
    void testGetSecurityPolicy_Default() {
        // 测试获取默认安全策略
        PluginSecurityPolicy defaultSecurityPolicy = securityManager.getSecurityPolicy("non-existent-plugin");
        
        assertNotNull(defaultSecurityPolicy);
        assertFalse(defaultSecurityPolicy.isAllowFileSystemAccess());
        assertFalse(defaultSecurityPolicy.isAllowNetworkAccess());
        assertTrue(defaultSecurityPolicy.isAllowReflectionAccess());
    }

    @Test
    void testAddSecurityListener() {
        TestSecurityListener newListener = new TestSecurityListener();
        
        assertDoesNotThrow(() -> {
            securityManager.addSecurityListener(newListener);
        });
        
        // 验证监听器可以通过权限检查来测试是否被添加
        PluginPermission permission = PluginPermission.fileSystem("/tmp", "read");
        securityManager.grantPermission("test-plugin", permission);
        assertTrue(securityManager.checkPermission("test-plugin", permission));
        
        // 验证监听器确实收到了通知
        assertTrue(newListener.isPermissionGranted());
    }

    @Test
    void testRemoveSecurityListener() {
        TestSecurityListener originalListener = securityListener;
        
        assertDoesNotThrow(() -> {
            securityManager.removeSecurityListener(originalListener);
        });
        
        // 添加新权限，验证原监听器不会收到通知
        PluginPermission permission = PluginPermission.fileSystem("/tmp", "read");
        securityManager.grantPermission("test-plugin", permission);
        
        // 原监听器应该不再被通知（新监听器应该仍然被通知）
        assertFalse(originalListener.isPermissionGranted());
    }

    @Test
    void testCleanupPluginSecurity() {
        // 添加安全信息
        securityManager.grantPermission("test-plugin", PluginPermission.fileSystem("/tmp", "read"));
        securityManager.setSecurityPolicy("test-plugin", defaultPolicy);
        
        // 清理插件安全信息
        securityManager.cleanupPluginSecurity("test-plugin");
        
        // 验证插件权限已被清理
        Set<PluginPermission> pluginPermissions = securityManager.getPluginPermissions("test-plugin");
        assertTrue(pluginPermissions.isEmpty());
    }

    @Test
    void testGetPluginPermissions() {
        PluginPermission permission1 = PluginPermission.fileSystem("/tmp", "read");
        PluginPermission permission2 = PluginPermission.network("localhost", "connect");
        
        securityManager.grantPermission("test-plugin", permission1);
        securityManager.grantPermission("test-plugin", permission2);
        
        Set<PluginPermission> permissions = securityManager.getPluginPermissions("test-plugin");
        
        assertNotNull(permissions);
        assertEquals(2, permissions.size());
        assertTrue(permissions.contains(permission1));
        assertTrue(permissions.contains(permission2));
    }

    @Test
    void testValidatePluginSecurity_Exception() {
        // 模拟配置验证异常
        when(configuration.getDefaultMaxMemoryUsage()).thenThrow(new RuntimeException("Test exception"));
        
        PluginSecurityValidationResult result = securityManager.validatePluginSecurity("test-plugin", testPluginPath);
        
        assertNotNull(result);
        assertFalse(result.isValid());
        assertTrue(result.getViolations().stream()
                .anyMatch(violation -> violation.contains("插件安全验证失败")));
    }

    @Test
    void testSecurityPolicyValidation() {
        // 创建超出全局限制的策略
        PluginSecurityPolicy excessivePolicy = PluginSecurityPolicy.builder()
                .maxMemoryUsage(4096L * 1024 * 1024) // 4GB，超过全局2GB限制
                .maxThreadCount(300) // 超过全局200限制
                .build();
        
        securityManager.setSecurityPolicy("test-plugin", excessivePolicy);
        PluginSecurityValidationResult result = securityManager.validatePluginSecurity("test-plugin", testPluginPath);
        
        assertNotNull(result);
        assertFalse(result.getViolations().isEmpty());
        assertTrue(result.getViolations().stream()
                .anyMatch(violation -> violation.contains("插件内存限制超过全局限制") ||
                                     violation.contains("插件线程数限制超过全局限制")));
    }

    /**
     * 测试安全监听器
     */
    private static class TestSecurityListener implements PluginSecurityListener {
        private boolean validationCalled = false;
        private boolean permissionGranted = false;
        private boolean permissionViolated = false;

        @Override
        public void onSecurityValidationCompleted(String pluginId, PluginSecurityValidationResult result) {
            this.validationCalled = true;
        }

        @Override
        public void onPermissionChecked(String pluginId, PluginPermission permission, boolean granted) {
            if (granted) {
                this.permissionGranted = true;
            }
        }

        @Override
        public void onPermissionViolation(String pluginId, PluginPermission permission, String reason) {
            this.permissionViolated = true;
        }

        public boolean isValidationCalled() {
            return validationCalled;
        }

        public boolean isPermissionGranted() {
            return permissionGranted;
        }

        public boolean isPermissionViolated() {
            return permissionViolated;
        }

        public void reset() {
            this.validationCalled = false;
            this.permissionGranted = false;
            this.permissionViolated = false;
        }
    }
}