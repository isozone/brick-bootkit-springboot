package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.ErrorCode;
import com.zqzqq.bootkits.web.dto.PageResult;
import com.zqzqq.bootkits.web.dto.PluginDTO;
import com.zqzqq.bootkits.web.dto.PluginDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 插件管理服务（完整功能，需要 PluginManager）
 * 
 * @author brick-bootkit
 */
@Slf4j
@Service
@ConditionalOnBean(PluginManager.class)
public class PluginWebService {

    private final PluginManager pluginManager;
    private final BrickWebProperties properties;

    public PluginWebService(PluginManager pluginManager, BrickWebProperties properties) {
        this.pluginManager = pluginManager;
        this.properties = properties;
    }

    /**
     * 获取插件列表
     */
    public PageResult<PluginDTO> listPlugins(int page, int size, String state, String keyword) {
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
        return pluginManager.getPlugins().stream()
                .map(PluginDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 获取插件详情
     */
    public PluginDetailDTO getPluginDetail(String pluginId) {
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
     */
    public ApiResult<PluginDTO> uploadPlugin(MultipartFile file, Boolean enableAfterUpload) {
        if (file.isEmpty()) {
            throw new PluginException("上传文件不能为空");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.endsWith(".jar")) {
            throw new PluginException("只能上传 JAR 格式的插件文件");
        }
        
        try {
            // 保存到临时目录
            Path uploadPath = Paths.get(properties.getUploadTempPath());
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path targetPath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            log.info("Plugin uploaded to: {}", targetPath);
            
            // 如果需要，立即安装
            if (enableAfterUpload != null && enableAfterUpload) {
                PluginInfo pluginInfo = pluginManager.install(targetPath);
                return ApiResult.success(PluginDTO.from(pluginInfo));
            }
            
            return ApiResult.success();
            
        } catch (IOException e) {
            log.error("Failed to upload plugin", e);
            throw new PluginException("插件上传失败: " + e.getMessage());
        }
    }

    /**
     * 安装插件
     */
    public PluginDTO installPlugin(Path pluginPath) {
        try {
            PluginInfo pluginInfo = pluginManager.install(pluginPath);
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
            pluginManager.start(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件启动失败: " + e.getMessage());
        }
    }

    /**
     * 停止插件
     */
    public void stopPlugin(String pluginId) {
        try {
            pluginManager.stop(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件停止失败: " + e.getMessage());
        }
    }

    /**
     * 重启插件
     */
    public void restartPlugin(String pluginId) {
        try {
            pluginManager.reload(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件重启失败: " + e.getMessage());
        }
    }

    /**
     * 卸载插件
     */
    public void uninstallPlugin(String pluginId) {
        try {
            pluginManager.uninstall(pluginId);
        } catch (PluginException e) {
            throw new PluginException("插件卸载失败: " + e.getMessage());
        }
    }

    /**
     * 获取插件文件资源
     */
    public Resource getPluginResource(String pluginId) {
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
            return pluginManager.verify(pluginPath);
        } catch (Exception e) {
            log.error("Failed to verify plugin: {}", pluginPath, e);
            return false;
        }
    }
}
