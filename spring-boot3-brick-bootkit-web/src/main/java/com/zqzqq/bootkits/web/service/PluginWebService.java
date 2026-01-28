package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.ErrorCode;
import com.zqzqq.bootkits.web.dto.PageResult;
import com.zqzqq.bootkits.web.dto.PluginDTO;
import com.zqzqq.bootkits.web.dto.PluginDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 插件管理服务（完整功能，需要 PluginManager）
 * 注意：PluginManager 是延迟初始化的（在 ApplicationStartedEvent 时），
 * 因此使用 ObjectProvider 在运行时动态获取，而不是使用 @ConditionalOnBean
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
public class PluginWebService {

    private final ObjectProvider<PluginManager> pluginManagerProvider;
    private final BrickWebProperties properties;

    public PluginWebService(ObjectProvider<PluginManager> pluginManagerProvider, BrickWebProperties properties) {
        this.pluginManagerProvider = pluginManagerProvider;
        this.properties = properties;
    }

    /**
     * 获取 PluginManager 实例
     */
    private PluginManager getPluginManager() {
        PluginManager pluginManager = pluginManagerProvider.getIfAvailable();
        if (pluginManager == null) {
            throw new PluginException("插件功能未启用，请确保 plugin.enable=true");
        }
        return pluginManager;
    }

