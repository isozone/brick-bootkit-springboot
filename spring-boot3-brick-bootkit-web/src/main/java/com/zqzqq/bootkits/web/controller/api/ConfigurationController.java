package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.core.config.PluginConfiguration;
import com.zqzqq.bootkits.core.config.PluginConfigurationStatistics;
import com.zqzqq.bootkits.core.config.PluginConfigurationVersion;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.ConfigurationWebService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 插件配置热更新 API 控制器。
 * 提供插件配置查看、热更新、版本管理与回滚接口。
 *
 * @author brick-bootkit
 */
@RestController
@RequestMapping("${plugin.web.api-prefix:/plugins-web/api}/configurations")
@Tag(name = "插件配置", description = "插件配置热更新与版本管理接口")
public class ConfigurationController {

    private final ObjectProvider<ConfigurationWebService> configurationWebServiceProvider;
    private final PluginWebAuthorizationService authorizationService;

    public ConfigurationController(ObjectProvider<ConfigurationWebService> configurationWebServiceProvider,
                                   PluginWebAuthorizationService authorizationService) {
        this.configurationWebServiceProvider = configurationWebServiceProvider;
        this.authorizationService = authorizationService;
    }

    private ConfigurationWebService getService() {
        ConfigurationWebService service = configurationWebServiceProvider.getIfAvailable();
        if (service == null) {
            throw new IllegalStateException("插件配置管理服务未启用");
        }
        return service;
    }

    /**
     * 获取配置统计
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取配置统计")
    public ApiResult<PluginConfigurationStatistics> statistics() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getStatistics());
    }

    /**
     * 获取所有插件配置
     */
    @GetMapping
    @Operation(summary = "获取所有插件配置")
    public ApiResult<Map<String, PluginConfiguration>> allConfigurations() {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, null);
        return ApiResult.success(getService().getAllConfigurations());
    }

    /**
     * 获取指定插件配置
     */
    @GetMapping("/{pluginId}")
    @Operation(summary = "获取指定插件配置")
    public ApiResult<PluginConfiguration> getConfiguration(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getConfiguration(pluginId));
    }

    /**
     * 热更新插件配置
     */
    @PutMapping("/{pluginId}")
    @Operation(summary = "热更新插件配置")
    public ApiResult<PluginConfiguration> updateConfiguration(@PathVariable String pluginId,
                                                              @RequestBody UpdateRequest request) {
        authorizationService.check(PluginWebPermission.PLUGIN_INSTALL, pluginId);
        PluginConfiguration configuration = request.getConfiguration();
        if (configuration == null) {
            configuration = new PluginConfiguration();
        }
        return ApiResult.success(getService().updateConfiguration(
                pluginId, configuration, request.getVersionDescription()));
    }

    /**
     * 获取配置版本历史
     */
    @GetMapping("/{pluginId}/versions")
    @Operation(summary = "获取配置版本历史")
    public ApiResult<List<PluginConfigurationVersion>> versions(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_VIEW, pluginId);
        return ApiResult.success(getService().getConfigurationVersions(pluginId));
    }

    /**
     * 回滚配置到指定版本
     */
    @PostMapping("/{pluginId}/rollback")
    @Operation(summary = "回滚配置到指定版本")
    public ApiResult<PluginConfiguration> rollback(@PathVariable String pluginId,
                                                   @RequestBody RollbackRequest request) {
        authorizationService.check(PluginWebPermission.PLUGIN_INSTALL, pluginId);
        return ApiResult.success(getService().rollbackToVersion(pluginId, request.getVersionId()));
    }

    /**
     * 删除插件配置
     */
    @DeleteMapping("/{pluginId}")
    @Operation(summary = "删除插件配置")
    public ApiResult<Void> remove(@PathVariable String pluginId) {
        authorizationService.check(PluginWebPermission.PLUGIN_INSTALL, pluginId);
        getService().removeConfiguration(pluginId);
        return ApiResult.success();
    }

    @Data
    public static class UpdateRequest {
        private PluginConfiguration configuration;
        private String versionDescription;
    }

    @Data
    public static class RollbackRequest {
        private String versionId;
    }
}
