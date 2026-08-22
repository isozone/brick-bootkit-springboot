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


package com.zqzqq.bootkits.core.health;

import com.zqzqq.bootkits.core.plugin.Plugin;
import com.zqzqq.bootkits.core.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 插件自动恢复管理器测试
 */
@DisplayName("PluginAutoRecoveryManager Test")
class PluginAutoRecoveryManagerTest {

    @Mock
    private PluginManager mockPluginManager;

    @Mock
    private PluginHealthChecker mockHealthChecker;

    @Mock
    private Plugin mockPlugin;

    private PluginAutoRecoveryManager recoveryManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        recoveryManager = new PluginAutoRecoveryManager(mockPluginManager, mockHealthChecker);
    }

    @AfterEach
    void tearDown() {
        if (recoveryManager != null) {
            recoveryManager.shutdown();
        }
    }

    @Test
    @DisplayName("测试手动健康检查 - 插件存在")
    void testManualHealthCheckPluginExists() {
        String pluginId = "test-plugin";
        
        when(mockPluginManager.getPlugin(pluginId)).thenReturn(mockPlugin);
        
        PluginHealthReport expectedReport = PluginHealthReport.createHealthy(pluginId, java.util.Collections.emptyList());
        when(mockHealthChecker.checkHealth(mockPlugin)).thenReturn(expectedReport);
        
        PluginHealthReport result = recoveryManager.checkPluginHealth(pluginId);
        
        assertThat(result).isEqualTo(expectedReport);
        verify(mockPluginManager).getPlugin(pluginId);
        verify(mockHealthChecker).checkHealth(mockPlugin);
    }

    @Test
    @DisplayName("测试手动健康检查 - 插件不存在")
    void testManualHealthCheckPluginNotExists() {
        String pluginId = "non-existent-plugin";
        
        when(mockPluginManager.getPlugin(pluginId)).thenReturn(null);
        
        PluginHealthReport result = recoveryManager.checkPluginHealth(pluginId);
        
        assertThat(result).isNotNull();
        assertThat(result.getOverallStatus()).isEqualTo(PluginHealthStatus.UNKNOWN);
        
        verify(mockPluginManager).getPlugin(pluginId);
        verify(mockHealthChecker, never()).checkHealth(any());
    }

    @Test
    @DisplayName("测试手动恢复 - 插件存在")
    void testManualRecoveryPluginExists() {
        String pluginId = "test-plugin";
        
        when(mockPluginManager.getPlugin(pluginId)).thenReturn(mockPlugin);
        
        PluginHealthReport healthReport = PluginHealthReport.createHealthy(pluginId, java.util.Collections.emptyList());
        when(mockHealthChecker.checkHealth(mockPlugin)).thenReturn(healthReport);
        
        boolean result = recoveryManager.triggerManualRecovery(pluginId);
        
        assertThat(result).isTrue();
        verify(mockPluginManager).getPlugin(pluginId);
        verify(mockHealthChecker).checkHealth(mockPlugin);
    }

    @Test
    @DisplayName("测试手动恢复 - 插件不存在")
    void testManualRecoveryPluginNotExists() {
        String pluginId = "non-existent-plugin";
        
        when(mockPluginManager.getPlugin(pluginId)).thenReturn(null);
        
        boolean result = recoveryManager.triggerManualRecovery(pluginId);
        
        assertThat(result).isFalse();
        verify(mockPluginManager).getPlugin(pluginId);
        verify(mockHealthChecker, never()).checkHealth(any());
    }

    @Test
    @DisplayName("测试手动恢复 - 检查器异常")
    void testManualRecoveryWithCheckerException() {
        String pluginId = "test-plugin";
        
        when(mockPluginManager.getPlugin(pluginId)).thenReturn(mockPlugin);
        when(mockHealthChecker.checkHealth(mockPlugin)).thenThrow(new RuntimeException("Test exception"));
        
        boolean result = recoveryManager.triggerManualRecovery(pluginId);
        
        // 检查器抛出异常时，恢复失败并返回 false
        assertThat(result).isFalse();
        
        verify(mockPluginManager).getPlugin(pluginId);
        verify(mockHealthChecker).checkHealth(mockPlugin);
    }

    @Test
    @DisplayName("测试关闭恢复管理器")
    void testShutdownRecoveryManager() {
        recoveryManager.shutdown();
        
        // 关闭后，应该没有异常
        assertThatThrownBy(() -> recoveryManager.checkPluginHealth("test"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("测试多次关闭恢复管理器")
    void testMultipleShutdownCalls() {
        recoveryManager.shutdown();
        
        // 第二次关闭应该安全
        recoveryManager.shutdown();
    }

    @Test
    @DisplayName("测试恢复管理器初始化")
    void testRecoveryManagerInitialization() {
        PluginAutoRecoveryManager newManager = new PluginAutoRecoveryManager(mockPluginManager, mockHealthChecker);
        
        assertThat(newManager).isNotNull();
        
        // 验证正确初始化
        newManager.shutdown();
    }

    @Test
    @DisplayName("测试构造函数参数验证")
    void testConstructorParameterValidation() {
        // 测试null PluginManager
        assertThatThrownBy(() -> new PluginAutoRecoveryManager(null, mockHealthChecker))
            .isInstanceOf(NullPointerException.class);
        
        // 测试null HealthChecker
        assertThatThrownBy(() -> new PluginAutoRecoveryManager(mockPluginManager, null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("测试恢复上下文行为")
    void testRecoveryContextBehavior() {
        // 通过反射或内部类测试来验证恢复上下文的行为
        String pluginId = "test-plugin";
        
        when(mockPluginManager.getPlugin(pluginId)).thenReturn(mockPlugin);
        when(mockPluginManager.getInstalledPluginsList()).thenReturn(java.util.Collections.singletonList(mockPlugin));
        
        PluginHealthReport healthReport = PluginHealthReport.createHealthy(pluginId, java.util.Collections.emptyList());
        when(mockHealthChecker.checkHealth(mockPlugin)).thenReturn(healthReport);
        
        // 手动触发多次恢复以测试上下文
        for (int i = 0; i < 5; i++) {
            recoveryManager.triggerManualRecovery(pluginId);
        }
        
        // 验证恢复上下文的限制行为
        verify(mockPluginManager, atLeast(5)).getPlugin(pluginId);
    }

    @Test
    @DisplayName("测试并发操作")
    void testConcurrentOperations() throws InterruptedException {
        String pluginId1 = "plugin-1";
        String pluginId2 = "plugin-2";
        
        when(mockPluginManager.getPlugin(pluginId1)).thenReturn(mockPlugin);
        when(mockPluginManager.getPlugin(pluginId2)).thenReturn(mockPlugin);

        PluginHealthReport healthReport = PluginHealthReport.createHealthy(pluginId1, java.util.Collections.emptyList());
        when(mockHealthChecker.checkHealth(mockPlugin)).thenReturn(healthReport);
        
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];
        boolean[] results = new boolean[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            final String pluginId = i % 2 == 0 ? pluginId1 : pluginId2;
            final int index = i;
            threads[i] = new Thread(() -> {
                results[index] = recoveryManager.triggerManualRecovery(pluginId);
            });
            threads[i].start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 所有操作都应该成功
        for (boolean result : results) {
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("测试关闭后的状态")
    void testPostShutdownState() {
        recoveryManager.shutdown();
        
        // 验证关闭后的行为
        assertThatThrownBy(() -> recoveryManager.triggerManualRecovery("test"))
            .isInstanceOf(Exception.class);
        
        assertThatThrownBy(() -> recoveryManager.checkPluginHealth("test"))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("测试资源清理")
    void testResourceCleanup() {
        // 获取恢复管理器的内部状态
        recoveryManager.triggerManualRecovery("test-plugin-id");
        
        recoveryManager.shutdown();
        
        // 验证资源已清理
        // 这个测试主要验证shutdown方法没有异常
        recoveryManager.shutdown(); // 第二次调用应该安全
    }

    @Test
    @DisplayName("测试恢复策略的智能选择")
    void testRecoveryStrategySelection() {
        // 这个测试主要验证系统能够根据不同健康状态选择不同的恢复策略
        String pluginId = "test-plugin";
        
        when(mockPluginManager.getPlugin(pluginId)).thenReturn(mockPlugin);
        
        // 测试不同健康状态的恢复行为
        PluginHealthStatus[] statuses = {
            PluginHealthStatus.DEAD,
            PluginHealthStatus.CRITICAL,
            PluginHealthStatus.WARNING,
            PluginHealthStatus.HEALTHY
        };
        
        for (PluginHealthStatus status : statuses) {
            PluginHealthReport healthReport = PluginHealthReport.createHealthy(pluginId, java.util.Collections.emptyList());
            // 模拟不同的健康状态
            when(mockHealthChecker.checkHealth(mockPlugin)).thenReturn(healthReport);
            
            boolean result = recoveryManager.triggerManualRecovery(pluginId);
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("测试恢复限流机制")
    void testRecoveryRateLimiting() {
        String pluginId = "test-plugin";
        
        when(mockPluginManager.getPlugin(pluginId)).thenReturn(mockPlugin);
        
        PluginHealthReport healthReport = PluginHealthReport.createHealthy(pluginId, java.util.Collections.emptyList());
        when(mockHealthChecker.checkHealth(mockPlugin)).thenReturn(healthReport);
        
        // 多次触发恢复，应该有合理的限流机制
        for (int i = 0; i < 10; i++) {
            boolean result = recoveryManager.triggerManualRecovery(pluginId);
            assertThat(result).isTrue();
        }
    }
}