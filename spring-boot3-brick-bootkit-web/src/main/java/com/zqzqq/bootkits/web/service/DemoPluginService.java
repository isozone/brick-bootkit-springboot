package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.PageResult;
import com.zqzqq.bootkits.web.dto.PluginDTO;
import com.zqzqq.bootkits.web.dto.PluginDetailDTO;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;

/**
 * Demo 环境的插件服务接口
 * 在完整环境中不需要此服务
 * 
 * @author brick-bootkit
 */
public interface DemoPluginService {

    /**
     * 获取插件列表
     */
    PageResult<PluginDTO> listPlugins(int page, int size, String state, String keyword);

    /**
     * 获取所有插件
     */
    List<PluginDTO> getAllPlugins();

    /**
     * 获取插件详情
     */
    PluginDetailDTO getDetail(String pluginId);

    /**
     * 上传插件
     */
    ApiResult<PluginDTO> uploadPlugin(MultipartFile file, Boolean enableAfterUpload);

    /**
     * 安装插件
     */
    PluginDTO installPlugin(Path pluginPath);

    /**
     * 启动插件
     */
    void startPlugin(String pluginId);

    /**
     * 停止插件
     */
    void stopPlugin(String pluginId);

    /**
     * 重启插件
     */
    void restartPlugin(String pluginId);

    /**
     * 卸载插件
     */
    void uninstallPlugin(String pluginId);

    /**
     * 验证插件
     */
    boolean verifyPlugin(Path pluginPath);
}
