package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.eventbus.PluginEvent;
import com.zqzqq.bootkits.core.eventbus.PluginEventBus;
import com.zqzqq.bootkits.core.eventbus.PluginEventListener;
import com.zqzqq.bootkits.core.exception.PluginException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 插件事件总线 Web 服务。
 * 提供事件统计、事件类型与最近事件流查询能力。
 *
 * @author brick-bootkit
 */
@Slf4j
@Service
public class EventBusWebService {

    private static final int MAX_RECENT_EVENTS = 200;

    private final ObjectProvider<PluginEventBus> eventBusProvider;
    private final ConcurrentLinkedDeque<PluginEvent> recentEvents = new ConcurrentLinkedDeque<>();

    public EventBusWebService(ObjectProvider<PluginEventBus> eventBusProvider) {
        this.eventBusProvider = eventBusProvider;
        PluginEventBus eventBus = eventBusProvider.getIfAvailable();
        if (eventBus != null) {
            // 注册一个 Web 控制台监听器，记录最近事件流
            eventBus.registerListener("web-console", new RecentEventsListener());
        }
    }

    private PluginEventBus getEventBus() {
        PluginEventBus eventBus = eventBusProvider.getIfAvailable();
        if (eventBus == null) {
            throw new PluginException("插件事件总线未启用");
        }
        return eventBus;
    }

    /**
     * 获取事件统计
     */
    public Map<String, Integer> getEventCounts() {
        return getEventBus().getEventCounts();
    }

    /**
     * 获取事件类型列表
     */
    public List<String> getEventTypes() {
        List<String> types = new ArrayList<>();
        for (PluginEvent.EventType type : PluginEvent.EventType.values()) {
            types.add(type.name());
        }
        return types;
    }

    /**
     * 获取最近事件流
     */
    public List<RecentEvent> getRecentEvents(int limit) {
        int max = Math.min(Math.max(limit, 1), MAX_RECENT_EVENTS);
        List<RecentEvent> result = new ArrayList<>();
        int count = 0;
        for (PluginEvent event : recentEvents) {
            if (count >= max) {
                break;
            }
            result.add(new RecentEvent(
                    event.getType() == null ? "UNKNOWN" : event.getType().name(),
                    event.getSourcePluginId(),
                    event.getTargetPluginId(),
                    event.getTimestamp() == null ? 0L : event.getTimestamp().toEpochMilli(),
                    event.isBroadcast()
            ));
            count++;
        }
        return result;
    }

    /**
     * 事件类型枚举名称
     */
    public List<String> getEventTypeNames() {
        return Arrays.stream(PluginEvent.EventType.values()).map(Enum::name).toList();
    }

    /**
     * Web 控制台事件监听器：仅记录，不消费事件。
     */
    private class RecentEventsListener implements PluginEventListener {

        @Override
        public void onEvent(PluginEvent event) {
            recentEvents.addFirst(event);
            while (recentEvents.size() > MAX_RECENT_EVENTS) {
                recentEvents.pollLast();
            }
        }

        @Override
        public boolean supportsType(PluginEvent.EventType type) {
            return true;
        }
    }

    /**
     * 最近事件（Web 展示视图）
     */
    public static class RecentEvent {
        private final String type;
        private final String sourcePluginId;
        private final String targetPluginId;
        private final long timestamp;
        private final boolean broadcast;

        public RecentEvent(String type, String sourcePluginId, String targetPluginId,
                           long timestamp, boolean broadcast) {
            this.type = type;
            this.sourcePluginId = sourcePluginId;
            this.targetPluginId = targetPluginId;
            this.timestamp = timestamp;
            this.broadcast = broadcast;
        }

        public String getType() {
            return type;
        }

        public String getSourcePluginId() {
            return sourcePluginId;
        }

        public String getTargetPluginId() {
            return targetPluginId;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isBroadcast() {
            return broadcast;
        }
    }
}
