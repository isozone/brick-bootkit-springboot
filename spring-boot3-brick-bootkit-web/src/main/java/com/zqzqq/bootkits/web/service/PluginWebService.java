package com.zqzqq.bootkits.web.service;

import cn.hutool.core.date.DateUtil;
import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.core.version.VersionUtils;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.ErrorCode;
import com.zqzqq.bootkits.web.dto.PageResult;
import com.zqzqq.bootkits.web.dto.PluginDTO;
import com.zqzqq.bootkits.web.dto.PluginDetailDTO;
import com.zqzqq.bootkits.web.dto.PluginUploadHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 插件管理服务（完整功能，需要 PluginManager）。
 */
@Slf4j
@Service
public class PluginWebService {

    private final ObjectProvider<PluginManager> pluginManagerProvider;
    private final PluginWebFileSupport fileSupport;

    @Autowired
    UploadHistoryService uploadHistoryService;

    public PluginWebService(ObjectProvider<PluginManager> pluginManagerProvider, BrickWebProperties properties) {
        this.pluginManagerProvider = pluginManagerProvider;
        this.fileSupport = new PluginWebFileSupport(properties);
    }

    private PluginManager getPluginManager() {
        PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
        log.info("PluginManagerProvider 返回: {}", pluginManager != null ? pluginManager.getClass().getName() : "null");
        if (pluginManager == null) {
            throw new PluginException("插件功能未启用，请确保 plugin.enable=true");
        }
        return pluginManager;
    }

    public PageResult<PluginDTO> listPlugins(int page, int size, String state, String keyword) {
        PluginManager pluginManager = getPluginManager();
        log.info("获取插件列表，PluginManager 实例: {}", pluginManager.getClass().getName());
        List<PluginDTO> filtered = pluginManager.getPlugins().stream()
                .map(PluginDTO::from)
                .filter(dto -> matchState(dto, state))
                .filter(dto -> matchKeyword(dto, keyword))
                .collect(Collectors.toList());

        log.info("过滤后有 {} 个插件", filtered.size());
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        if (fromIndex > filtered.size()) {
            fromIndex = 0;
            toIndex = 0;
        }
        return PageResult.of(filtered.subList(fromIndex, toIndex), filtered.size(), page, size);
    }

    public List<PluginDTO> getAllPlugins() {
        return getPluginManager().getPlugins().stream()
                .map(PluginDTO::from)
                .collect(Collectors.toList());
    }

    public PluginDetailDTO getPluginDetail(String pluginId) {
        PluginInfo pluginInfo = getRequiredPlugin(pluginId);

        PluginDetailDTO.PluginDetailDTOBuilder builder = PluginDetailDTO.builder()
                .pluginId(pluginInfo.getPluginId())
                .state(pluginInfo.getPluginState() != null ? pluginInfo.getPluginState().name() : null)
                .stateDescription(pluginInfo.getPluginState() != null ? pluginInfo.getPluginState().getDescription() : null)
                .pluginPath(pluginInfo.getPluginPath())
                .extensionInfo(pluginInfo.getExtensionInfo() != null
                        ? pluginInfo.getExtensionInfo()
                        : java.util.Collections.emptyMap());

        if (pluginInfo.getStartTime() > 0) {
            builder.startTime(DateUtil.format(DateUtil.date(pluginInfo.getStartTime()), "yyyy-MM-dd HH:mm:ss"));
        }
        if (pluginInfo.getStopTime() > 0) {
            builder.stopTime(DateUtil.format(DateUtil.date(pluginInfo.getStopTime()), "yyyy-MM-dd HH:mm:ss"));
        }

        if (pluginInfo.getPluginDescriptor() != null) {
            builder.name(pluginInfo.getPluginDescriptor().getName())
                    .version(pluginInfo.getPluginDescriptor().getPluginVersion())
                    .description(pluginInfo.getPluginDescriptor().getDescription())
                    .mainClass(pluginInfo.getPluginDescriptor().getMainClass())
                    .author(pluginInfo.getPluginDescriptor().getProvider())
                    .pluginType(pluginInfo.getPluginDescriptor().getType() != null
                            ? pluginInfo.getPluginDescriptor().getType().name()
                            : null);

            List<com.zqzqq.bootkits.common.DependencyPlugin> dependencies =
                    pluginInfo.getPluginDescriptor().getDependencyPlugin();
            if (dependencies != null && !dependencies.isEmpty()) {
                builder.dependentPlugins(
                        dependencies.stream()
                                .map(dep -> PluginDetailDTO.DependencyPluginDTO.builder()
                                        .pluginId(dep.getId())
                                        .pluginName(dep.getId())
                                        .version(dep.getVersion())
                                        .build())
                                .collect(Collectors.toList()));
            }
        }

        return builder.build();
    }

