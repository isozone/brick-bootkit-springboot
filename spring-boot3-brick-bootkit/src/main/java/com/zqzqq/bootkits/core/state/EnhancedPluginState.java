package com.zqzqq.bootkits.core.state;

import com.zqzqq.bootkits.core.PluginState;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * 增强型插件状态枚举
 * 细化插件在生命周期中的各种状态以及它们之间的转换关系
 */
public enum EnhancedPluginState implements PluginState {
    PARSED("插件描述文件已解析"),
    LOADED("插件已加载"),
    STARTED("插件已启动"),
    STOPPED("插件已停止"),
    DISABLED("插件已禁用"), 
    UNLOADED("插件已卸载"),
    STOPPED_FAILURE("插件停止失败"),
    STARTED_FAILURE("插件启动失败");
    private Set<EnhancedPluginState> allowedTransitions;
    private final String description;

    static {
        PARSED.allowedTransitions = EnumSet.of(LOADED, DISABLED);
        LOADED.allowedTransitions = EnumSet.of(STARTED, DISABLED, UNLOADED);
        STARTED.allowedTransitions = EnumSet.of(STOPPED, UNLOADED);
        STOPPED.allowedTransitions = EnumSet.of(STARTED, UNLOADED);
        DISABLED.allowedTransitions = EnumSet.of(LOADED, UNLOADED);
        UNLOADED.allowedTransitions = EnumSet.noneOf(EnhancedPluginState.class);
    }

    EnhancedPluginState(String description) {
        this.description = description;
    }

    /**
     * 检查状态转换是否有效
     */
    public boolean canTransitionTo(EnhancedPluginState newState) {
        Objects.requireNonNull(newState);
        if (this == newState) {
            throw new IllegalArgumentException("Cannot transition to same state");
        }
        return allowedTransitions.contains(newState);
    }

    @Override
    public boolean canTransitionTo(PluginState targetState) {
        if (targetState instanceof EnhancedPluginState) {
            return canTransitionTo((EnhancedPluginState) targetState);
        }
        return false;
    }

    /**
     * 获取所有允许的转换状态
     */
    public Set<EnhancedPluginState> getAllowedTransitions() {
        return EnumSet.copyOf(allowedTransitions);
    }

    /**
     * 获取状态描述
     */
    @Override
    public String getDescription() {
        return description;
    }
}