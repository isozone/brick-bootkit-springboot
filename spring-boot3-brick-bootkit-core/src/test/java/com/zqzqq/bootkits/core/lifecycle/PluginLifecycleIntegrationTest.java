package com.zqzqq.bootkits.core.lifecycle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 插件生命周期管理集成测试
 *
 * @author zqzqq
 * @since 4.1.0
 */
class PluginLifecycleIntegrationTest {

    private PluginLifecycleManager lifecycleManager;
    private TestLifecycleListener testListener;

    @BeforeEach
    void setUp() {
        lifecycleManager = new PluginLifecycleManager();
        testListener = new TestLifecycleListener();
        
        lifecycleManager.addListener(testListener);
    }

    @Test
    void testPluginStateManagement() {
        String pluginId = "test-plugin-1";
        
        // 初始状态应该是UNINSTALLED
        assertEquals(PluginLifecycleState.UNINSTALLED, lifecycleManager.getCurrentState(pluginId));
        
        // 更新状态到INSTALLED
        lifecycleManager.updateState(pluginId, PluginLifecycleState.INSTALLED);
        assertEquals(PluginLifecycleState.INSTALLED, lifecycleManager.getCurrentState(pluginId));
        
        // 验证监听器被通知
        assertEquals(1, testListener.getEventCount());
        PluginLifecycleEvent event = testListener.getEvents().get(0);
        assertEquals(pluginId, event.getPluginId());
        assertEquals(PluginLifecycleState.UNINSTALLED, event.getOldState());
        assertEquals(PluginLifecycleState.INSTALLED, event.getNewState());
    }

    @Test
    void testPluginLifecycleTransition() {
        String pluginId = "test-plugin-2";
        
        // 模拟完整的生命周期转换
        lifecycleManager.updateState(pluginId, PluginLifecycleState.INSTALLED);
        assertEquals(PluginLifecycleState.INSTALLED, lifecycleManager.getCurrentState(pluginId));
        
        lifecycleManager.updateState(pluginId, PluginLifecycleState.STARTED);
        assertEquals(PluginLifecycleState.STARTED, lifecycleManager.getCurrentState(pluginId));
        
        lifecycleManager.updateState(pluginId, PluginLifecycleState.STOPPED);
        assertEquals(PluginLifecycleState.STOPPED, lifecycleManager.getCurrentState(pluginId));
        
        lifecycleManager.updateState(pluginId, PluginLifecycleState.UNINSTALLED);
        assertEquals(PluginLifecycleState.UNINSTALLED, lifecycleManager.getCurrentState(pluginId));
        
        // 验证监听器收到了所有事件
        assertEquals(4, testListener.getEventCount());
    }

    @Test
    void testMultiplePluginStateManagement() {
        String plugin1 = "plugin-1";
        String plugin2 = "plugin-2";
        String plugin3 = "plugin-3";
        
        // 为多个插件更新状态
        lifecycleManager.updateState(plugin1, PluginLifecycleState.INSTALLED);
        lifecycleManager.updateState(plugin2, PluginLifecycleState.STARTED);
        lifecycleManager.updateState(plugin3, PluginLifecycleState.STOPPED);
        
        // 验证每个插件的状态
        assertEquals(PluginLifecycleState.INSTALLED, lifecycleManager.getCurrentState(plugin1));
        assertEquals(PluginLifecycleState.STARTED, lifecycleManager.getCurrentState(plugin2));
        assertEquals(PluginLifecycleState.STOPPED, lifecycleManager.getCurrentState(plugin3));
        
        // 验证新插件的默认状态
        String newPlugin = "new-plugin";
        assertEquals(PluginLifecycleState.UNINSTALLED, lifecycleManager.getCurrentState(newPlugin));
    }

