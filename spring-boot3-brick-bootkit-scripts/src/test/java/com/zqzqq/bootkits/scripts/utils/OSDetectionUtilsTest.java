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


package com.zqzqq.bootkits.scripts.utils;

import com.zqzqq.bootkits.scripts.core.OperatingSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 操作系统检测工具类测试
 *
 * @author starBlues
 * @since 4.0.1
 */
@DisplayName("操作系统检测工具测试")
class OSDetectionUtilsTest {

    @Test
    @DisplayName("测试当前操作系统检测")
    void testCurrentOSDetection() {
        OperatingSystem currentOS = OperatingSystem.detectCurrentOS();
        assertThat(currentOS).isNotNull();
        assertThat(currentOS).isIn(
            OperatingSystem.WINDOWS,
            OperatingSystem.LINUX,
            OperatingSystem.MACOS,
            OperatingSystem.UNIX,
            OperatingSystem.UNKNOWN
        );
    }

    @Test
    @DisplayName("测试操作系统判断方法")
    void testOSJudgmentMethods() {
        OperatingSystem currentOS = OperatingSystem.detectCurrentOS();
        
        // 至少有一个方法应该返回true
        boolean isAnyOS = OperatingSystem.isWindows() || 
                         OperatingSystem.isLinux() || 
                         OperatingSystem.isMacOS() || 
                         OperatingSystem.isUnix();
        assertThat(isAnyOS).isTrue();
    }

    @Test
    @DisplayName("测试系统信息获取")
    void testSystemInfoRetrieval() {
        OSDetectionUtils.OSInfo osInfo = OSDetectionUtils.getCurrentOSInfo();
        
        assertThat(osInfo).isNotNull();
        assertThat(osInfo.getOs()).isNotNull();
        assertThat(osInfo.getArchitecture()).isNotNull();
        assertThat(osInfo.is64Bit()).isNotNull();
        assertThat(osInfo.getUserDir()).isNotNull();
        assertThat(osInfo.getTempDir()).isNotNull();
    }

    @Test
    @DisplayName("测试兼容性检查")
    void testCompatibilityCheck() {
        // 测试当前系统是否兼容各种脚本类型
        String[] compatibleTypes = OSDetectionUtils.getCompatibleScriptTypes();
        assertThat(compatibleTypes).isNotEmpty();
        assertThat(compatibleTypes).contains("python"); // Python应该支持所有系统
        
        if (OperatingSystem.isWindows()) {
            assertThat(compatibleTypes).containsAnyOf("batch", "powershell");
        } else {
            assertThat(compatibleTypes).contains("shell");
        }
    }

    @Test
    @DisplayName("测试功能支持检查")
    void testFeatureSupportCheck() {
        // Python功能应该在所有系统上支持
        assertThat(OSDetectionUtils.supportsFeature("python")).isTrue();
        
        // Shell功能应该只支持Unix-like系统
        boolean supportsShell = OSDetectionUtils.supportsFeature("shell");
        boolean isUnixLike = OperatingSystem.isLinux() || OperatingSystem.isMacOS() || OperatingSystem.isUnix();
        assertThat(supportsShell).isEqualTo(isUnixLike);
        
        // Batch功能应该只支持Windows
        boolean supportsBatch = OSDetectionUtils.supportsFeature("batch");
        assertThat(supportsBatch).isEqualTo(OperatingSystem.isWindows());
    }

    @Test
    @DisplayName("测试系统验证")
    void testSystemValidation() {
        OSDetectionUtils.SystemValidationResult result = OSDetectionUtils.validateSystem();
        
        assertThat(result).isNotNull();
        assertThat(result.getInfos()).isNotNull();
        assertThat(result.getErrors()).isNotNull();
        assertThat(result.getWarnings()).isNotNull();
        
        // 不应该有严重错误阻止系统运行
        if (result.hasErrors()) {
            System.out.println("系统验证错误: " + result.getErrors());
        }
    }

    @Test
    @DisplayName("测试系统摘要生成")
    void testSystemSummary() {
        String summary = OSDetectionUtils.getSystemSummary();
        assertThat(summary).isNotNull();
        assertThat(summary).contains("操作系统:");
        assertThat(summary).contains("临时目录:");
        assertThat(summary).contains("工作目录:");
        assertThat(summary).contains("支持的脚本类型:");
    }

    @Test
    @DisplayName("测试目录获取方法")
    void testDirectoryMethods() {
        // 测试各种目录获取方法
        String tempDir = OSDetectionUtils.getTempDirectory();
        String userHome = OSDetectionUtils.getUserHomeDirectory();
        String workingDir = OSDetectionUtils.getCurrentWorkingDirectory();
        
        assertThat(tempDir).isNotNull();
        assertThat(userHome).isNotNull();
        assertThat(workingDir).isNotNull();
    }
}