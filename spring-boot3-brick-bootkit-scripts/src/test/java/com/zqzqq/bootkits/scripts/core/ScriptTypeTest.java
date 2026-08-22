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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 脚本类型测试
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("脚本类型测试")
class ScriptTypeTest {

    @Test
    @DisplayName("测试脚本类型枚举值")
    void testScriptTypeValues() {
        ScriptType[] types = ScriptType.values();
        assertThat(types).isNotEmpty();
        assertThat(types).contains(
            ScriptType.SHELL,
            ScriptType.BATCH,
            ScriptType.POWERSHELL,
            ScriptType.LUA,
            ScriptType.PYTHON,
            ScriptType.RUBY,
            ScriptType.PERL,
            ScriptType.JAVASCRIPT,
            ScriptType.NODEJS,
            ScriptType.GROOVY,
            ScriptType.EXECUTABLE
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "script.sh", "test.sh", "deploy.sh", "backup.sh"
    })
    @DisplayName("测试Shell脚本类型检测")
    void testShellScriptDetection(String fileName) {
        ScriptType detectedType = ScriptType.fromFileName(fileName);
        assertThat(detectedType).isEqualTo(ScriptType.SHELL);
        assertThat(detectedType.getExtension()).isEqualTo(".sh");
        assertThat(detectedType.getType()).isEqualTo("shell");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "script.bat", "test.bat", "install.bat", "setup.bat", "openclaw.cmd", "OPENCLAW.CMD"
    })
    @DisplayName("测试批处理脚本类型检测")
    void testBatchScriptDetection(String fileName) {
        ScriptType detectedType = ScriptType.fromFileName(fileName);
        assertThat(detectedType).isEqualTo(ScriptType.BATCH);
        assertThat(detectedType.getExtension()).isEqualTo(".bat");
        assertThat(detectedType.getType()).isEqualTo("batch");
    }

    @Test
    @DisplayName("测试Windows CMD完整路径类型检测")
    void testCmdPathDetection() {
        ScriptType detectedType = ScriptType.fromFileName("E:\\codes\\openclaw\\dist\\window\\openclaw-portable-win-x64\\openclaw.cmd");
        assertThat(detectedType).isEqualTo(ScriptType.BATCH);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "script.ps1", "test.ps1", "deploy.ps1"
    })
    @DisplayName("测试PowerShell脚本类型检测")
    void testPowerShellScriptDetection(String fileName) {
        ScriptType detectedType = ScriptType.fromFileName(fileName);
        assertThat(detectedType).isEqualTo(ScriptType.POWERSHELL);
        assertThat(detectedType.getExtension()).isEqualTo(".ps1");
        assertThat(detectedType.getType()).isEqualTo("powershell");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "script.py", "test.py", "main.py", "runner.py"
    })
    @DisplayName("测试Python脚本类型检测")
    void testPythonScriptDetection(String fileName) {
        ScriptType detectedType = ScriptType.fromFileName(fileName);
        assertThat(detectedType).isEqualTo(ScriptType.PYTHON);
        assertThat(detectedType.getExtension()).isEqualTo(".py");
        assertThat(detectedType.getType()).isEqualTo("python");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "script.js", "test.js", "main.js", "runner.js"
    })
    @DisplayName("测试JavaScript脚本类型检测")
    void testJavaScriptScriptDetection(String fileName) {
        ScriptType detectedType = ScriptType.fromFileName(fileName);
        assertThat(detectedType).isEqualTo(ScriptType.JAVASCRIPT);
        assertThat(detectedType.getExtension()).isEqualTo(".js");
        assertThat(detectedType.getType()).isEqualTo("javascript");
    }

    @Test
    @DisplayName("测试未知文件类型")
    void testUnknownFileType() {
        ScriptType detectedType = ScriptType.fromFileName("unknown.txt");
        assertThat(detectedType).isNull();
    }

    @Test
    @DisplayName("测试空文件名")
    void testEmptyFileName() {
        assertThat(ScriptType.fromFileName(null)).isNull();
        assertThat(ScriptType.fromFileName("")).isNull();
        assertThat(ScriptType.fromFileName("   ")).isNull();
    }

    @Test
    @DisplayName("测试类型名称检测")
    void testTypeNameDetection() {
        assertThat(ScriptType.fromTypeName("shell")).isEqualTo(ScriptType.SHELL);
        assertThat(ScriptType.fromTypeName("batch")).isEqualTo(ScriptType.BATCH);
        assertThat(ScriptType.fromTypeName("powershell")).isEqualTo(ScriptType.POWERSHELL);
        assertThat(ScriptType.fromTypeName("python")).isEqualTo(ScriptType.PYTHON);
        assertThat(ScriptType.fromTypeName("javascript")).isEqualTo(ScriptType.JAVASCRIPT);
        assertThat(ScriptType.fromTypeName("executable")).isEqualTo(ScriptType.EXECUTABLE);
    }

    @Test
    @DisplayName("测试不区分大小写的类型名称检测")
    void testCaseInsensitiveTypeNameDetection() {
        assertThat(ScriptType.fromTypeName("SHELL")).isEqualTo(ScriptType.SHELL);
        assertThat(ScriptType.fromTypeName("Shell")).isEqualTo(ScriptType.SHELL);
        assertThat(ScriptType.fromTypeName("sHeLl")).isEqualTo(ScriptType.SHELL);
        
        assertThat(ScriptType.fromTypeName("PYTHON")).isEqualTo(ScriptType.PYTHON);
        assertThat(ScriptType.fromTypeName("Python")).isEqualTo(ScriptType.PYTHON);
        assertThat(ScriptType.fromTypeName("pYtHoN")).isEqualTo(ScriptType.PYTHON);
    }

    @Test
    @DisplayName("测试无效类型名称")
    void testInvalidTypeName() {
        assertThat(ScriptType.fromTypeName(null)).isNull();
        assertThat(ScriptType.fromTypeName("")).isNull();
        assertThat(ScriptType.fromTypeName("unknown")).isNull();
        assertThat(ScriptType.fromTypeName("php")).isNull();
    }

    @Test
    @DisplayName("测试扩展名和类型一致性")
    void testExtensionAndTypeConsistency() {
        for (ScriptType scriptType : ScriptType.values()) {
            assertThat(scriptType.getExtension()).isNotNull();
            assertThat(scriptType.getType()).isNotNull();
            
            // 验证扩展名格式
            if (!scriptType.getExtension().isEmpty()) {
                assertThat(scriptType.getExtension()).startsWith(".");
            }
            
            // 验证类型名称不为空
            assertThat(scriptType.getType()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("测试所有扩展名唯一性")
    void testExtensionUniqueness() {
        String[] extensions = java.util.Arrays.stream(ScriptType.values())
            .map(ScriptType::getExtension)
            .filter(ext -> !ext.isEmpty())
            .toArray(String[]::new);
        
        // 验证所有非空扩展名都是唯一的
        java.util.Map<String, Long> extensionCount = java.util.Arrays.stream(extensions)
            .collect(java.util.stream.Collectors.groupingBy(ext -> ext, java.util.stream.Collectors.counting()));

        assertThat(extensionCount.get(".js")).isEqualTo(2L);

        long duplicatedExtensionKinds = extensionCount.values().stream()
            .filter(count -> count > 1)
            .count();
        assertThat(duplicatedExtensionKinds).isEqualTo(1L);
    }

    @Test
    @DisplayName("测试类型名称唯一性")
    void testTypeNameUniqueness() {
        String[] typeNames = java.util.Arrays.stream(ScriptType.values())
            .map(ScriptType::getType)
            .toArray(String[]::new);
        
        // 验证所有类型名称都是唯一的
        long uniqueTypes = java.util.Arrays.stream(typeNames)
            .distinct()
            .count();
        
        assertThat(uniqueTypes).isEqualTo(typeNames.length);
    }
}