    public ApiResult<String> uploadPluginTemp(MultipartFile file) {
        if (file.isEmpty()) {
            throw new PluginException("上传文件不能为空");
        }

        String originalFilename = fileSupport.requireJarFilename(file.getOriginalFilename());
        try {
            Path tempFilePath = fileSupport.createManagedTempUploadPath(originalFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, tempFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("插件已上传到临时目录: {}", tempFilePath);
            return ApiResult.success(tempFilePath.toString());
        } catch (IOException e) {
            log.error("插件上传失败", e);
            throw new PluginException("插件上传失败: " + e.getMessage(), e);
        }
    }

    public ApiResult<PluginDTO> installPluginFromTemp(String tempFilePath, Boolean autoStart) {
        Path tempPath = fileSupport.resolveManagedTempPath(tempFilePath);
        PluginManager pluginManager = getPluginManager();
        PluginInfo uploadPluginInfo = pluginManager.parse(tempPath);
        if (uploadPluginInfo == null) {
            throw new PluginException("插件文件校验失败");
        }

        String originalFilename = tempPath.getFileName().toString();
        String pluginId = uploadPluginInfo.getPluginId();
        String newVersion = uploadPluginInfo.getPluginDescriptor().getPluginVersion();
        log.info("安装插件: {}, 版本: {}", pluginId, newVersion);

        Path backupPath = null;
        try {
            Path pluginRootPath = fileSupport.getPluginRootPath();
            PluginInfo existingPlugin = pluginManager.getPlugin(pluginId);

            if (existingPlugin != null) {
                validateReplacementVersion(existingPlugin, newVersion);
                stopIfStarted(pluginManager, existingPlugin, pluginId);
                backupPath = backupExistingPlugin(pluginRootPath, existingPlugin);
                log.info("卸载旧插件: {}", pluginId);
                pluginManager.uninstall(pluginId);
            }

            String previousFilename = existingPlugin != null
                    ? Paths.get(existingPlugin.getPluginPath()).getFileName().toString()
                    : null;

            PluginInfo pluginInfo = pluginManager.install(tempPath);
            if (pluginInfo == null) {
                throw new PluginException("插件安装失败: " + pluginId);
            }

            fileSupport.deleteManagedTempUpload(tempPath);
            log.info("临时文件已删除: {}", tempPath);
            deletePreviousPluginFileIfRenamed(existingPlugin, previousFilename, originalFilename);

            pluginInfo = autoStartIfNeeded(pluginManager, pluginInfo, autoStart);
            recordSuccessHistory(pluginInfo, autoStart, backupPath);
            return ApiResult.success(PluginDTO.from(pluginInfo));
        } catch (IOException e) {
            handleInstallFailure(pluginId, originalFilename, tempPath, autoStart, backupPath, e);
            throw new PluginException("插件安装失败: " + e.getMessage(), e);
        } catch (PluginException e) {
            recordFailedHistory(pluginId, originalFilename, e.getMessage(), tempPath, autoStart);
            throw e;
        } catch (Exception e) {
            log.error("插件安装异常", e);
            recordFailedHistory(pluginId, originalFilename, e.getMessage(), tempPath, autoStart);
            throw new PluginException("插件安装异常: " + e.getMessage(), e);
        }
    }

    public ApiResult<PluginDTO> uploadPlugin(MultipartFile file, Boolean enableAfterUpload) {
        if (file.isEmpty()) {
            throw new PluginException("上传文件不能为空");
        }

        String originalFilename = fileSupport.requireJarFilename(file.getOriginalFilename());
        Path backupPath = null;
        Path tempPath = null;
        String pluginId = null;

        try {
            tempPath = fileSupport.createManagedTempUploadPath(originalFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("插件已上传到临时目录: {}", tempPath);

            PluginManager pluginManager = getPluginManager();
            PluginInfo uploadPluginInfo = pluginManager.parse(tempPath);
            if (uploadPluginInfo == null) {
                throw new PluginException("插件文件校验失败");
            }
            pluginId = uploadPluginInfo.getPluginId();
            String newVersion = uploadPluginInfo.getPluginDescriptor().getPluginVersion();
            log.info("插件ID: {}, 版本: {}", pluginId, newVersion);

            Path pluginRootPath = fileSupport.getPluginRootPath();
            PluginInfo existingPlugin = pluginManager.getPlugin(pluginId);
            if (existingPlugin != null) {
                backupPath = backupExistingPlugin(pluginRootPath, existingPlugin);
            }

            String previousFilename = existingPlugin != null
                    ? Paths.get(existingPlugin.getPluginPath()).getFileName().toString()
                    : null;

            log.info("开始安装插件: {}", pluginId);
            PluginInfo pluginInfo = pluginManager.install(tempPath);
            if (pluginInfo == null) {
                throw new PluginException("插件安装失败: " + pluginId);
            }

            fileSupport.deleteManagedTempUpload(tempPath);
            log.info("临时文件已删除: {}", tempPath);
            deletePreviousPluginFileIfRenamed(existingPlugin, previousFilename, originalFilename);

            pluginInfo = autoStartIfNeeded(pluginManager, pluginInfo, enableAfterUpload);
            recordSuccessHistory(pluginInfo, enableAfterUpload, backupPath);
            return ApiResult.success(PluginDTO.from(pluginInfo));
        } catch (IOException e) {
            log.error("插件上传失败", e);
            cleanupTempUploadQuietly(tempPath);
            restoreBackupQuietly(backupPath, pluginId, enableAfterUpload);
            recordFailedHistory(pluginId, originalFilename, e.getMessage(), tempPath, enableAfterUpload);
            throw new PluginException("插件上传失败: " + e.getMessage(), e);
        } catch (PluginException e) {
            recordFailedHistory(pluginId, originalFilename, e.getMessage(), tempPath, enableAfterUpload);
            throw e;
        } catch (Exception e) {
            log.error("插件上传异常", e);
            recordFailedHistory(pluginId, originalFilename, e.getMessage(), tempPath, enableAfterUpload);
            throw new PluginException("插件上传异常: " + e.getMessage(), e);
        }
    }

    public PluginDTO installPlugin(Path pluginPath) {
        try {
            PluginInfo pluginInfo = getPluginManager().install(fileSupport.resolveManagedPluginPath(pluginPath));
            return PluginDTO.from(pluginInfo);
        } catch (IOException e) {
            throw new PluginException("插件安装失败: " + e.getMessage(), e);
        } catch (PluginException e) {
            throw new PluginException("插件安装失败: " + e.getMessage());
        }
    }

    public void startPlugin(String pluginId) {
        try {
            getPluginManager().start(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件启动失败: " + e.getMessage());
        }
    }

    public void stopPlugin(String pluginId) {
        try {
            getPluginManager().stop(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件停止失败: " + e.getMessage());
        }
    }

    public void restartPlugin(String pluginId) {
        try {
            getPluginManager().reload(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件重启失败: " + e.getMessage());
        }
    }

    public void uninstallPlugin(String pluginId) {
        try {
            getPluginManager().uninstall(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件卸载失败: " + e.getMessage());
        }
    }

    public Resource getPluginResource(String pluginId) {
        PluginInfo pluginInfo = getRequiredPlugin(pluginId);
        try {
            Path pluginPath = Paths.get(pluginInfo.getPluginPath());
            Resource resource = new UrlResource(pluginPath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new PluginException(ErrorCode.FILE_NOT_FOUND.getMessage());
        } catch (MalformedURLException e) {
            throw new PluginException("插件文件路径无效: " + e.getMessage(), e);
        }
    }

    public boolean verifyPlugin(Path pluginPath) {
        try {
            return getPluginManager().verify(fileSupport.resolveManagedPluginPath(pluginPath));
        } catch (Exception e) {
            log.error("Failed to verify plugin: {}", pluginPath, e);
            return false;
        }
    }

    private boolean matchState(PluginDTO dto, String state) {
        if (!StringUtils.hasText(state) || state.equalsIgnoreCase("all")) {
            return true;
        }
        return dto.getState() != null && dto.getState().equalsIgnoreCase(state);
    }

    private boolean matchKeyword(PluginDTO dto, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String lowerKeyword = keyword.toLowerCase();
        return dto.getPluginId().toLowerCase().contains(lowerKeyword)
                || (dto.getName() != null && dto.getName().toLowerCase().contains(lowerKeyword));
    }

    private PluginInfo getRequiredPlugin(String pluginId) {
        PluginInfo pluginInfo = getPluginManager().getPlugin(pluginId);
        if (pluginInfo == null) {
            throw new PluginException(ErrorCode.PLUGIN_NOT_FOUND.getMessage());
        }
        return pluginInfo;
    }

    private void validateReplacementVersion(PluginInfo existingPlugin, String newVersion) {
        String oldVersion = existingPlugin.getPluginDescriptor().getPluginVersion();
        log.info("发现已存在的同名插件: {}, 当前版本: {}, 新版本: {}",
                existingPlugin.getPluginId(), oldVersion, newVersion);
        if (!isVersionGreaterThan(newVersion, oldVersion)) {
            throw new PluginException(String.format(
                    "安装失败：新版本号 %s 必须大于旧版本号 %s", newVersion, oldVersion));
        }
    }

    private void stopIfStarted(PluginManager pluginManager, PluginInfo pluginInfo, String pluginId) {
        EnhancedPluginState state = (EnhancedPluginState) pluginInfo.getPluginState();
        if (state == EnhancedPluginState.STARTED) {
            log.info("停止旧插件: {}", pluginId);
            pluginManager.stop(pluginId);
        }
    }

    private Path backupExistingPlugin(Path pluginRootPath, PluginInfo existingPlugin) throws IOException {
        if (existingPlugin == null) {
            return null;
        }
        Path oldPluginFile = Paths.get(existingPlugin.getPluginPath());
        if (!Files.exists(oldPluginFile)) {
            return null;
        }
        Path backupPath = fileSupport.backupPluginFile(pluginRootPath, oldPluginFile);
        log.info("旧插件已备份到: {}", backupPath);
        return backupPath;
    }

    private PluginInfo autoStartIfNeeded(PluginManager pluginManager, PluginInfo pluginInfo, Boolean autoStart) {
        if (!Boolean.TRUE.equals(autoStart)) {
            return pluginInfo;
        }
        String pluginId = pluginInfo.getPluginId();
        log.info("自动启动插件: {}", pluginId);
        pluginManager.start(pluginId);
        PluginInfo startedPlugin = pluginManager.getPlugin(pluginId);
        log.info("插件启动成功: {}", pluginId);
        return startedPlugin;
    }

    private void recordSuccessHistory(PluginInfo pluginInfo, Boolean autoStart, Path backupPath) {
        PluginUploadHistory history = PluginUploadHistory.builder()
                .uploadId(UploadHistoryService.generateUploadId())
                .pluginId(pluginInfo.getPluginId())
                .pluginName(pluginInfo.getPluginDescriptor() != null
                        ? pluginInfo.getPluginDescriptor().getName()
                        : pluginInfo.getPluginId())
                .version(pluginInfo.getPluginDescriptor() != null
                        ? pluginInfo.getPluginDescriptor().getPluginVersion()
                        : null)
                .uploadTime(LocalDateTime.now())
                .status(PluginUploadHistory.UploadStatus.SUCCESS)
                .filePath(pluginInfo.getPluginPath())
                .fileSize(fileSupport.fileSize(pluginInfo.getPluginPath()))
                .autoStart(autoStart)
                .backupPath(backupPath != null ? backupPath.toString() : null)
                .errorMessage(null)
                .build();
        uploadHistoryService.recordUpload(history);
    }

    private void recordFailedHistory(String pluginId, String filename, String errorMessage,
                                     Path tempPath, Boolean autoStart) {
        try {
            PluginUploadHistory history = PluginUploadHistory.builder()
                    .uploadId(UploadHistoryService.generateUploadId())
                    .pluginId(pluginId != null ? pluginId : filename)
                    .pluginName(pluginId)
                    .version(null)
                    .uploadTime(LocalDateTime.now())
                    .status(PluginUploadHistory.UploadStatus.FAILED)
                    .filePath(tempPath != null ? tempPath.toString() : filename)
                    .fileSize(tempPath != null && Files.exists(tempPath) ? tempPath.toFile().length() : null)
                    .autoStart(autoStart)
                    .backupPath(null)
                    .errorMessage(errorMessage)
                    .build();
            uploadHistoryService.recordUpload(history);
        } catch (Exception e) {
            log.error("记录失败历史时发生错误", e);
        }
    }

    private void deletePreviousPluginFileIfRenamed(PluginInfo existingPlugin,
                                                   String previousFilename,
                                                   String currentFilename) throws IOException {
        if (existingPlugin == null || previousFilename == null || previousFilename.equals(currentFilename)) {
            return;
        }
        Path oldPluginFile = Paths.get(existingPlugin.getPluginPath());
        if (Files.exists(oldPluginFile)) {
            Files.deleteIfExists(oldPluginFile);
            log.info("旧版本文件已删除: {}", oldPluginFile);
        }
    }

    private void handleInstallFailure(String pluginId,
                                      String originalFilename,
                                      Path tempPath,
                                      Boolean autoStart,
                                      Path backupPath,
                                      IOException exception) {
        log.error("插件安装失败", exception);
        restoreBackupQuietly(backupPath, pluginId, autoStart);
        recordFailedHistory(pluginId, originalFilename, exception.getMessage(), tempPath, autoStart);
    }

    private void restoreBackupQuietly(Path backupPath, String pluginId, Boolean autoStart) {
        if (backupPath == null || !Files.exists(backupPath)) {
            return;
        }
        try {
            restoreOldPlugin(backupPath, pluginId, autoStart);
        } catch (Exception restoreEx) {
            log.error("恢复旧版本失败", restoreEx);
        }
    }

    private void cleanupTempUploadQuietly(Path tempPath) {
        if (tempPath == null || !Files.exists(tempPath)) {
            return;
        }
        try {
            fileSupport.deleteManagedTempUpload(tempPath);
        } catch (Exception ignored) {
            // ignore cleanup failure
        }
    }

    private boolean isVersionGreaterThan(String newVersion, String oldVersion) {
        if (!StringUtils.hasText(newVersion) || !StringUtils.hasText(oldVersion)) {
            return false;
        }
        return VersionUtils.compareVersions(newVersion, oldVersion) > 0;
    }

    private void restoreOldPlugin(Path backupPath, String pluginId, Boolean enableAfterUpload) {
        log.info("尝试恢复旧版本插件: {}", pluginId);
        PluginManager pluginManager = getPluginManager();
        try {
            PluginInfo currentPlugin = pluginManager.getPlugin(pluginId);
            if (currentPlugin != null) {
                EnhancedPluginState state = (EnhancedPluginState) currentPlugin.getPluginState();
                if (state == EnhancedPluginState.STARTED) {
                    pluginManager.stop(pluginId);
                }
                pluginManager.uninstall(pluginId);
            }

            PluginInfo restoredPlugin = pluginManager.install(backupPath);
            log.info("旧版本已恢复: {}", pluginId);

            if (Boolean.TRUE.equals(enableAfterUpload)) {
                pluginManager.start(pluginId);
                log.info("旧版本已启动: {}", pluginId);
            }

            log.info("旧版本插件已成功恢复: {}", restoredPlugin.getPluginId());
        } catch (Exception e) {
            log.error("恢复旧版本插件失败: {}", pluginId, e);
            throw new RuntimeException("恢复旧版本失败: " + e.getMessage(), e);
        }
    }
}
