package com.zqzqq.bootkits.core.eventbus;

import com.zqzqq.bootkits.core.logging.PluginLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 插件事件总线
 * 支持订阅/发布模式，同步/异步事件，优先级排序
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
public class PluginEventBus {

    private static final PluginLogger logger = PluginLogger.getLogger(PluginEventBus.class);

    /** 事件监听器注册表: eventType -> pluginId -> listeners */
    private final Map<PluginEvent.EventType, Map<String, List<PluginEventListener>>> listeners = new ConcurrentHashMap<>();
    
    /** 所有注册的监听器（按优先级排序） */
    private final List<PluginEventListener> allListeners = new CopyOnWriteArrayList<>();

    /** 异步事件执行器 */
    private final ExecutorService asyncExecutor;

    /** 事件处理统计 */
    private final Map<String, AtomicInteger> eventCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> eventDurations = new ConcurrentHashMap<>();

    public PluginEventBus() {
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "PluginEventBus-Async");
            t.setDaemon(true);
            return t;
        });
        logger.info("system", "插件事件总线已初始化");
    }

    /**
     * 注册事件监听器
     */
    public void registerListener(String pluginId, PluginEventListener listener) {
        Objects.requireNonNull(pluginId, "pluginId cannot be null");
        Objects.requireNonNull(listener, "listener cannot be null");

        allListeners.add(listener);
        
        // 按事件类型注册
        for (PluginEvent.EventType type : PluginEvent.EventType.values()) {
            if (listener.supportsType(type)) {
                listeners.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(pluginId, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(listener);
            }
        }

        logger.info(pluginId, "eventbus", "监听器已注册: {} 优先级={}", 
                listener.getClass().getSimpleName(), listener.priority());
    }

    /**
     * 注销事件监听器
     */
    public void unregisterListener(String pluginId, PluginEventListener listener) {
        allListeners.removeIf(l -> l == listener);
        
        for (Map<String, List<PluginEventListener>> pluginListeners : listeners.values()) {
            List<PluginEventListener> list = pluginListeners.get(pluginId);
            if (list != null) {
                list.remove(listener);
            }
        }

        logger.info(pluginId, "eventbus", "监听器已注销: {}", listener.getClass().getSimpleName());
    }

    /**
     * 发布事件（同步）
     */
    public void publish(PluginEvent event) {
        Objects.requireNonNull(event, "event cannot be null");
        
        long startTime = System.currentTimeMillis();
        
        // 获取该事件类型的所有监听器
        Map<String, List<PluginEventListener>> pluginListeners = 
                listeners.getOrDefault(event.getType(), Collections.emptyMap());
        
        // 收集匹配的监听器
        List<PluginEventListener> matchedListeners = new ArrayList<>();
        
        if (event.isBroadcast()) {
            // 广播事件：匹配所有监听器
            for (Map.Entry<String, List<PluginEventListener>> entry : pluginListeners.entrySet()) {
                if (!event.getSourcePluginId().equals(entry.getKey())) { // 排除发送者
                    matchedListeners.addAll(entry.getValue());
                }
            }
        } else {
            // 定向事件：只发送给目标插件
            List<PluginEventListener> targetListeners = pluginListeners.get(event.getTargetPluginId());
            if (targetListeners != null) {
                matchedListeners.addAll(targetListeners);
            }
        }

        // 按优先级排序
        matchedListeners.sort(Comparator.comparingInt(PluginEventListener::priority));

        // 分发事件
        for (PluginEventListener listener : matchedListeners) {
            if (event.isHandled()) {
                break; // 事件已被处理，停止分发
            }
            
            try {
                if (listener.async()) {
                    asyncExecutor.submit(() -> {
                        try {
                            listener.onEvent(event);
                        } catch (Exception e) {
                            logger.error(event.getSourcePluginId(), "eventbus",
                                    "异步事件处理异常: {}", e.getMessage(), e);
                        }
                    });
                } else {
                    listener.onEvent(event);
                }
            } catch (Exception e) {
                logger.error(event.getSourcePluginId(), "eventbus",
                        "事件处理异常: {}", e.getMessage(), e);
            }
        }

        // 统计
        String key = event.getType() + "." + event.getSourcePluginId();
        eventCounters.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        eventDurations.put(key, new AtomicLong(System.currentTimeMillis() - startTime));
        
        logger.debug(event.getSourcePluginId(), "eventbus", 
                "事件已分发: {} -> {} 个监听器", event.getType(), matchedListeners.size());
    }

    /**
     * 发布事件（异步）
     */
    public void publishAsync(PluginEvent event) {
        asyncExecutor.submit(() -> publish(event));
    }

    /**
     * 创建事件
     */
    public PluginEvent createEvent(PluginEvent.EventType type, String sourcePluginId) {
        return new PluginEvent(type, sourcePluginId);
    }

    public PluginEvent createEvent(PluginEvent.EventType type, String sourcePluginId, String targetPluginId) {
        return new PluginEvent(type, sourcePluginId, targetPluginId);
    }

    /**
     * 获取事件统计
     */
    public Map<String, Integer> getEventCounts() {
        return new HashMap<>(eventCounters);
    }

    /**
     * 关闭事件总线
     */
    public void shutdown() {
        asyncExecutor.shutdown();
        try {
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("system", "插件事件总线已关闭");
    }
}
