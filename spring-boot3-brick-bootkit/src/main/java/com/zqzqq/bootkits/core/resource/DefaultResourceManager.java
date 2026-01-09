package com.zqzqq.bootkits.core.resource;

import com.zqzqq.bootkits.core.PluginInsideInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultResourceManager implements ResourceManager {
    private static final Logger log = LoggerFactory.getLogger(DefaultResourceManager.class);
    private final ConcurrentHashMap<String, ResourceEntry> resources = new ConcurrentHashMap<>();
    
    private static class ResourceEntry {
        final Object resource;
        final AtomicInteger refCount = new AtomicInteger(1);
        
        ResourceEntry(Object resource) {
            this.resource = resource;
        }
    }

    @Override
    public String register(PluginInsideInfo plugin, Object resource) {
        String id = plugin.getPluginId() + ":" + System.identityHashCode(resource);
        resources.put(id, new ResourceEntry(resource));
        return id;
    }

    @Override
    public void retain(String resourceId) {
        ResourceEntry entry = resources.get(resourceId);
        if (entry != null) {
            entry.refCount.incrementAndGet();
        }
    }

            @Override
            public void release(String resourceId) {
                ResourceEntry entry = resources.get(resourceId);
                if (entry != null && entry.refCount.decrementAndGet() == 0) {
                    resources.remove(resourceId);
                    if (entry.resource instanceof AutoCloseable) {
                        try {
                            ((AutoCloseable) entry.resource).close();
                        } catch (Exception e) {
                            log.warn("Failed to close resource: {}", resourceId, e);
                        }
                    }
                }
            }
            @Override
            public void release(PluginInsideInfo plugin) {
                resources.entrySet().removeIf(entry -> {
                    if (entry.getKey().startsWith(plugin.getPluginId() + ":")) {
                        if (entry.getValue().resource instanceof AutoCloseable) {
                            try {
                                ((AutoCloseable) entry.getValue().resource).close();
                            } catch (Exception e) {
                                log.warn("Failed to close resource for plugin: {}", plugin.getPluginId(), e);
                            }
                        }
                        return true;
                    }
                    return false;
                });
            }}