    @Test
    void testListenerNotification() throws InterruptedException {
        String pluginId = "test-plugin-3";
        
        // 等待监听器设置完成
        Thread.sleep(10);
        
        // 更新状态并验证监听器通知
        lifecycleManager.updateState(pluginId, PluginLifecycleState.INSTALLED);
        lifecycleManager.updateState(pluginId, PluginLifecycleState.STARTED);
        
        // 验证监听器收到了正确的事件
        assertEquals(2, testListener.getEventCount());
        
        List<PluginLifecycleEvent> events = testListener.getEvents();
        assertEquals(PluginLifecycleState.INSTALLED, events.get(0).getNewState());
        assertEquals(PluginLifecycleState.STARTED, events.get(1).getNewState());
    }

    @Test
    void testStateUpdateWithExistingState() {
        String pluginId = "test-plugin-4";
        
        // 多次更新同一个插件的状态
        lifecycleManager.updateState(pluginId, PluginLifecycleState.INSTALLED);
        lifecycleManager.updateState(pluginId, PluginLifecycleState.INSTALLED); // 相同状态
        lifecycleManager.updateState(pluginId, PluginLifecycleState.STARTED);
        
        // 验证状态更新正确（即使状态相同也会触发事件）
        assertEquals(3, testListener.getEventCount());
        assertEquals(PluginLifecycleState.STARTED, lifecycleManager.getCurrentState(pluginId));
    }

    @Test
    void testUnknownPluginDefaultState() {
        String unknownPluginId = "unknown-plugin";
        
        // 验证未知插件的默认状态
        assertEquals(PluginLifecycleState.UNINSTALLED, lifecycleManager.getCurrentState(unknownPluginId));
        
        // 更新状态后应该正常生效
        lifecycleManager.updateState(unknownPluginId, PluginLifecycleState.INSTALLED);
        assertEquals(PluginLifecycleState.INSTALLED, lifecycleManager.getCurrentState(unknownPluginId));
    }

    @Test
    void testListenerErrorHandling() {
        String pluginId = "test-plugin-5";
        
        // 添加一个会产生异常的监听器
        TestLifecycleListener exceptionListener = new TestLifecycleListener();
        exceptionListener.setShouldThrowException(true);
        lifecycleManager.addListener(exceptionListener);
        
        // 更新状态，即使监听器抛出异常也应该继续执行
        lifecycleManager.updateState(pluginId, PluginLifecycleState.INSTALLED);
        
        // 验证状态更新成功（异常监听器不影响其他监听器）
        assertEquals(PluginLifecycleState.INSTALLED, lifecycleManager.getCurrentState(pluginId));
        assertEquals(1, testListener.getEventCount());
    }

    @Test
    void testListenerRegistrationAndNotification() {
        TestLifecycleListener listener1 = new TestLifecycleListener();
        TestLifecycleListener listener2 = new TestLifecycleListener();
        
        // 注册多个监听器
        lifecycleManager.addListener(listener1);
        lifecycleManager.addListener(listener2);
        
        // 更新状态触发通知
        String pluginId = "test-plugin-6";
        lifecycleManager.updateState(pluginId, PluginLifecycleState.INSTALLED);
        
        // 两个监听器都应该收到通知
        assertEquals(1, listener1.getEventCount());
        assertEquals(1, listener2.getEventCount());
    }

    /**
     * 测试生命周期监听器
     */
    private static class TestLifecycleListener implements PluginLifecycleListener {
        
        private final List<PluginLifecycleEvent> events = new ArrayList<>();
        private boolean shouldThrowException = false;
        
        @Override
        public void onEvent(PluginLifecycleEvent event) {
            if (shouldThrowException) {
                throw new RuntimeException("Test exception");
            }
            synchronized (events) {
                events.add(event);
            }
        }
        
        public int getEventCount() {
            synchronized (events) {
                return events.size();
            }
        }
        
        public List<PluginLifecycleEvent> getEvents() {
            synchronized (events) {
                return new ArrayList<>(events);
            }
        }
        
        public void setShouldThrowException(boolean shouldThrow) {
            this.shouldThrowException = shouldThrow;
        }
    }
}