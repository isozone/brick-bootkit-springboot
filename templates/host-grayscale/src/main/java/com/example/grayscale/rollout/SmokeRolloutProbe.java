package com.example.grayscale.rollout;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbe;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 冒烟探针示例：灰度升级前的健康检查。
 * <p>
 * 实现 {@link PluginRolloutProbe} SPI 并以 Spring Bean 注入，
 * 框架在灰度升级时会自动收集并依次执行容器中的所有探针。
 * <p>
 * 该探针检查升级后的插件描述符基本信息是否完整（pluginId/版本/主类）。
 */
@Component
public class SmokeRolloutProbe implements PluginRolloutProbe {

    private static final Logger log = LoggerFactory.getLogger(SmokeRolloutProbe.class);

    @Override
    public String getName() {
        return "smoke-probe";
    }

    @Override
    public PluginRolloutProbeResult probe(String pluginId, PluginInfo pluginInfo) {
        if (pluginInfo == null) {
            return PluginRolloutProbeResult.reject("插件信息为空，拒绝灰度升级");
        }
        if (pluginInfo.getPluginDescriptor() == null) {
            return PluginRolloutProbeResult.reject("插件描述符缺失，拒绝灰度升级");
        }

        String version = pluginInfo.getPluginDescriptor().getPluginVersion();
        String mainClass = pluginInfo.getPluginDescriptor().getMainClassName();
        if (version == null || version.trim().isEmpty()) {
            return PluginRolloutProbeResult.reject("插件版本为空，拒绝灰度升级");
        }
        if (mainClass == null || mainClass.trim().isEmpty()) {
            return PluginRolloutProbeResult.reject("插件主类为空，拒绝灰度升级");
        }

        log.info("冒烟探针通过: pluginId={}, version={}, mainClass={}", pluginId, version, mainClass);
        return PluginRolloutProbeResult.pass("插件基本信息完整，冒烟探针通过");
    }
}
