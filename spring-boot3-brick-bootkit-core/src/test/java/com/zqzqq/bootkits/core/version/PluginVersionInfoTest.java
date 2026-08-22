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


package com.zqzqq.bootkits.core.version;

import com.zqzqq.bootkits.core.dependency.VersionConstraint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 插件版本信息测试
 */
@DisplayName("PluginVersionInfo Test")
class PluginVersionInfoTest {

    @Test
    @DisplayName("测试创建基本版本信息")
    void testBasicPluginVersionInfo() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0").build();
        
        assertThat(info.getPluginId()).isEqualTo("test-plugin");
        assertThat(info.getVersion().toString()).isEqualTo("1.0.0");
    }

    @Test
    @DisplayName("测试添加必需依赖")
    void testAddRequiredDependencies() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .addRequiredPlugin("dep-plugin-1")
            .addRequiredPlugin("dep-plugin-2")
            .build();
        
        List<String> requiredPlugins = info.getRequiredPlugins();
        assertThat(requiredPlugins).containsExactlyInAnyOrder("dep-plugin-1", "dep-plugin-2");
    }

    @Test
    @DisplayName("测试添加可选依赖")
    void testAddOptionalDependencies() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .addOptionalPlugin("opt-plugin-1")
            .addOptionalPlugin("opt-plugin-2")
            .build();
        
        List<String> optionalPlugins = info.getOptionalPlugins();
        assertThat(optionalPlugins).containsExactlyInAnyOrder("opt-plugin-1", "opt-plugin-2");
    }

    @Test
    @DisplayName("测试添加版本约束")
    void testAddVersionConstraints() {
        VersionConstraint constraint = VersionConstraint.parse(">=2.0.0");
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .addVersionConstraint("target-plugin", constraint)
            .build();
        
        Map<String, VersionConstraint> constraints = info.getVersionConstraints();
        assertThat(constraints).hasSize(1);
        assertThat(constraints.get("target-plugin")).isEqualTo(constraint);
    }

    @Test
    @DisplayName("测试添加元数据")
    void testAddMetadata() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .addMetadata("author", "test-author")
            .addMetadata("description", "test description")
            .build();
        
        Map<String, String> metadata = info.getMetadata();
        assertThat(metadata.get("author")).isEqualTo("test-author");
        assertThat(metadata.get("description")).isEqualTo("test description");
    }

    @Test
    @DisplayName("测试设置框架版本")
    void testFrameworkVersionSettings() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .setMinimumFrameworkVersion("3.0.0")
            .setMaximumFrameworkVersion("4.0.0")
            .setTargetFrameworkVersion("3.5.0")
            .build();
        
        assertThat(info.getMinimumFrameworkVersion()).isEqualTo("3.0.0");
        assertThat(info.getMaximumFrameworkVersion()).isEqualTo("4.0.0");
        assertThat(info.getTargetFrameworkVersion()).isEqualTo("3.5.0");
    }

    @Test
    @DisplayName("测试添加冲突")
    void testAddConflicts() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .addConflict("conflict-plugin-1")
            .addConflict("conflict-plugin-2")
            .build();
        
        List<String> conflicts = info.getConflicts();
        assertThat(conflicts).containsExactlyInAnyOrder("conflict-plugin-1", "conflict-plugin-2");
    }

    @Test
    @DisplayName("测试标记为已弃用")
    void testMarkAsDeprecated() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .setDeprecated("This plugin is deprecated")
            .build();
        
        assertThat(info.isDeprecated()).isTrue();
        assertThat(info.getDeprecationMessage()).isEqualTo("This plugin is deprecated");
    }

    @Test
    @DisplayName("测试不可变性")
    void testImmutability() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .addRequiredPlugin("dep-plugin")
            .build();
        
        List<String> requiredPlugins = info.getRequiredPlugins();
        assertThat(requiredPlugins).isNotSameAs(info.getRequiredPlugins());
        assertThat(requiredPlugins).isEqualTo(info.getRequiredPlugins());
    }

    @Test
    @DisplayName("测试字符串表示")
    void testToString() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .addRequiredPlugin("dep-plugin")
            .build();
        
        String str = info.toString();
        assertThat(str).contains("test-plugin");
        assertThat(str).contains("1.0.0");
        assertThat(str).contains("dep-plugin");
    }

    @Test
    @DisplayName("测试构建器参数验证")
    void testBuilderParameterValidation() {
        assertThatThrownBy(() -> PluginVersionInfo.newBuilder(null, "1.0.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("插件ID不能为空");
            
        assertThatThrownBy(() -> PluginVersionInfo.newBuilder("", "1.0.0"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("插件ID不能为空");
            
        assertThatThrownBy(() -> PluginVersionInfo.newBuilder("test-plugin", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("插件版本不能为空");
            
        assertThatThrownBy(() -> PluginVersionInfo.newBuilder("test-plugin", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("插件版本不能为空");
    }

    @Test
    @DisplayName("测试获取插件ID和版本")
    void testGetPluginIdAndVersion() {
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "2.1.3").build();
        
        assertThat(info.getPluginId()).isEqualTo("test-plugin");
        assertThat(info.getVersion().toString()).isEqualTo("2.1.3");
    }

    @Test
    @DisplayName("测试复制版本约束")
    void testCopyVersionConstraints() {
        VersionConstraint constraint = VersionConstraint.parse(">=1.0.0");
        PluginVersionInfo info = PluginVersionInfo.newBuilder("test-plugin", "1.0.0")
            .addVersionConstraint("target", constraint)
            .build();
        
        Map<String, VersionConstraint> originalConstraints = info.getVersionConstraints();
        Map<String, VersionConstraint> copiedConstraints = new HashMap<>(originalConstraints);
        
        copiedConstraints.put("new-target", VersionConstraint.parse(">=2.0.0"));
        
        assertThat(originalConstraints).hasSize(1);
        assertThat(copiedConstraints).hasSize(2);
    }
}