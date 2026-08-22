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


package com.zqzqq.bootkits.springboot.starter.actuator;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件健康检查 Actuator 端点
 * 暴露 /actuator/health 中的插件健康状态
 * 基于主框架真实运行时（com.zqzqq.bootkits.core.PluginManager）
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Slf4j
public class PluginHealthIndicator implements HealthIndicator {

    private final PluginManager pluginManager;
    private final Map<String, Boolean> pluginHealthCache = new ConcurrentHashMap<>();
    private long lastCheckTime = 0;
    private long checkIntervalMs = 60000; // 默认 60 秒

    public PluginHealthIndicator(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
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
            for (Boolean healthy : pluginHealthCache.values()) {
                if (!healthy) {
                    overallStatus = Status.DOWN;
                    break;
                }
            }

            Health.Builder builder = Health.status(overallStatus);

            // 添加每个插件的状态
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("pluginCount", pluginHealthCache.size());
            details.put("lastCheckTime", lastCheckTime);

            for (Map.Entry<String, Boolean> entry : pluginHealthCache.entrySet()) {
                details.put(entry.getKey(), entry.getValue() ? "UP" : "DOWN");
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
        if (pluginManager == null) {
            return;
        }

        // 已启动的插件视为健康，其余状态视为不健康
        Map<String, Boolean> startedIds = new ConcurrentHashMap<>();
        List<PluginInfo> startedPlugins = pluginManager.getStartedPlugins();
        if (startedPlugins != null) {
            for (PluginInfo pluginInfo : startedPlugins) {
                startedIds.put(pluginInfo.getPluginId(), Boolean.TRUE);
            }
        }

        List<PluginInfo> allPlugins = pluginManager.getPlugins();
        pluginHealthCache.clear();
        if (allPlugins == null) {
            return;
        }
        for (PluginInfo pluginInfo : allPlugins) {
            pluginHealthCache.put(pluginInfo.getPluginId(), startedIds.containsKey(pluginInfo.getPluginId()));
        }
    }

    /**
     * 设置检查间隔
     */
    public void setCheckIntervalMs(long intervalMs) {
        this.checkIntervalMs = intervalMs;
    }
}
