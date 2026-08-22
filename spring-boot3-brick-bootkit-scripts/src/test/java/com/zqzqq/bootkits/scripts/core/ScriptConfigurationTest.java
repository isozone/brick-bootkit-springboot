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


package com.zqzqq.bootkits.scripts.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 脚本执行配置测试
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("脚本执行配置测试")
class ScriptConfigurationTest {

    @Test
    @DisplayName("测试默认配置创建")
    void testDefaultConfiguration() {
        ScriptConfiguration config = ScriptConfiguration.defaultConfiguration();
        
        assertThat(config).isNotNull();
        assertThat(config.getTimeoutMs()).isEqualTo(30000);
        assertThat(config.getEncoding()).isEqualTo("UTF-8");
        assertThat(config.isMergeOutputStreams()).isTrue();
        assertThat(config.isDebugMode()).isFalse();
        assertThat(config.getMaxOutputSize()).isEqualTo(10 * 1024 * 1024);
        assertThat(config.getEnvironmentVariables()).isNotNull();
        assertThat(config.getEnvironmentVariables()).isEmpty();
    }

    @Test
    @DisplayName("测试自定义超时配置")
    void testCustomTimeoutConfiguration() {
        long customTimeout = 60000;
        ScriptConfiguration config = new ScriptConfiguration(customTimeout);
        
        assertThat(config.getTimeoutMs()).isEqualTo(customTimeout);
        assertThat(config.getEncoding()).isEqualTo("UTF-8");
        assertThat(config.isMergeOutputStreams()).isTrue();
    }

    @Test
    @DisplayName("测试工作目录配置")
    void testWorkingDirectoryConfiguration() {
        String workingDir = "/tmp/scripts";
        ScriptConfiguration config = new ScriptConfiguration(30000, workingDir);
        
        assertThat(config.getTimeoutMs()).isEqualTo(30000);
        assertThat(config.getWorkingDirectory()).isEqualTo(workingDir);
    }

    @Test
    @DisplayName("测试环境变量配置")
    void testEnvironmentVariablesConfiguration() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("JAVA_HOME", "/opt/java");
        envVars.put("MAVEN_HOME", "/opt/maven");
        
        ScriptConfiguration config = new ScriptConfiguration();
        config.setEnvironmentVariables(envVars);
        
