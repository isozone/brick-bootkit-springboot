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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * 插件健康检查器接口测试
 */
@DisplayName("PluginHealthChecker Test")
class PluginHealthCheckerTest {

    @Mock
    private Plugin mockPlugin;

    @Mock
    private PluginHealthChecker mockChecker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("测试检查器名称和描述")
    void testCheckerNameAndDescription() {
        String name = "test-checker";
        String description = "Test health checker";

        when(mockChecker.getName()).thenReturn(name);
        when(mockChecker.getDescription()).thenReturn(description);

        assertThat(mockChecker.getName()).isEqualTo(name);
        assertThat(mockChecker.getDescription()).isEqualTo(description);
    }

    @Test
    @DisplayName("测试检查器启用状态")
    void testCheckerEnabledState() {
        when(mockChecker.isEnabled()).thenReturn(true);
        assertThat(mockChecker.isEnabled()).isTrue();

        doNothing().when(mockChecker).setEnabled(false);
        mockChecker.setEnabled(false);
        verify(mockChecker, times(1)).setEnabled(false);

        when(mockChecker.isEnabled()).thenReturn(false);
        assertThat(mockChecker.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("测试超时时间设置")
    void testTimeoutSettings() {
        long timeout = 5000L;

        doNothing().when(mockChecker).setTimeoutMillis(timeout);
        mockChecker.setTimeoutMillis(timeout);

        when(mockChecker.getTimeoutMillis()).thenReturn(timeout);
        assertThat(mockChecker.getTimeoutMillis()).isEqualTo(timeout);
    }

    @Test
    @DisplayName("测试健康检查方法签名")
    void testHealthCheckMethodSignatures() {
        // 验证快速检查
        PluginHealthStatus status = PluginHealthStatus.HEALTHY;
        when(mockChecker.quickHealthCheck(mockPlugin)).thenReturn(status);
        assertThat(mockChecker.quickHealthCheck(mockPlugin)).isEqualTo(status);
        
        // 验证完整检查
        PluginHealthReport mockReport = PluginHealthReport.createHealthy("test", Collections.emptyList());
        when(mockChecker.checkHealth(mockPlugin)).thenReturn(mockReport);
        assertThat(mockChecker.checkHealth(mockPlugin)).isEqualTo(mockReport);
    }

    @Test
    @DisplayName("测试检查器状态变化")
    void testCheckerStateChanges() {
        // 初始状态为启用
        when(mockChecker.isEnabled()).thenReturn(true);
        assertThat(mockChecker.isEnabled()).isTrue();

        // 禁用检查器
        doNothing().when(mockChecker).setEnabled(false);
        mockChecker.setEnabled(false);
        verify(mockChecker).setEnabled(false);

        // 验证状态变更
        when(mockChecker.isEnabled()).thenReturn(false);
        assertThat(mockChecker.isEnabled()).isFalse();

        // 重新启用
        doNothing().when(mockChecker).setEnabled(true);
        mockChecker.setEnabled(true);
        verify(mockChecker).setEnabled(true);

        when(mockChecker.isEnabled()).thenReturn(true);
        assertThat(mockChecker.isEnabled()).isTrue();
    }
}