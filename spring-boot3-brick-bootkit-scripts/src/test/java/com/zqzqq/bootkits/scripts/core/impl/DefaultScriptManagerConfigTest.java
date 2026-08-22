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


package com.zqzqq.bootkits.scripts.core.impl;

import com.zqzqq.bootkits.scripts.core.ScriptExecutionResult;
import com.zqzqq.bootkits.scripts.core.ScriptManager;
import com.zqzqq.bootkits.scripts.core.ScriptType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 脚本管理器配置测试
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("脚本管理器配置测试")
class DefaultScriptManagerConfigTest {

    @Test
    @DisplayName("测试多路径配置")
    void testMultiplePathsConfiguration() throws Exception {
        // 创建脚本管理器
        DefaultScriptManager scriptManager = new DefaultScriptManager();
        
        // 初始化管理器以加载默认路径
        scriptManager.initialize();
        
        // 添加多个路径
        scriptManager.addScriptPath("/custom/path1");
        scriptManager.addScriptPath("/custom/path2");
        scriptManager.addScriptPath("/custom/path3");
        
        List<String> paths = scriptManager.getScriptPaths();
        
        assertThat(paths).isNotEmpty();
        assertThat(paths.size()).isGreaterThanOrEqualTo(4); // 至少包含3个自定义路径 + 1个默认路径
        assertThat(paths).contains("/custom/path1", "/custom/path2", "/custom/path3");
    }

    @Test
    @DisplayName("测试路径重复添加")
    void testDuplicatePathHandling() {
        DefaultScriptManager scriptManager = new DefaultScriptManager();
        
        // 多次添加相同路径
        scriptManager.addScriptPath("/test/path");
        scriptManager.addScriptPath("/test/path");
        scriptManager.addScriptPath("/test/path");
        
        List<String> paths = scriptManager.getScriptPaths();
        
        // 验证重复路径只保留一个
        long count = paths.stream().filter("/test/path"::equals).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("测试路径移除")
    void testPathRemoval() {
        DefaultScriptManager scriptManager = new DefaultScriptManager();
        
        String testPath = "/test/remove/path";
        scriptManager.addScriptPath(testPath);
        
        // 验证路径已添加
        assertThat(scriptManager.getScriptPaths()).contains(testPath);
        
        // 移除路径
        scriptManager.removeScriptPath(testPath);
        
        // 验证路径已移除
        assertThat(scriptManager.getScriptPaths()).doesNotContain(testPath);
    }

    @Test
    @DisplayName("测试空路径处理")
    void testEmptyPathHandling() {
        DefaultScriptManager scriptManager = new DefaultScriptManager();
        
        int initialSize = scriptManager.getScriptPaths().size();
        
        // 添加空路径
        scriptManager.addScriptPath(null);
        scriptManager.addScriptPath("");
        scriptManager.addScriptPath("   ");
        
        // 验证大小没有变化
        assertThat(scriptManager.getScriptPaths().size()).isEqualTo(initialSize);
    }

    @Test
    @DisplayName("测试路径包含默认路径")
    void testDefaultPathsIncluded() throws Exception {
        DefaultScriptManager scriptManager = new DefaultScriptManager();
        
        // 初始化管理器以加载默认路径
        scriptManager.initialize();
        
        List<String> paths = scriptManager.getScriptPaths();
        
        // 验证包含默认路径
        assertThat(paths).contains(
            System.getProperty("user.dir"),
            System.getProperty("user.home"),
            System.getProperty("java.io.tmpdir")
        );
    }

    @Test
    @DisplayName("测试配置属性验证")
    void testConfigurationProperties() throws Exception {
        DefaultScriptManager scriptManager = new DefaultScriptManager();
        
        // 初始化管理器以注册执行器
        scriptManager.initialize();
        
        // 测试能够访问基本属性
        assertThat(scriptManager.getCurrentOperatingSystem()).isNotNull();
        assertThat(scriptManager.getRegisteredExecutors()).isNotEmpty();
    }

    @Test
    @DisplayName("测试脚本管理器初始化")
    void testManagerInitialization() throws Exception {
        DefaultScriptManager scriptManager = new DefaultScriptManager();
        
        // 测试初始化
        scriptManager.initialize();
        
        // 验证初始化后状态
        assertThat(scriptManager).isNotNull();
        assertThat(scriptManager.getCurrentOperatingSystem()).isNotNull();
        
        // 清理
        scriptManager.destroy();
    }

    @Test
    @DisplayName("测试管理器能识别CMD脚本")
    void testCmdScriptTypeDetection() {
        DefaultScriptManager scriptManager = new DefaultScriptManager();

        assertThat(scriptManager.detectScriptType("E:\\codes\\openclaw\\dist\\window\\openclaw-portable-win-x64\\openclaw.cmd"))
            .isEqualTo(ScriptType.BATCH);
    }

    @Test
    @DisplayName("测试执行单行命令")
    void testExecuteCommand() throws Exception {
        DefaultScriptManager scriptManager = new DefaultScriptManager();
        scriptManager.initialize();

        ScriptExecutionResult result = scriptManager.executeCommand("echo command-result");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResultValue()).contains("command-result");
    }
}
