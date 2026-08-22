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


package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Shared file/path operations for plugin web flows.
 */
final class PluginWebFileSupport {

    private static final String DEFAULT_UPLOAD_TEMP = "upload_temp";

    private final BrickWebProperties properties;

    PluginWebFileSupport(BrickWebProperties properties) {
        this.properties = properties;
    }

    String requireJarFilename(String originalFilename) {
        String sanitizedFilename = sanitizeFilename(originalFilename);
        if (!sanitizedFilename.toLowerCase().endsWith(".jar")) {
            throw new PluginException("只能上传 JAR 格式的插件文件");
        }
        return sanitizedFilename;
    }

    Path createManagedTempUploadPath(String originalFilename) throws IOException {
        Path uploadTempPath = getManagedUploadTempPath();
        Path requestDir = uploadTempPath.resolve(UUID.randomUUID().toString()).normalize();
        Files.createDirectories(requestDir);
        Path tempFilePath = requestDir.resolve(originalFilename).normalize();
        if (!tempFilePath.startsWith(uploadTempPath)) {
            throw new PluginException("临时文件路径不合法");
        }
        return tempFilePath;
    }

    Path resolveManagedTempPath(String tempFilePath) {
        if (!StringUtils.hasText(tempFilePath)) {
            throw new PluginException("请提供临时文件路径 (tempFilePath)");
        }
        try {
            Path uploadTempPath = getManagedUploadTempPath();
            Path candidate = Paths.get(tempFilePath);
            if (!candidate.isAbsolute()) {
                candidate = uploadTempPath.resolve(candidate);
            }
            candidate = candidate.toAbsolutePath().normalize();
            if (!candidate.startsWith(uploadTempPath)) {
                throw new PluginException("临时文件路径超出允许目录");
            }
            if (!Files.exists(candidate)) {
                throw new PluginException("临时文件不存在: " + tempFilePath);
            }
            return candidate;
        } catch (InvalidPathException e) {
            throw new PluginException("临时文件路径不合法: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new PluginException("解析临时文件路径失败: " + e.getMessage(), e);
        }
    }

    Path getPluginRootPath() throws IOException {
        if (properties.getPluginPaths() == null || properties.getPluginPaths().isEmpty()
                || !StringUtils.hasText(properties.getPluginPaths().get(0))) {
            throw new PluginException("插件根目录未配置");
        }
        Path pluginRootPath = Paths.get(properties.getPluginPaths().get(0)).toAbsolutePath().normalize();
        Files.createDirectories(pluginRootPath);
        return pluginRootPath;
    }

    Path resolveManagedPluginPath(Path pluginPath) throws IOException {
        if (pluginPath == null) {
            throw new PluginException("插件路径不能为空");
        }
        try {
            Path normalized = pluginPath.toAbsolutePath().normalize();
            Path uploadTempPath = getManagedUploadTempPath();
            Path pluginRootPath = getPluginRootPath();
            if (!normalized.startsWith(uploadTempPath) && !normalized.startsWith(pluginRootPath)) {
                throw new PluginException("仅允许访问插件上传目录或插件根目录中的文件");
            }
            if (!Files.exists(normalized)) {
                throw new PluginException("插件文件不存在: " + normalized);
            }
            return normalized;
        } catch (InvalidPathException e) {
            throw new PluginException("插件路径不合法: " + e.getMessage(), e);
        }
    }

    void deleteManagedTempUpload(Path tempPath) throws IOException {
        if (tempPath == null) {
            return;
        }
        Files.deleteIfExists(tempPath);
        Path parent = tempPath.getParent();
        if (parent != null) {
            try {
                Files.deleteIfExists(parent);
            } catch (IOException ignored) {
                // Ignore non-empty temp request directory.
            }
        }
    }

    Path backupPluginFile(Path pluginRootPath, Path pluginFile) throws IOException {
        String oldFilename = pluginFile.getFileName().toString();
        String backupDirName = oldFilename.endsWith(".jar")
                ? oldFilename.substring(0, oldFilename.length() - 4)
                : oldFilename;
        Path projectRoot = pluginRootPath.getParent();
        if (projectRoot == null) {
            projectRoot = Paths.get(".");
        }
        Path backupRootDir = projectRoot.resolve("plugins-backup");
        Path backupDir = backupRootDir.resolve(backupDirName);
        Files.createDirectories(backupDir);

        Path backupPath = backupDir.resolve(pluginFile.getFileName());
        Files.copy(pluginFile, backupPath, StandardCopyOption.REPLACE_EXISTING);
        return backupPath;
    }

    Long fileSize(String pluginPath) {
        if (!StringUtils.hasText(pluginPath)) {
            return null;
        }
        try {
            Path path = Paths.get(pluginPath);
            return Files.exists(path) ? path.toFile().length() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String sanitizeFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new PluginException("上传文件名不能为空");
        }
        String cleaned = StringUtils.cleanPath(originalFilename.trim());
        if (!StringUtils.hasText(cleaned) || cleaned.contains("..")) {
            throw new PluginException("上传文件名不合法");
        }
        try {
            Path fileName = Paths.get(cleaned).getFileName();
            if (fileName == null || !StringUtils.hasText(fileName.toString())) {
                throw new PluginException("上传文件名不合法");
            }
            return fileName.toString();
        } catch (InvalidPathException e) {
            throw new PluginException("上传文件名不合法: " + e.getMessage(), e);
        }
    }

    private Path getManagedUploadTempPath() throws IOException {
        String tempPathStr = properties.getUploadTempPath();
        if (!StringUtils.hasText(tempPathStr)) {
            tempPathStr = DEFAULT_UPLOAD_TEMP;
        }
        Path uploadTempPath = Paths.get(tempPathStr).toAbsolutePath().normalize();
        Files.createDirectories(uploadTempPath);
        return uploadTempPath;
    }
}
