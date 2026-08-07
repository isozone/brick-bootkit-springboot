package com.zqzqq.bootkits.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 插件配置管理器单元测试
 *
 * @author zqzqq
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PluginConfigurationManagerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PluginConfigurationProperties properties;

    private PluginConfigurationManager manager;
    private PluginConfiguration testConfig;

    @BeforeEach
    void setUp() {
        // 设置基本属性
        when(properties.isHotReloadEnabled()).thenReturn(false);
        when(properties.isPersistenceEnabled()).thenReturn(false);
        when(properties.getMaxVersionHistory()).thenReturn(10);
        when(properties.getConfigDirectory()).thenReturn("test-config");

        manager = new PluginConfigurationManager(eventPublisher, properties);

        // 创建测试配置
        testConfig = new PluginConfiguration("test-plugin");
        testConfig.setVersion("1.0.0");
        testConfig.setDescription("测试插件配置");
        testConfig.setProperty("enabled", true);
        testConfig.setProperty("port", 8080);
        testConfig.setProperty("timeout", 30000L);
    }

    @Test
    void testGetConfiguration() {
        // 测试获取不存在的配置
        PluginConfiguration config = manager.getConfiguration("non-existent");
        assertNull(config);

        // 添加配置并测试获取
        manager.updateConfiguration("test-plugin", testConfig);
        PluginConfiguration retrieved = manager.getConfiguration("test-plugin");
        
        assertNotNull(retrieved);
        assertEquals("test-plugin", retrieved.getPluginId());
        assertEquals("1.0.0", retrieved.getVersion());
    }

    @Test
    void testGetConfigurationProperty() {
        // 添加配置
        manager.updateConfiguration("test-plugin", testConfig);

        // 测试属性获取
        assertEquals(true, manager.getConfigurationProperty("test-plugin", "enabled", Boolean.class));
        assertEquals(Integer.valueOf(8080), manager.getConfigurationProperty("test-plugin", "port", Integer.class));
        assertEquals(Long.valueOf(30000L), manager.getConfigurationProperty("test-plugin", "timeout", Long.class));

        // 测试默认值
        assertEquals(Boolean.FALSE, manager.getConfigurationProperty("test-plugin", "non-existent", Boolean.FALSE, Boolean.class));
        assertEquals("default", manager.getConfigurationProperty("test-plugin", "non-existent", "default", String.class));
    }

    @Test
    void testUpdateConfiguration() {
        // 测试正常更新
        manager.updateConfiguration("test-plugin", testConfig);
        PluginConfiguration retrieved = manager.getConfiguration("test-plugin");
        assertNotNull(retrieved);

        // 验证事件发布
        verify(eventPublisher).publishEvent(any(PluginConfigurationChangeEvent.class));

        // 更新已有配置
        PluginConfiguration updatedConfig = new PluginConfiguration("test-plugin");
        updatedConfig.setVersion("2.0.0");
        updatedConfig.setDescription("更新后的配置");
        
        manager.updateConfiguration(updatedConfig.getPluginId(), updatedConfig);
        
        PluginConfiguration finalConfig = manager.getConfiguration("test-plugin");
        assertEquals("2.0.0", finalConfig.getVersion());
        assertEquals("更新后的配置", finalConfig.getDescription());
    }

    @Test
    void testRemoveConfiguration() {
        // 先添加配置
        manager.updateConfiguration("test-plugin", testConfig);
        assertNotNull(manager.getConfiguration("test-plugin"));

        // 移除配置
        manager.removeConfiguration("test-plugin");
        
        // 验证配置已移除
        assertNull(manager.getConfiguration("test-plugin"));
        
        // 验证事件发布
        verify(eventPublisher, times(2)).publishEvent(any(PluginConfigurationChangeEvent.class));
    }

    @Test
    void testGetConfigurationVersions() {
        manager.updateConfiguration("test-plugin", testConfig);

        // 更新配置产生版本历史
        PluginConfiguration updatedConfig = new PluginConfiguration("test-plugin");
        updatedConfig.setVersion("2.0.0");
        manager.updateConfiguration("test-plugin", updatedConfig);

        List<PluginConfigurationVersion> versions = manager.getConfigurationVersions("test-plugin");
        assertNotNull(versions);
        assertEquals(2, versions.size());
    }

    @Test
    void testRollbackToVersion() {
        manager.updateConfiguration("test-plugin", testConfig);

        // 更新配置
        PluginConfiguration updatedConfig = new PluginConfiguration("test-plugin");
        updatedConfig.setVersion("2.0.0");
        manager.updateConfiguration("test-plugin", updatedConfig);

        List<PluginConfigurationVersion> versions = manager.getConfigurationVersions("test-plugin");
        String versionId = versions.get(0).getVersionId();

        // 回滚到第一个版本
        manager.rollbackToVersion("test-plugin", versionId);

        PluginConfiguration current = manager.getConfiguration("test-plugin");
        assertEquals("1.0.0", current.getVersion());
    }

    @Test
    void testRollbackToVersion_InvalidVersionId() {
        manager.updateConfiguration("test-plugin", testConfig);

        // 尝试回滚到不存在的版本
        assertThrows(IllegalArgumentException.class, () -> {
            manager.rollbackToVersion("test-plugin", "invalid-version-id");
        });
    }

    @Test
    void testRollbackToVersion_PluginNotFound() {
        // 尝试回滚不存在的插件
        assertThrows(IllegalArgumentException.class, () -> {
            manager.rollbackToVersion("non-existent-plugin", "version-id");
        });
    }

    @Test
    void testAddConfigurationListener() {
        TestPluginConfigurationListener listener = new TestPluginConfigurationListener();
        
        manager.addConfigurationListener(listener);
        manager.updateConfiguration("test-plugin", testConfig);
        
        assertTrue(listener.isCalled());
        assertEquals("test-plugin", listener.getPluginId());
    }

    @Test
    void testRemoveConfigurationListener() {
        TestPluginConfigurationListener listener = new TestPluginConfigurationListener();
        
        manager.addConfigurationListener(listener);
        manager.removeConfigurationListener(listener);
        listener.resetCallCount();
        
        manager.updateConfiguration("test-plugin", testConfig);
        
        assertFalse(listener.isCalled());
    }

    @Test
    void testGetAllConfigurations() {
        // 添加多个配置
        manager.updateConfiguration("plugin1", testConfig);
        
        PluginConfiguration config2 = new PluginConfiguration("plugin2");
        manager.updateConfiguration("plugin2", config2);

        Map<String, PluginConfiguration> allConfigs = manager.getAllConfigurations();
        assertEquals(2, allConfigs.size());
        assertTrue(allConfigs.containsKey("plugin1"));
        assertTrue(allConfigs.containsKey("plugin2"));
    }

    @Test
    void testHasConfiguration() {
        assertFalse(manager.hasConfiguration("test-plugin"));
        
        manager.updateConfiguration("test-plugin", testConfig);
        assertTrue(manager.hasConfiguration("test-plugin"));
    }

    @Test
    void testGetStatistics() {
        manager.updateConfiguration("test-plugin", testConfig);
        manager.addConfigurationListener(new TestPluginConfigurationListener());

        PluginConfigurationStatistics stats = manager.getStatistics();
        assertNotNull(stats);
        assertEquals(1, stats.getTotalConfigurations());
        assertEquals(1, stats.getTotalVersions());
        assertEquals(0, stats.getActiveWatchers()); // 因为我们关闭了热重载
        assertEquals(1, stats.getListenerCount());
    }

    @Test
    void testConfigurationManagementWithHotReload() {
        // 启用热重载
        when(properties.isHotReloadEnabled()).thenReturn(true);
        
        manager = new PluginConfigurationManager(eventPublisher, properties);
        manager.updateConfiguration("test-plugin", testConfig);

        PluginConfigurationStatistics stats = manager.getStatistics();
        assertEquals(1, stats.getActiveWatchers());
    }

    @Test
    void testShutdown() {
        manager.addConfigurationListener(new TestPluginConfigurationListener());
        manager.updateConfiguration("test-plugin", testConfig);

        // 关闭管理器
        assertDoesNotThrow(() -> manager.shutdown());

        // 再次尝试操作应该不会报错
        PluginConfiguration config = manager.getConfiguration("test-plugin");
        assertNotNull(config);
    }

    /**
     * 测试配置监听器
     */
    private static class TestPluginConfigurationListener implements PluginConfigurationListener {
        private boolean called = false;
        private String pluginId;

        @Override
        public void onConfigurationChanged(PluginConfigurationChangeEvent event) {
            this.called = true;
            this.pluginId = event.getPluginId();
        }

        public boolean isCalled() {
            return called;
        }

        public String getPluginId() {
            return pluginId;
        }

        public void resetCallCount() {
            this.called = false;
        }
    }
}