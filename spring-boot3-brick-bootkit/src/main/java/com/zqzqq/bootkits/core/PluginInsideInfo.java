package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.state.EnhancedPluginState;

/**
 * 插件内部信息接口
 * 定义了插件内部管理所需的方法
 */
public interface PluginInsideInfo extends PluginInfo {

    /**
     * 获取插件类加载器
     *
     * @return 插件的类加载器
     */
    ClassLoader getClassLoader();

    /**
     * 设置插件类加载器
     *
     * @param classLoader 插件的类加载器
     */
    void setClassLoader(ClassLoader classLoader);

    /**
     * 设置插件状态
     *
     * @param state 新的插件状态
     */
    void setPluginState(EnhancedPluginState state);

    /**
     * 获取插件状态
     *
     * @return 当前插件状态
     */
    EnhancedPluginState getPluginState();

    /**
     * 检查插件是否跟随系统
     *
     * @return 如果跟随系统返回true，否则返回false
     */
    boolean isFollowSystem();

    /**
     * 设置插件是否跟随系统
     *
     * @param follow 是否跟随系统
     */
    void setFollowSystem(boolean follow);

}
