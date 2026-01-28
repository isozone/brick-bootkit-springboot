package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.web.dto.PageResult;
import com.zqzqq.bootkits.web.dto.PluginDTO;
import com.zqzqq.bootkits.web.dto.PluginDetailDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 简化的插件服务（当没有 PluginManager 时使用）
 * 
 * @author brick-bootkit
 */
@Service
@ConditionalOnMissingBean(PluginWebService.class)
public class SimplePluginService {

    /**
     * 获取插件列表
     */
    public PageResult<PluginDTO> listPlugins(int page, int size, String state, String keyword) {
        List<PluginDTO> emptyList = new ArrayList<>();
        return PageResult.of(emptyList, 0, page, size);
    }

    /**
     * 获取所有插件列表
     */
    public List<PluginDTO> getAllPlugins() {
        return new ArrayList<>();
    }

    /**
     * 获取插件详情
     */
    public PluginDetailDTO getPluginDetail(String pluginId) {
        return PluginDetailDTO.builder()
                .pluginId(pluginId)
                .name("示例插件")
                .version("1.0.0")
                .author("示例作者")
                .description("这是一个演示插件")
                .state("STOPPED")
                .build();
    }

    /**
     * 上传插件（不支持）
     */
    public PluginDTO uploadPlugin(MultipartFile file, Boolean enable) {
        throw new UnsupportedOperationException("插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 安装插件（不支持）
     */
    public PluginDTO installPlugin(Path pluginPath) {
        throw new UnsupportedOperationException("插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 启动插件（不支持）
     */
    public void startPlugin(String pluginId) {
        throw new UnsupportedOperationException("插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 停止插件（不支持）
     */
    public void stopPlugin(String pluginId) {
        throw new UnsupportedOperationException("插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 重启插件（不支持）
     */
    public void restartPlugin(String pluginId) {
        throw new UnsupportedOperationException("插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 卸载插件（不支持）
     */
    public void uninstallPlugin(String pluginId) {
        throw new UnsupportedOperationException("插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 获取插件资源（不支持）
     */
    public Object getPluginResource(String pluginId) {
        throw new UnsupportedOperationException("插件功能未启用，请在完整brick-bootkit环境中使用");
    }

    /**
     * 验证插件（不支持）
     */
    public boolean verifyPlugin(Path pluginPath) {
        throw new UnsupportedOperationException("插件功能未启用，请在完整brick-bootkit环境中使用");
    }
}
