package com.zqzqq.bootkits.core.eventbus;

/**
 * 事件监听器接口
 * 插件通过此接口订阅事件
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
public interface PluginEventListener {

    /**
     * 处理事件
     * 
     * @param event 事件对象
     */
    void onEvent(PluginEvent event);

    /**
     * 获取监听器优先级（数值越小优先级越高）
     */
    default int priority() {
        return 0;
    }

    /**
     * 是否异步处理（默认同步）
     */
    default boolean async() {
        return false;
    }

    /**
     * 是否支持的事件类型
     * 返回 false 表示支持所有类型
     */
    default boolean supportsType(PluginEvent.EventType type) {
        return true;
    }
}
