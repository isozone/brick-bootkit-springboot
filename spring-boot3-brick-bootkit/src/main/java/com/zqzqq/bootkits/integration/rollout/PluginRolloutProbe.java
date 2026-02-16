package com.zqzqq.bootkits.integration.rollout;

import com.zqzqq.bootkits.core.PluginInfo;

/**
 * Host-provided probe used in gray rollout.
 */
public interface PluginRolloutProbe {

    String getName();

    PluginRolloutProbeResult probe(String pluginId, PluginInfo pluginInfo);
}
