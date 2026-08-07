package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.config.PluginConfiguration;
import com.zqzqq.bootkits.core.config.PluginConfigurationManager;
import com.zqzqq.bootkits.core.config.PluginConfigurationStatistics;
import com.zqzqq.bootkits.core.config.PluginConfigurationVersion;
import com.zqzqq.bootkits.core.exception.PluginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 插件配置热更新 Web 服务。
 * 基于主框架注册的 {@link PluginConfigurationManager} Bean，
 * 提供插件配置查看、热更新、版本回滚与统计能力。
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class ConfigurationWebService {

    private final ObjectProvider<PluginConfigurationManager> configurationManagerProvider;

    public ConfigurationWebService(ObjectProvider<PluginConfigurationManager> configurationManagerProvider) {
        this.configurationManagerProvider = configurationManagerProvider;
    }

    private PluginConfigurationManager getManager() {
        PluginConfigurationManager manager = configurationManagerProvider.getIfAvailable();
        if (manager == null) {
            throw new PluginException("插件配置管理未启用（plugin.configuration.enabled=false）");
        }
        return manager;
    }

    /**
     * 获取配置统计
     */
    public PluginConfigurationStatistics getStatistics() {
        return getManager().getStatistics();
    }

    /**
     * 获取所有插件配置
     */
    public Map<String, PluginConfiguration> getAllConfigurations() {
        return getManager().getAllConfigurations();
    }

    /**
     * 获取指定插件的配置
     */
    public PluginConfiguration getConfiguration(String pluginId) {
        if (!getManager().hasConfiguration(pluginId)) {
            throw new PluginException("插件配置不存在: " + pluginId);
        }
        return getManager().getConfiguration(pluginId);
    }

    /**
     * 热更新插件配置
     */
    public PluginConfiguration updateConfiguration(String pluginId, PluginConfiguration configuration,
                                                   String versionDescription) {
        if (configuration == null) {
            throw new PluginException("配置内容不能为空");
        }
        configuration.setPluginId(pluginId);
        getManager().updateConfiguration(pluginId, configuration, versionDescription);
        log.info("插件配置已热更新: {}", pluginId);
        return getManager().getConfiguration(pluginId);
    }

    /**
     * 获取配置版本历史
     */
    public List<PluginConfigurationVersion> getConfigurationVersions(String pluginId) {
        return getManager().getConfigurationVersions(pluginId);
    }

    /**
     * 回滚配置到指定版本
     */
    public PluginConfiguration rollbackToVersion(String pluginId, String versionId) {
        getManager().rollbackToVersion(pluginId, versionId);
        log.info("插件配置已回滚: {} -> {}", pluginId, versionId);
        return getManager().getConfiguration(pluginId);
    }

    /**
     * 删除插件配置
     */
    public void removeConfiguration(String pluginId) {
        getManager().removeConfiguration(pluginId);
        log.info("插件配置已删除: {}", pluginId);
    }
}
