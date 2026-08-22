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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本号工具类
 * 提供版本号解析、比较和验证功能
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public class VersionUtils {
    
    // 版本号正则表达式: major.minor.patch[-qualifier]
    private static final Pattern VERSION_PATTERN = Pattern.compile(
        "(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-]([a-zA-Z0-9-]+))?"
    );
    
    private static final Pattern SEMANTIC_VERSION_PATTERN = Pattern.compile(
        "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$"
    );
    
    /**
     * 解析版本号
     * 
     * @param versionString 版本号字符串
     * @return VersionInfo对象
     */
    public static VersionInfo parseVersion(String versionString) {
        if (versionString == null || versionString.trim().isEmpty()) {
            throw new IllegalArgumentException("版本号不能为空");
        }
        
        String trimmed = versionString.trim();
        
        // 首先尝试语义化版本格式
        SemanticVersion semver = parseSemanticVersion(trimmed);
        if (semver != null) {
            return new VersionInfo(trimmed, semver.getMajor(), semver.getMinor(), 
                                 semver.getPatch(), semver.getQualifier());
        }
        
        // 然后尝试传统版本格式
        TraditionalVersion tradver = parseTraditionalVersion(trimmed);
        if (tradver != null) {
            return new VersionInfo(trimmed, tradver.getMajor(), tradver.getMinor(), 
                                 tradver.getPatch(), tradver.getQualifier());
        }
        
        throw new IllegalArgumentException("无法解析版本号: " + versionString);
    }
    
    /**
     * 解析语义化版本
     */
    private static SemanticVersion parseSemanticVersion(String version) {
        Matcher matcher = SEMANTIC_VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return null;
        }
        
        try {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int patch = Integer.parseInt(matcher.group(3));
            String preRelease = matcher.group(4);
            String build = matcher.group(5);
            
            return new SemanticVersion(major, minor, patch, preRelease, build);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 解析传统版本
     */
    private static TraditionalVersion parseTraditionalVersion(String version) {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.matches()) {
            return null;
        }
        
        try {
            int major = Integer.parseInt(matcher.group(1));
            Integer minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            Integer patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            String qualifier = matcher.group(4);
            
            return new TraditionalVersion(major, minor, patch, qualifier);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 比较两个版本
     * 
     * @param version1 版本1
     * @param version2 版本2
     * @return 比较结果: -1小于, 0等于, 1大于
     */
    public static int compareVersions(String version1, String version2) {
        try {
            VersionInfo v1 = parseVersion(version1);
            VersionInfo v2 = parseVersion(version2);
            return v1.compareTo(v2);
        } catch (Exception e) {
            // 如果无法解析，使用字符串比较
            return version1.compareTo(version2);
        }
    }
    
    /**
     * 检查版本1是否兼容版本2
     * 
     * @param version1 当前版本
     * @param version2 目标版本
     * @return 是否兼容
     */
    public static boolean isCompatible(String version1, String version2) {
        try {
            VersionInfo v1 = parseVersion(version1);
            VersionInfo v2 = parseVersion(version2);
            
            // 主要版本号不兼容
            if (v1.getMajor() != v2.getMajor()) {
                return false;
            }
            
            // 次要版本号向后兼容
            return v1.getMinor() >= v2.getMinor();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查是否满足版本要求
     * 
     * @param currentVersion 当前版本
     * @param requiredVersion 要求版本
     * @param includePrerelease 是否包括预发布版本
     * @return 是否满足
     */
    public static boolean satisfies(String currentVersion, String requiredVersion, boolean includePrerelease) {
        try {
            VersionInfo current = parseVersion(currentVersion);
            VersionInfo required = parseVersion(requiredVersion);
            
            // 检查主版本和次版本
            if (current.getMajor() < required.getMajor()) {
                return false;
            }
            
            if (current.getMajor() == required.getMajor() && 
                current.getMinor() < required.getMinor()) {
                return false;
            }
            
            // 如果要求特定版本号
            if (required.getPatch() != null && 
                (current.getPatch() == null || current.getPatch() < required.getPatch())) {
                return false;
            }
            
            // 检查预发布版本
            if (required.hasPrerelease() && !includePrerelease) {
                return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 获取版本范围
     * 
     * @param minVersion 最小版本
     * @param maxVersion 最大版本
     * @return 版本范围字符串
     */
    public static String getVersionRange(String minVersion, String maxVersion) {
        return String.format(">=%s <%s", minVersion, maxVersion);
    }
    
    /**
     * 检查版本格式是否有效
     * 
     * @param version 版本号
     * @return 是否有效
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.trim().isEmpty()) {
            return false;
        }
        
        return parseSemanticVersion(version.trim()) != null || 
               parseTraditionalVersion(version.trim()) != null;
    }
    
    /**
     * 获取下一个补丁版本
     * 
     * @param version 当前版本
     * @return 下一个补丁版本
     */
    public static String getNextPatchVersion(String version) {
        try {
            VersionInfo v = parseVersion(version);
            int nextPatch = (v.getPatch() != null ? v.getPatch() : 0) + 1;
            return String.format("%d.%d.%d", v.getMajor(), v.getMinor(), nextPatch);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法获取下一补丁版本: " + version, e);
        }
    }
    
    /**
     * 获取下一个次版本
     * 
     * @param version 当前版本
     * @return 下一个次版本
     */
    public static String getNextMinorVersion(String version) {
        try {
            VersionInfo v = parseVersion(version);
            int nextMinor = v.getMinor() + 1;
            return String.format("%d.%d.0", v.getMajor(), nextMinor);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法获取下一版本: " + version, e);
        }
    }
    
    /**
     * 获取下一个主版本
     * 
     * @param version 当前版本
     * @return 下一个主版本
     */
    public static String getNextMajorVersion(String version) {
        try {
            VersionInfo v = parseVersion(version);
            int nextMajor = v.getMajor() + 1;
            return String.format("%d.0.0", nextMajor);
        } catch (Exception e) {
            throw new IllegalArgumentException("无法获取下一主版本: " + version, e);
        }
    }
    
    /**
     * 版本信息类
     */
    public static class VersionInfo implements Comparable<VersionInfo> {
        private final String original;
        private final int major;
        private final int minor;
        private final Integer patch;
        private final String qualifier;
        
        public VersionInfo(String original, int major, int minor, Integer patch, String qualifier) {
            this.original = original;
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.qualifier = qualifier;
        }
        
        public String getOriginal() {
            return original;
        }
        
        public int getMajor() {
            return major;
        }
        
        public int getMinor() {
            return minor;
        }
        
        public Integer getPatch() {
            return patch;
        }
        
        public String getQualifier() {
            return qualifier;
        }
        
        public boolean hasPrerelease() {
            return qualifier != null && !qualifier.isEmpty();
        }
        
        @Override
        public int compareTo(VersionInfo other) {
            // 首先比较主版本
            int majorCompare = Integer.compare(this.major, other.major);
            if (majorCompare != 0) {
                return majorCompare;
            }
            
            // 然后比较次版本
            int minorCompare = Integer.compare(this.minor, other.minor);
            if (minorCompare != 0) {
                return minorCompare;
            }
            
            // 比较补丁版本
            int thisPatch = this.patch != null ? this.patch : 0;
            int otherPatch = other.patch != null ? other.patch : 0;
            int patchCompare = Integer.compare(thisPatch, otherPatch);
            if (patchCompare != 0) {
                return patchCompare;
            }
            
            // 比较限定符
            if (this.qualifier == null && other.qualifier == null) {
                return 0;
            } else if (this.qualifier == null) {
                return 1; // 有限定符的版本大于无限定符的版本
            } else if (other.qualifier == null) {
                return -1;
            } else {
                return this.qualifier.compareTo(other.qualifier);
            }
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            VersionInfo that = (VersionInfo) obj;
            return major == that.major &&
                   minor == that.minor &&
                   (patch != null ? patch.equals(that.patch) : that.patch == null) &&
                   (qualifier != null ? qualifier.equals(that.qualifier) : that.qualifier == null);
        }
        
        @Override
        public int hashCode() {
            int result = major;
            result = 31 * result + minor;
            result = 31 * result + (patch != null ? patch.hashCode() : 0);
            result = 31 * result + (qualifier != null ? qualifier.hashCode() : 0);
            return result;
        }
        
        @Override
        public String toString() {
            if (qualifier != null) {
                return String.format("%d.%d.%d-%s", major, minor, 
                                   patch != null ? patch : 0, qualifier);
            } else {
                return String.format("%d.%d.%d", major, minor, 
                                   patch != null ? patch : 0);
            }
        }
    }
    
    /**
     * 语义化版本类
     */
    private static class SemanticVersion extends VersionInfo {
        private final String preRelease;
        private final String build;
        
        public SemanticVersion(int major, int minor, int patch, String preRelease, String build) {
            super(formatVersion(major, minor, patch, preRelease), major, minor, patch, preRelease);
            this.preRelease = preRelease;
            this.build = build;
        }
        
        public String getPreRelease() {
            return preRelease;
        }
        
        public String getBuild() {
            return build;
        }
        
        private static String formatVersion(int major, int minor, int patch, String preRelease) {
            String version = String.format("%d.%d.%d", major, minor, patch);
            if (preRelease != null) {
                version += "-" + preRelease;
            }
            return version;
        }
    }
    
    /**
     * 传统版本类
     */
    private static class TraditionalVersion extends VersionInfo {
        public TraditionalVersion(int major, int minor, int patch, String qualifier) {
            super(formatVersion(major, minor, patch, qualifier), major, minor, patch, qualifier);
        }
        
        private static String formatVersion(int major, int minor, int patch, String qualifier) {
            String version = String.format("%d.%d.%d", major, minor, patch);
            if (qualifier != null) {
                version += "-" + qualifier;
            }
            return version;
        }
    }
}
