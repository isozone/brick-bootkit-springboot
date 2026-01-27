package com.zqzqq.bootkits.common;

import java.util.HashSet;
import java.util.Set;

/**
 * 插件禁用AutoConfiguration配置
 *
 * @author starBlues
 * @version 3.1.0
 * @since 3.0.4
 */
public class PluginDisableAutoConfig {

    private final static Set<String> COMMON_PLUGIN_DISABLE_AUTO_CONFIG = new HashSet<>();


    static {
        COMMON_PLUGIN_DISABLE_AUTO_CONFIG.add("com.zqzqq.bootkits.integration.SpringBootPluginStarter");
    }

    public static Set<String> getCommonPluginDisableAutoConfig() {
        return COMMON_PLUGIN_DISABLE_AUTO_CONFIG;
    }
}