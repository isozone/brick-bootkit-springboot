package com.example.capabilitydemo.listener;

import com.zqzqq.bootkits.core.eventbus.PluginEvent;
import com.zqzqq.bootkits.sdk.annotation.BrickEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 插件事件监听器演示。
 * <p>
 * 使用 {@code @BrickEventListener} 监听插件生命周期事件，
 * 事件总线（PluginEventBus）会按注解自动装配该监听器。
 */
@Component
public class CapabilityEventListeners {

    private static final Logger log = LoggerFactory.getLogger(CapabilityEventListeners.class);

    @BrickEventListener({PluginEvent.EventType.PLUGIN_STARTED})
    public void onPluginStarted() {
        log.info("[capability-demo] 收到事件: 插件已启动");
    }

    @BrickEventListener({PluginEvent.EventType.PLUGIN_STOPPED})
    public void onPluginStopped() {
        log.info("[capability-demo] 收到事件: 插件已停止");
    }

    @BrickEventListener(value = {PluginEvent.EventType.PLUGIN_ERROR}, priority = 5)
    public void onPluginError() {
        log.warn("[capability-demo] 收到事件: 插件错误");
    }
}
