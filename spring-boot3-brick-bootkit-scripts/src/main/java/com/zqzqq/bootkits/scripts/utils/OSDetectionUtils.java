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

/**
 * 操作系统检测工具类
 * 提供详细的操作系统检测功能
 *
 * @author starBlues
 * @since 4.0.1
 */
public class OSDetectionUtils {
    
    /**
     * 操作系统详细信息
     */
    public static class OSInfo {
        private final OperatingSystem os;
        private final String version;
        private final String architecture;
        private final String name;
        private final boolean is64Bit;
        private final String userDir;
        private final String tempDir;
        private final String lineSeparator;
        private final String fileSeparator;
        private final String pathSeparator;
        
        public OSInfo(OperatingSystem os, String version, String architecture, String name, 
                     boolean is64Bit, String userDir, String tempDir, 
                     String lineSeparator, String fileSeparator, String pathSeparator) {
            this.os = os;
            this.version = version;
            this.architecture = architecture;
            this.name = name;
            this.is64Bit = is64Bit;
            this.userDir = userDir;
            this.tempDir = tempDir;
            this.lineSeparator = lineSeparator;
            this.fileSeparator = fileSeparator;
            this.pathSeparator = pathSeparator;
        }
        
        // Getter methods
        public OperatingSystem getOs() { return os; }
        public String getVersion() { return version; }
        public String getArchitecture() { return architecture; }
        public String getName() { return name; }
        public boolean is64Bit() { return is64Bit; }
        public String getUserDir() { return userDir; }
        public String getTempDir() { return tempDir; }
        public String getLineSeparator() { return lineSeparator; }
        public String getFileSeparator() { return fileSeparator; }
        public String getPathSeparator() { return pathSeparator; }
        
        @Override
        public String toString() {
            return String.format("OSInfo{os=%s, name='%s', version='%s', architecture='%s', is64Bit=%s}",
                    os, name, version, architecture, is64Bit);
        }
    }
    
    /**
     * 获取当前操作系统详细信息
     *
     * @return 操作系统详细信息
     */
    public static OSInfo getCurrentOSInfo() {
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String userDir = System.getProperty("user.dir");
        String tempDir = System.getProperty("java.io.tmpdir");
        String lineSeparator = System.getProperty("line.separator");
        String fileSeparator = System.getProperty("file.separator");
        String pathSeparator = System.getProperty("path.separator");
        
        OperatingSystem os = OperatingSystem.detectCurrentOS();
        boolean is64Bit = is64BitArchitecture(osArch);
        
        return new OSInfo(os, osVersion, osArch, osName, is64Bit, 
                         userDir, tempDir, lineSeparator, fileSeparator, pathSeparator);
    }
    
    /**
     * 检测是否为64位架构
     *
     * @param architecture 架构信息
     * @return 是否为64位
     */
    private static boolean is64BitArchitecture(String architecture) {
        if (architecture == null || architecture.isEmpty()) {
            return false;
        }
        
        String lowerArch = architecture.toLowerCase();
        return lowerArch.contains("64") || lowerArch.equals("amd64") || 
               lowerArch.equals("x86_64") || lowerArch.equals("aarch64");
    }
    
