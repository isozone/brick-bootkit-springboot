package com.zqzqq.bootkits.actuator;

import com.zqzqq.bootkits.core.health.PluginHealthChecker;
import com.zqzqq.bootkits.core.health.PluginHealthStatus;
import com.zqzqq.bootkits.core.health.PluginHealthReport;
import com.zqzqq.bootkits.core.plugin.Plugin;
import com.zqzqq.bootkits.core.plugin.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件健康检查 Actuator 端点
 * 暴露 /actuator/health 中的插件健康状态
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
@Slf4j
public class PluginHealthIndicator implements HealthIndicator {

    private final PluginManager pluginManager;
    private final PluginHealthChecker healthChecker;
    private final Map<String, PluginHealthStatus> pluginHealthCache = new ConcurrentHashMap<>();
    private long lastCheckTime = 0;
    private long checkIntervalMs = 60000; // 默认 60 秒

    public PluginHealthIndicator(PluginManager pluginManager, PluginHealthChecker healthChecker) {
        this.pluginManager = pluginManager;
        this.healthChecker = healthChecker;
    }

    @Override
    public Health health() {
        try {
            // 定期刷新缓存
            long now = System.currentTimeMillis();
            if (now - lastCheckTime > checkIntervalMs) {
                refreshHealthCache();
                lastCheckTime = now;
            }

            // 判断总体状态
            Status overallStatus = Status.UP;
            for (PluginHealthStatus status : pluginHealthCache.values()) {
                if (!status.isHealthy()) {
                    overallStatus = Status.DOWN;
                    break;
                } else if (status == PluginHealthStatus.WARNING) {
                    overallStatus = Status.OUT_OF_SERVICE;
                }
            }

            Health.Builder builder = Health.status(overallStatus);
            
            // 添加每个插件的状态
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("pluginCount", pluginHealthCache.size());
            details.put("lastCheckTime", lastCheckTime);
            
            for (Map.Entry<String, PluginHealthStatus> entry : pluginHealthCache.entrySet()) {
                details.put(entry.getKey(), entry.getValue().name());
            }
            
            builder.withDetail("plugins", details);
            
            log.debug("插件健康检查完成: {}", overallStatus);
            return builder.build();

        } catch (Exception e) {
            log.error("插件健康检查异常", e);
            return Health.down(e).build();
        }
    }

    private void refreshHealthCache() {
        if (healthChecker == null || pluginManager == null) {
            return;
        }

        List<Plugin> runningPlugins = pluginManager.getRunningPluginsList();
        pluginHealthCache.clear();

        for (Plugin plugin : runningPlugins) {
            try {
                PluginHealthReport report = healthChecker.checkHealth(plugin);
                pluginHealthCache.put(plugin.getId(), report.getOverallStatus());
            } catch (Exception e) {
                log.warn("插件 {} 健康检查失败: {}", plugin.getId(), e.getMessage());
                pluginHealthCache.put(plugin.getId(), PluginHealthStatus.UNKNOWN);
            }
        }
    }

    /**
     * 设置检查间隔
     */
    public void setCheckIntervalMs(long intervalMs) {
        this.checkIntervalMs = intervalMs;
    }
}
