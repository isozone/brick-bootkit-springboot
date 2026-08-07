package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutMode;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbe;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件灰度发布 Web 服务。
 * 提供灰度配置查看、灰度探针列表与手动灰度决策模拟能力。
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class RolloutWebService {

    private final ObjectProvider<IntegrationConfiguration> configurationProvider;
    private final ObjectProvider<PluginManager> pluginManagerProvider;
    private final List<PluginRolloutProbe> rolloutProbes;

    public RolloutWebService(ObjectProvider<IntegrationConfiguration> configurationProvider,
                             ObjectProvider<PluginManager> pluginManagerProvider,
                             List<PluginRolloutProbe> rolloutProbes) {
        this.configurationProvider = configurationProvider;
        this.pluginManagerProvider = pluginManagerProvider;
        this.rolloutProbes = rolloutProbes == null ? new ArrayList<>() : rolloutProbes;
    }

    private IntegrationConfiguration getConfiguration() {
        IntegrationConfiguration configuration = configurationProvider.getIfAvailable();
        if (configuration == null) {
            throw new PluginException("插件集成配置不可用");
        }
        return configuration;
    }

    private PluginManager getPluginManager() {
        PluginManager manager = pluginManagerProvider.getIfAvailable();
        if (manager == null) {
            throw new PluginException("插件管理器不可用");
        }
        return manager;
    }

    /**
     * 获取灰度发布配置
     */
    public Map<String, Object> getRolloutConfig() {
        IntegrationConfiguration configuration = getConfiguration();
        Map<String, Object> result = new LinkedHashMap<>();
        PluginRolloutMode mode = configuration.pluginRolloutMode();
        result.put("mode", mode == null ? "DIRECT" : mode.name());
        result.put("modeDescription", mode == PluginRolloutMode.GRAY ? "灰度发布" : "直接发布");
        result.put("autoStart", configuration.pluginRolloutAutoStart());
        result.put("rollbackOnFailure", configuration.pluginRolloutRollbackOnFailure());
        result.put("probeCount", rolloutProbes.size());
        return result;
    }

    /**
     * 获取已注册的灰度探针列表
     */
    public List<String> getProbeNames() {
        List<String> names = new ArrayList<>();
        for (PluginRolloutProbe probe : rolloutProbes) {
            names.add(probe.getName());
        }
        return names;
    }

    /**
     * 对指定插件运行全部灰度探针，模拟灰度决策
     */
    public RolloutDecision checkPlugin(String pluginId) {
        if (pluginId == null || pluginId.trim().isEmpty()) {
            throw new PluginException("插件 ID 不能为空");
        }
        PluginManager pluginManager = getPluginManager();
        PluginInfo pluginInfo = pluginManager.getPlugin(pluginId);
        if (pluginInfo == null) {
            throw new PluginException("插件不存在: " + pluginId);
        }

        RolloutDecision decision = new RolloutDecision(pluginId);
        boolean allPassed = true;
        for (PluginRolloutProbe probe : rolloutProbes) {
            String name = probe.getName();
            try {
                PluginRolloutProbeResult result = probe.probe(pluginId, pluginInfo);
                boolean passed = result != null && result.isPassed();
                String message = result == null ? "探针无返回结果" : result.getMessage();
                decision.addProbeResult(name, passed, message);
                if (!passed) {
                    allPassed = false;
                }
            } catch (Exception e) {
                log.warn("灰度探针执行异常. probe={}, plugin={}", name, pluginId, e);
                decision.addProbeResult(name, false, "探针执行异常: " + e.getMessage());
                allPassed = false;
            }
        }
        decision.setPassed(allPassed);
        return decision;
    }

    /**
     * 灰度决策结果
     */
    public static class RolloutDecision {
        private final String pluginId;
        private boolean passed;
        private final List<ProbeResult> probes = new ArrayList<>();

        public RolloutDecision(String pluginId) {
            this.pluginId = pluginId;
        }

        public void addProbeResult(String name, boolean passed, String message) {
            probes.add(new ProbeResult(name, passed, message));
        }

        public String getPluginId() {
            return pluginId;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public List<ProbeResult> getProbes() {
            return probes;
        }
    }

    public static class ProbeResult {
        private final String name;
        private final boolean passed;
        private final String message;

        public ProbeResult(String name, boolean passed, String message) {
            this.name = name;
            this.passed = passed;
            this.message = message;
        }

        public String getName() {
            return name;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getMessage() {
            return message;
        }
    }
}