        assertThat(config.getEnvironmentVariables()).isEqualTo(envVars);
        assertThat(config.getEnvironmentVariables().get("JAVA_HOME")).isEqualTo("/opt/java");
        assertThat(config.getEnvironmentVariables().get("MAVEN_HOME")).isEqualTo("/opt/maven");
    }

    @Test
    @DisplayName("测试添加环境变量")
    void testAddEnvironmentVariable() {
        ScriptConfiguration config = new ScriptConfiguration();
        
        config.addEnvironmentVariable("KEY1", "value1");
        config.addEnvironmentVariable("KEY2", "value2");
        
        assertThat(config.getEnvironmentVariables()).hasSize(2);
        assertThat(config.getEnvironmentVariables().get("KEY1")).isEqualTo("value1");
        assertThat(config.getEnvironmentVariables().get("KEY2")).isEqualTo("value2");
    }

    @Test
    @DisplayName("test null environment variables handling")
    void testNullEnvironmentVariablesHandling() {
        ScriptConfiguration config = new ScriptConfiguration();
        config.setEnvironmentVariables(null);

        assertThat(config.getEnvironmentVariables()).isNotNull();
        assertThat(config.getEnvironmentVariables()).isEmpty();

        config.addEnvironmentVariable("KEY", "value");
        assertThat(config.getEnvironmentVariables().get("KEY")).isEqualTo("value");
    }

    @Test
    @DisplayName("测试输出流合并配置")
    void testOutputStreamsMergeConfiguration() {
        ScriptConfiguration config = new ScriptConfiguration();
        
        // 测试默认值
        assertThat(config.isMergeOutputStreams()).isTrue();
        
        // 测试设置为false
        config.setMergeOutputStreams(false);
        assertThat(config.isMergeOutputStreams()).isFalse();
        
        // 测试链式调用
        config.setMergeOutputStreams(true);
        assertThat(config.isMergeOutputStreams()).isTrue();
    }

    @Test
    @DisplayName("测试字符编码配置")
    void testEncodingConfiguration() {
        ScriptConfiguration config = new ScriptConfiguration();
        
        // 测试默认值
        assertThat(config.getEncoding()).isEqualTo("UTF-8");
        
        // 测试自定义编码
        String customEncoding = "GBK";
        config.setEncoding(customEncoding);
        assertThat(config.getEncoding()).isEqualTo(customEncoding);
    }

    @Test
    @DisplayName("测试调试模式配置")
    void testDebugModeConfiguration() {
        ScriptConfiguration config = new ScriptConfiguration();
        
        // 测试默认值
        assertThat(config.isDebugMode()).isFalse();
        
        // 测试启用调试模式
        config.setDebugMode(true);
        assertThat(config.isDebugMode()).isTrue();
        
        // 测试链式调用
        config.setDebugMode(false);
        assertThat(config.isDebugMode()).isFalse();
    }

    @Test
    @DisplayName("测试最大输出大小配置")
    void testMaxOutputSizeConfiguration() {
        ScriptConfiguration config = new ScriptConfiguration();
        
        // 测试默认值 (10MB)
        assertThat(config.getMaxOutputSize()).isEqualTo(10 * 1024 * 1024);
        
        // 测试自定义大小
        long customSize = 5 * 1024 * 1024; // 5MB
        config.setMaxOutputSize(customSize);
        assertThat(config.getMaxOutputSize()).isEqualTo(customSize);
        
        // 测试链式调用
        config.setMaxOutputSize(20 * 1024 * 1024); // 20MB
        assertThat(config.getMaxOutputSize()).isEqualTo(20 * 1024 * 1024);
    }

    @Test
    @DisplayName("测试用户和组配置")
    void testUserAndGroupConfiguration() {
        ScriptConfiguration config = new ScriptConfiguration();
        
        // 测试默认值
        assertThat(config.getUser()).isNull();
        assertThat(config.getGroup()).isNull();
        
        // 测试设置用户
        String testUser = "testuser";
        config.setUser(testUser);
        assertThat(config.getUser()).isEqualTo(testUser);
        
        // 测试设置组
        String testGroup = "testgroup";
        config.setGroup(testGroup);
        assertThat(config.getGroup()).isEqualTo(testGroup);
    }

    @Test
    @DisplayName("测试链式调用")
    void testMethodChaining() {
        ScriptConfiguration config = new ScriptConfiguration()
                .setTimeoutMs(60000)
                .setWorkingDirectory("/tmp/test")
                .setMergeOutputStreams(false)
                .setEncoding("GBK")
                .setDebugMode(true)
                .setMaxOutputSize(5 * 1024 * 1024)
                .setUser("testuser")
                .setGroup("testgroup");
        
        assertThat(config.getTimeoutMs()).isEqualTo(60000);
        assertThat(config.getWorkingDirectory()).isEqualTo("/tmp/test");
        assertThat(config.isMergeOutputStreams()).isFalse();
        assertThat(config.getEncoding()).isEqualTo("GBK");
        assertThat(config.isDebugMode()).isTrue();
        assertThat(config.getMaxOutputSize()).isEqualTo(5 * 1024 * 1024);
        assertThat(config.getUser()).isEqualTo("testuser");
        assertThat(config.getGroup()).isEqualTo("testgroup");
    }

    @Test
    @DisplayName("测试静态工厂方法")
    void testStaticFactoryMethods() {
        // 测试默认配置
        ScriptConfiguration defaultConfig = ScriptConfiguration.defaultConfiguration();
        assertThat(defaultConfig.getTimeoutMs()).isEqualTo(30000);
        assertThat(defaultConfig.isDebugMode()).isFalse();
        
        // 测试快速配置
        ScriptConfiguration quickConfig = ScriptConfiguration.quickConfiguration();
        assertThat(quickConfig.getTimeoutMs()).isEqualTo(5000);
        assertThat(quickConfig.isDebugMode()).isFalse();
        
        // 测试调试配置
        ScriptConfiguration debugConfig = ScriptConfiguration.debugConfiguration();
        assertThat(debugConfig.getTimeoutMs()).isEqualTo(60000);
        assertThat(debugConfig.isDebugMode()).isTrue();
    }

    @Test
    @DisplayName("测试toString方法")
    void testToStringMethod() {
        ScriptConfiguration config = new ScriptConfiguration()
                .setTimeoutMs(45000)
                .setWorkingDirectory("/custom/path")
                .setDebugMode(true);
        
        String configString = config.toString();
        
        assertThat(configString).isNotNull();
        assertThat(configString).contains("timeoutMs=45000");
        assertThat(configString).contains("workingDirectory='/custom/path'");
        assertThat(configString).contains("debugMode=true");
    }

    @Test
    @DisplayName("测试环境变量可变性问题")
    void testEnvironmentVariablesMutability() {
        ScriptConfiguration config = new ScriptConfiguration();
        Map<String, String> envVars = new HashMap<>();
        envVars.put("TEST", "value");
        
        // 设置外部映射
        config.setEnvironmentVariables(envVars);
        
        // 修改外部映射
        envVars.put("NEW_KEY", "new_value");
        
        // 验证配置中的环境变量也发生了变化
        assertThat(config.getEnvironmentVariables()).containsKey("NEW_KEY");
        assertThat(config.getEnvironmentVariables().get("NEW_KEY")).isEqualTo("new_value");
    }

    @Test
    @DisplayName("测试配置对象独立性")
    void testConfigurationIndependence() {
        ScriptConfiguration config1 = new ScriptConfiguration();
        config1.setTimeoutMs(60000);
        
        ScriptConfiguration config2 = new ScriptConfiguration();
        config2.setTimeoutMs(30000);
        
        // 验证两个配置对象独立
        assertThat(config1.getTimeoutMs()).isNotEqualTo(config2.getTimeoutMs());
        assertThat(config1.getTimeoutMs()).isEqualTo(60000);
        assertThat(config2.getTimeoutMs()).isEqualTo(30000);
    }
}