    /**
     * 获取插件列表
     */
    public PageResult<PluginDTO> listPlugins(int page, int size, String state, String keyword) {
        PluginManager pluginManager = getPluginManager();
        List<PluginInfo> plugins = pluginManager.getPlugins();
        
        // 过滤
        List<PluginDTO> filtered = plugins.stream()
                .map(PluginDTO::from)
                .filter(dto -> {
                    // 状态过滤
                    if (StringUtils.hasText(state) && !state.equalsIgnoreCase("all")) {
                        if (dto.getState() == null || !dto.getState().equalsIgnoreCase(state)) {
                            return false;
                        }
                    }
                    // 关键词搜索
                    if (StringUtils.hasText(keyword)) {
                        String lowerKeyword = keyword.toLowerCase();
                        boolean match = dto.getPluginId().toLowerCase().contains(lowerKeyword) ||
                                       (dto.getName() != null && dto.getName().toLowerCase().contains(lowerKeyword));
                        return match;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        
        // 分页
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());
        
        if (fromIndex > filtered.size()) {
            fromIndex = 0;
            toIndex = 0;
        }
        
        List<PluginDTO> pageRecords = filtered.subList(fromIndex, toIndex);
        return PageResult.of(pageRecords, filtered.size(), page, size);
    }

    /**
     * 获取所有插件列表（不分页）
     */
    public List<PluginDTO> getAllPlugins() {
        return getPluginManager().getPlugins().stream()
                .map(PluginDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 获取插件详情
     */
    public PluginDetailDTO getPluginDetail(String pluginId) {
        PluginManager pluginManager = getPluginManager();
        PluginInfo pluginInfo = pluginManager.getPlugin(pluginId);
        if (pluginInfo == null) {
            throw new PluginException(ErrorCode.PLUGIN_NOT_FOUND.getMessage());
        }
        
        PluginDetailDTO.PluginDetailDTOBuilder builder = PluginDetailDTO.builder()
                .pluginId(pluginInfo.getPluginId())
                .state(pluginInfo.getPluginState() != null ? pluginInfo.getPluginState().name() : null)
                .stateDescription(pluginInfo.getPluginState() != null ? pluginInfo.getPluginState().getDescription() : null)
                .pluginPath(pluginInfo.getPluginPath())
                .extensionInfo(pluginInfo.getExtensionInfo());
        
        if (pluginInfo.getPluginDescriptor() != null) {
            builder.name(pluginInfo.getPluginDescriptor().getName())
                   .version(pluginInfo.getPluginDescriptor().getPluginVersion())
                   .description(pluginInfo.getPluginDescriptor().getDescription())
                   .mainClass(pluginInfo.getPluginDescriptor().getMainClass());
        }
        
        return builder.build();
    }

    /**
     * 上传插件
     * 插件ID基于文件名生成（去掉.jar扩展名），与上传文件名保持一致
     * 版本号必须大于旧版本，否则报错
     */
    public ApiResult<PluginDTO> uploadPlugin(MultipartFile file, Boolean enableAfterUpload) {
        if (file.isEmpty()) {
            throw new PluginException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.endsWith(".jar")) {
            throw new PluginException("只能上传 JAR 格式的插件文件");
        }

        Path backupPath = null;
        Path targetPath = null;
        String pluginId = null;

        try {
            // 基于原始文件名生成pluginId（去掉.jar扩展名），与上传文件名保持一致
            pluginId = originalFilename;
            if (pluginId.endsWith(".jar")) {
                pluginId = pluginId.substring(0, pluginId.length() - 4);
            }
            log.info("插件ID基于文件名生成: {}", pluginId);

            // 使用插件目录作为临时目录，确保文件名不被修改
            Path pluginRootPath = Paths.get(properties.getPluginPaths().get(0));
            if (!Files.exists(pluginRootPath)) {
                Files.createDirectories(pluginRootPath);
            }

            // 直接使用原始文件名保存到插件目录
            targetPath = pluginRootPath.resolve(originalFilename);
            
            // 从 MultipartFile 直接复制输入流到目标文件
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Plugin saved to: {}", targetPath);

            // 解析插件信息获取描述符信息
            PluginManager pluginManager = getPluginManager();
            PluginInfo uploadPluginInfo = pluginManager.parse(targetPath);
            if (uploadPluginInfo == null) {
                throw new PluginException("插件文件校验失败");
            }

            String newVersion = uploadPluginInfo.getPluginDescriptor().getPluginVersion();
            log.info("上传插件版本: {}", newVersion);

            // 检查是否已存在同名插件
            PluginInfo existingPlugin = pluginManager.getPlugin(pluginId);
            if (existingPlugin != null) {
                String oldVersion = existingPlugin.getPluginDescriptor().getPluginVersion();
                log.info("发现已存在的同名插件: {}, 当前版本: {}, 新版本: {}", pluginId, oldVersion, newVersion);

                // 比较版本号：新版本必须大于旧版本
                if (!isVersionGreaterThan(newVersion, oldVersion)) {
                    throw new PluginException(String.format(
                            "上传失败：新版本号 %s 必须大于旧版本号 %s", newVersion, oldVersion));
                }

                // 如果旧插件正在运行，先停止
                EnhancedPluginState state = (EnhancedPluginState) existingPlugin.getPluginState();
                if (state == EnhancedPluginState.STARTED) {
                    log.info("停止旧插件: {}", pluginId);
                    pluginManager.stop(pluginId);
                }

                // 备份旧插件
                Path oldPluginFile = Paths.get(existingPlugin.getPluginPath());
                if (Files.exists(oldPluginFile)) {
                    // 创建备份目录
                    String backupDirName = "upload_backup_" + System.currentTimeMillis();
                    Path backupDir = pluginRootPath.resolve(backupDirName).resolve(pluginId);
                    Files.createDirectories(backupDir);

                    backupPath = backupDir.resolve(oldPluginFile.getFileName());
                    Files.copy(oldPluginFile, backupPath, StandardCopyOption.REPLACE_EXISTING);
                    log.info("旧插件已备份到: {}", backupPath);
                }

                // 卸载旧插件
                log.info("卸载旧插件: {}", pluginId);
                pluginManager.uninstall(pluginId);
            }

            // 安装新插件（此时文件已在正确位置，直接返回信息）
            PluginInfo pluginInfo = pluginManager.getPlugin(pluginId);
            if (pluginInfo == null) {
                // 如果还没有注册，手动解析并添加到解析列表
                pluginInfo = pluginManager.parse(targetPath);
            }
            log.info("插件安装成功: {}", pluginId);

            // 如果需要自动启动
            if (enableAfterUpload != null && enableAfterUpload) {
                log.info("自动启动插件: {}", pluginId);
                pluginManager.start(pluginId);
                pluginInfo = pluginManager.getPlugin(pluginId);
                log.info("新插件启动成功: {}", pluginId);

                // 启动成功后，删除旧版本备份
                if (backupPath != null && Files.exists(backupPath)) {
                    Files.deleteIfExists(backupPath);
                    log.info("旧版本备份已删除: {}", backupPath);
                }
            }

            return ApiResult.success(PluginDTO.from(pluginInfo));

        } catch (IOException e) {
            log.error("插件上传失败", e);
            // 如果上传失败，尝试恢复旧版本
            if (backupPath != null && Files.exists(backupPath)) {
                try {
                    restoreOldPlugin(backupPath, pluginId, enableAfterUpload);
                } catch (Exception restoreEx) {
                    log.error("恢复旧版本失败", restoreEx);
                }
            }
            throw new PluginException("插件上传失败: " + e.getMessage());
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            log.error("插件上传异常", e);
            // 如果发生异常，尝试恢复旧版本
            if (backupPath != null && Files.exists(backupPath)) {
                try {
                    restoreOldPlugin(backupPath, pluginId, enableAfterUpload);
                } catch (Exception restoreEx) {
                    log.error("恢复旧版本失败", restoreEx);
                }
            }
            throw new PluginException("插件上传异常: " + e.getMessage());
        } finally {
            // 不再删除临时文件，因为文件已保存到插件目录
        }
    }

    /**
     * 恢复旧版本插件
     */
    private void restoreOldPlugin(Path backupPath, String pluginId, Boolean enableAfterUpload) {
        log.info("尝试恢复旧版本插件: {}", pluginId);
        PluginManager pluginManager = getPluginManager();
        try {
            // 卸载当前插件（如果有）
            PluginInfo currentPlugin = pluginManager.getPlugin(pluginId);
            if (currentPlugin != null) {
                EnhancedPluginState state = (EnhancedPluginState) currentPlugin.getPluginState();
                if (state == EnhancedPluginState.STARTED) {
                    pluginManager.stop(pluginId);
                }
                pluginManager.uninstall(pluginId);
            }

            // 恢复旧版本
            PluginInfo restoredPlugin = pluginManager.install(backupPath);
            log.info("旧版本已恢复: {}", pluginId);

            // 如果需要自动启动，恢复后启动
            if (enableAfterUpload != null && enableAfterUpload) {
                pluginManager.start(pluginId);
                log.info("旧版本已启动: {}", pluginId);
            }

            log.info("旧版本插件已成功恢复: {}", pluginId);
        } catch (Exception e) {
            log.error("恢复旧版本插件失败: {}", pluginId, e);
            throw new RuntimeException("恢复旧版本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 比较版本号：判断新版本是否大于旧版本
     * 支持语义化版本号 (x.y.z) 和简单的数字版本号
     */
    private boolean isVersionGreaterThan(String newVersion, String oldVersion) {
        if (newVersion == null || oldVersion == null) {
            return false;
        }

        String[] newParts = newVersion.split("[.\\-_]");
        String[] oldParts = oldVersion.split("[.\\-_]");

        int maxLength = Math.max(newParts.length, oldParts.length);

        for (int i = 0; i < maxLength; i++) {
            int newPart = i < newParts.length ? parseVersionPart(newParts[i]) : 0;
            int oldPart = i < oldParts.length ? parseVersionPart(oldParts[i]) : 0;

            if (newPart > oldPart) {
                return true;
            } else if (newPart < oldPart) {
                return false;
            }
        }

        // 版本号完全相同
        return false;
    }

    /**
     * 解析版本号的单个部分为整数
     */
    private int parseVersionPart(String part) {
        if (part == null || part.isEmpty()) {
            return 0;
        }
        try {
            // 移除可能的非数字字符前缀（如 v、release- 等）
            String numericPart = part.replaceAll("^[^0-9]*", "");
            if (numericPart.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 安装插件
     */
    public PluginDTO installPlugin(Path pluginPath) {
        try {
            PluginInfo pluginInfo = getPluginManager().install(pluginPath);
            return PluginDTO.from(pluginInfo);
        } catch (PluginException e) {
            throw new PluginException("插件安装失败: " + e.getMessage());
        }
    }

    /**
     * 启动插件
     */
    public void startPlugin(String pluginId) {
        try {
            getPluginManager().start(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止插件
     */
    public void stopPlugin(String pluginId) {
        try {
            getPluginManager().stop(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件停止失败: " + e.getMessage());
        }
    }

    /**
     * 重启插件
     */
    public void restartPlugin(String pluginId) {
        try {
            getPluginManager().reload(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件重启失败: " + e.getMessage());
        }
    }

    /**
     * 卸载插件
     */
    public void uninstallPlugin(String pluginId) {
        try {
            getPluginManager().uninstall(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件卸载失败: " + e.getMessage());
        }
    }

    /**
     * 获取插件文件资源
     */
    public Resource getPluginResource(String pluginId) {
        PluginManager pluginManager = getPluginManager();
        PluginInfo pluginInfo = pluginManager.getPlugin(pluginId);
        if (pluginInfo == null) {
            throw new PluginException(ErrorCode.PLUGIN_NOT_FOUND.getMessage());
        }
        
        try {
            Path pluginPath = Paths.get(pluginInfo.getPluginPath());
            Resource resource = new UrlResource(pluginPath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new PluginException(ErrorCode.FILE_NOT_FOUND.getMessage());
            }
        } catch (MalformedURLException e) {
            throw new PluginException("插件文件路径无效: " + e.getMessage());
        }
    }

    /**
     * 验证插件
     */
    public boolean verifyPlugin(Path pluginPath) {
        try {
            return getPluginManager().verify(pluginPath);
        } catch (Exception e) {
            log.error("Failed to verify plugin: {}", pluginPath, e);
            return false;
        }
    }
}