    /**
     * 获取系统临时目录
     *
     * @return 临时目录路径
     */
    public static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }
    
    /**
     * 获取用户主目录
     *
     * @return 用户主目录路径
     */
    public static String getUserHomeDirectory() {
        return System.getProperty("user.home");
    }
    
    /**
     * 获取当前工作目录
     *
     * @return 当前工作目录路径
     */
    public static String getCurrentWorkingDirectory() {
        return System.getProperty("user.dir");
    }
    
    /**
     * 检测是否支持特定的脚本功能
     *
     * @param feature 功能名称
     * @return 是否支持
     */
    public static boolean supportsFeature(String feature) {
        OperatingSystem currentOS = OperatingSystem.detectCurrentOS();
        
        switch (feature.toLowerCase()) {
            case "shell":
                return currentOS == OperatingSystem.LINUX || currentOS == OperatingSystem.MACOS || 
                       currentOS == OperatingSystem.UNIX;
            case "batch":
                return currentOS == OperatingSystem.WINDOWS;
            case "powershell":
                return currentOS == OperatingSystem.WINDOWS;
            case "python":
                return currentOS != OperatingSystem.UNKNOWN;
            case "executable":
                return currentOS != OperatingSystem.UNKNOWN;
            default:
                return false;
        }
    }
    
    /**
     * 获取系统兼容的脚本类型
     *
     * @return 兼容的脚本类型列表
     */
    public static String[] getCompatibleScriptTypes() {
        OperatingSystem currentOS = OperatingSystem.detectCurrentOS();
        
        switch (currentOS) {
            case WINDOWS:
                return new String[]{"batch", "powershell", "python", "executable"};
            case LINUX:
            case MACOS:
            case UNIX:
                return new String[]{"shell", "python", "javascript", "executable"};
            default:
                return new String[]{"executable"};
        }
    }
    
    /**
     * 检测系统是否满足运行要求
     *
     * @param requiredOS 要求的操作系统
     * @return 是否满足
     */
    public static boolean isCompatibleWith(OperatingSystem requiredOS) {
        if (requiredOS == null || requiredOS == OperatingSystem.UNKNOWN) {
            return true; // 如果没有特定要求，则认为兼容
        }
        
        OperatingSystem currentOS = OperatingSystem.detectCurrentOS();
        return currentOS == requiredOS;
    }
    
    /**
     * 获取系统相关信息摘要
     *
     * @return 系统信息摘要
     */
    public static String getSystemSummary() {
        OSInfo osInfo = getCurrentOSInfo();
        return String.format("操作系统: %s %s (%s, %s-bit)%n" +
                           "临时目录: %s%n" +
                           "工作目录: %s%n" +
                           "支持的脚本类型: %s",
                osInfo.getName(), osInfo.getVersion(), osInfo.getOs().getDisplayName(),
                osInfo.is64Bit() ? "64" : "32",
                osInfo.getTempDir(),
                osInfo.getUserDir(),
                String.join(", ", getCompatibleScriptTypes()));
    }
    
    /**
     * 验证系统环境
     *
     * @return 验证结果
     */
    public static SystemValidationResult validateSystem() {
        SystemValidationResult result = new SystemValidationResult();
        
        OperatingSystem currentOS = OperatingSystem.detectCurrentOS();
        if (currentOS == OperatingSystem.UNKNOWN) {
            result.addError("无法识别的操作系统: " + System.getProperty("os.name"));
        } else {
            result.addInfo("检测到操作系统: " + currentOS.getDisplayName());
        }
        
        // 检查临时目录
        String tempDir = getTempDirectory();
        if (tempDir == null || tempDir.isEmpty()) {
            result.addError("临时目录未设置");
        } else {
            result.addInfo("临时目录: " + tempDir);
        }
        
        // 检查Java版本
        String javaVersion = System.getProperty("java.version");
        if (javaVersion != null && !javaVersion.isEmpty()) {
            result.addInfo("Java版本: " + javaVersion);
        }
        
        // 检查架构
        String arch = System.getProperty("os.arch");
        if (arch != null && !arch.isEmpty()) {
            result.addInfo("系统架构: " + arch + (is64BitArchitecture(arch) ? " (64位)" : " (32位)"));
        }
        
        return result;
    }
    
    /**
     * 系统验证结果
     */
    public static class SystemValidationResult {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        private final java.util.List<String> warnings = new java.util.ArrayList<>();
        private final java.util.List<String> infos = new java.util.ArrayList<>();
        
        public void addError(String error) { errors.add(error); }
        public void addWarning(String warning) { warnings.add(warning); }
        public void addInfo(String info) { infos.add(info); }
        
        public boolean hasErrors() { return !errors.isEmpty(); }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        
        public java.util.List<String> getErrors() { return new java.util.ArrayList<>(errors); }
        public java.util.List<String> getWarnings() { return new java.util.ArrayList<>(warnings); }
        public java.util.List<String> getInfos() { return new java.util.ArrayList<>(infos); }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("系统验证结果:\n");
            
            if (!infos.isEmpty()) {
                sb.append("信息:\n");
                for (String info : infos) {
                    sb.append("  - ").append(info).append("\n");
                }
            }
            
            if (!warnings.isEmpty()) {
                sb.append("警告:\n");
                for (String warning : warnings) {
                    sb.append("  ! ").append(warning).append("\n");
                }
            }
            
            if (!errors.isEmpty()) {
                sb.append("错误:\n");
                for (String error : errors) {
                    sb.append("  X ").append(error).append("\n");
                }
            }
            
            if (errors.isEmpty() && warnings.isEmpty()) {
                sb.append("系统验证通过，所有检查项都正常。");
            }
            
            return sb.toString();
        }
    }
}