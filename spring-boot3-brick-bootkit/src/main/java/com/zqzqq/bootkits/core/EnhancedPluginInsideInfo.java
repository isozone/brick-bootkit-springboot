package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.state.DefaultStateInterceptor;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.core.state.PluginStateInterceptor;
import lombok.Getter;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 增强插件内部信息实现（线程安全）
 * 提供者浜嗗畬鏁寸殑插件鐢熷懡鍛ㄦ湡绠＄悊鍜岃祫婧愯窡韪姛锟?
 */
public class EnhancedPluginInsideInfo implements PluginInsideInfo {
    private static final Logger LOGGER = Logger.getLogger(EnhancedPluginInsideInfo.class.getName());
    
    private final InsidePluginDescriptor descriptor;
    private volatile ClassLoader classLoader;
    private volatile PluginState state;
    private final List<PluginStateInterceptor> interceptors;
    private boolean followSystem = false;
    @Getter
    private Supplier<Map<String, Object>> extensionInfoSupplier;
    private long startTime;
    private long stopTime;
    
    // 用于跟踪资源，确保在卸载时清理
    private final Set<WeakReference<Object>> resourceTracker = Collections.synchronizedSet(new HashSet<>());
    
    public EnhancedPluginInsideInfo(InsidePluginDescriptor descriptor) {
        this.descriptor = Objects.requireNonNull(descriptor);
        this.interceptors = new CopyOnWriteArrayList<>();
        this.interceptors.add(new DefaultStateInterceptor());
        this.extensionInfoSupplier = Collections::emptyMap;
    }

    @Override
    public String getPluginId() {
        return descriptor.getPluginId();
    }

    @Override
    public String getPluginPath() {
        return descriptor.getPluginPath();
    }

    @Override
    public InsidePluginDescriptor getPluginDescriptor() {
        return descriptor;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        ClassLoader oldLoader = this.classLoader;
        this.classLoader = classLoader;
        
        // 如果设置了新的ClassLoader，登记到资源跟踪
        if (classLoader != null) {
            trackResource(classLoader);
        }
        
        // 清理之前的ClassLoader
        if (oldLoader != null) {
            cleanupClassLoader(oldLoader);
        }
    }

    @Override
    public EnhancedPluginState getPluginState() {
        return (EnhancedPluginState) state;
    }

    @Override
    public void setPluginState(EnhancedPluginState state) {
        setEnhancedPluginState(state);
    }

    private void setEnhancedPluginState(EnhancedPluginState newState) {
        PluginState oldState = this.state;
        
        try {
            // 预先拦截检查
            for (PluginStateInterceptor interceptor : interceptors) {
                if (!interceptor.preStateChange(this, oldState, newState)) {
                    return;
                }
            }
            
            // 鐘舵€佽浆鎹㈤獙锟?
            if (oldState != null && oldState instanceof EnhancedPluginState && 
                !((EnhancedPluginState)oldState).canTransitionTo(newState)) {
                throw new IllegalStateException(String.format(
                    "Invalid state transition from %s to %s for plugin %s",
                    oldState, newState, getPluginId()));
            }
            
            // 执行状态变动锟?
            this.state = newState;
            
            // 记录时间戳
            if (newState == EnhancedPluginState.STARTED) {
                this.startTime = System.currentTimeMillis();
            } else if (newState == EnhancedPluginState.STOPPED) {
                this.stopTime = System.currentTimeMillis();
            }
            
            // 特殊状态处理
            if (newState == EnhancedPluginState.UNLOADED) {
                // 鍦ㄥ嵏杞界姸鎬佷笅清理所有夎祫锟?
                cleanupAllResources();
            }
            
            // 鍚庣疆閫氱煡
            for (PluginStateInterceptor interceptor : interceptors) {
                interceptor.postStateChange(this, oldState, newState);
            }
        } catch (Exception e) {
            // 异常通知
            for (PluginStateInterceptor interceptor : interceptors) {
                interceptor.onStateChangeFailure(this, oldState, newState, e);
            }
            throw new IllegalStateException("Failed to change plugin state: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isFollowSystem() {
        return followSystem;
    }

    @Override
    public void setFollowSystem(boolean follow) {
        this.followSystem = follow;
    }

    @Override
    public Map<String, Object> getExtensionInfo() {
        return extensionInfoSupplier.get();
    }


    public void setExtensionInfoSupplier(Supplier<Map<String, Object>> supplier) {
        this.extensionInfoSupplier = supplier != null ? supplier : Collections::emptyMap;
    }


    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getStopTime() {
        return stopTime;
    }


    public PluginInfo toPluginInfo() {
        return new DefaultPluginInfo(descriptor);
    }
    
    public void addInterceptor(PluginStateInterceptor interceptor) {
        if (interceptor != null) {
            interceptors.add(interceptor);
        }
    }
    
    /**
     * 移除拦截器
     * @param interceptor 要移除的拦截器
     * @return 如果成功移除返回true，否则返回false
     */
    public boolean removeInterceptor(PluginStateInterceptor interceptor) {
        return interceptors.remove(interceptor);
    }
    
    /**
     * 清理所有拦截器
     */
    public void clearInterceptors() {
        // 保留默认拦截器
        PluginStateInterceptor defaultInterceptor = null;
        for (PluginStateInterceptor interceptor : interceptors) {
            if (interceptor instanceof DefaultStateInterceptor) {
                defaultInterceptor = interceptor;
                break;
            }
        }
        
        interceptors.clear();
        
        if (defaultInterceptor != null) {
            interceptors.add(defaultInterceptor);
        } else {
            interceptors.add(new DefaultStateInterceptor());
        }
    }
    
    /**
     * 跟踪资源以便在卸载时清理
     * @param resource 闇€瑕佽窡韪殑资源
     */
    public void trackResource(Object resource) {
        if (resource != null) {
            resourceTracker.add(new WeakReference<>(resource));
        }
    }
    
    /**
     * 清理所有夎窡韪殑资源
     */
    private void cleanupAllResources() {
        // 清理所有夎窡韪殑资源
        Iterator<WeakReference<Object>> iterator = resourceTracker.iterator();
        while (iterator.hasNext()) {
            WeakReference<Object> ref = iterator.next();
            Object resource = ref.get();
            if (resource != null) {
                if (resource instanceof ClassLoader) {
                    cleanupClassLoader((ClassLoader) resource);
                } else if (resource instanceof AutoCloseable) {
                    try {
                        ((AutoCloseable) resource).close();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error closing resource: " + resource, e);
                    }
                }
            }
            iterator.remove();
        }
        
        // 清理拦截器
        clearInterceptors();
        
        // 清理扩展信息提供者
        this.extensionInfoSupplier = Collections::emptyMap;
    }
    
    /**
     * 增强的ClassLoader清理方法
     * 处理各种类型鐨凜lassLoader骞跺皾璇曢噴鏀惧叾资源
     */
    private void cleanupClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        
        try {
            // 1. 如果是Closeable接口，直接关闭
            if (classLoader instanceof Closeable) {
                try {
                    ((Closeable) classLoader).close();
                    LOGGER.fine("Closed ClassLoader via Closeable interface: " + classLoader);
                    return;
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing ClassLoader: " + classLoader, e);
                }
            }
            
            // 2. 如果是URLClassLoader，尝试关闭其URLs
            if (classLoader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
                try {
                    // 尝试使用close方法（JDK 1.7+）
                    Method closeMethod = URLClassLoader.class.getDeclaredMethod("close");
                    closeMethod.setAccessible(true);
                    closeMethod.invoke(urlClassLoader);
                    LOGGER.fine("Closed URLClassLoader via reflection: " + classLoader);
                    return;
                } catch (Exception e) {
                    // 如果没有close方法或调用失败，尝试清理URLs
                    try {
                        Field ucpField = URLClassLoader.class.getDeclaredField("ucp");
                        ucpField.setAccessible(true);
                        Object ucp = ucpField.get(urlClassLoader);
                        
                        Field loadersField = ucp.getClass().getDeclaredField("loaders");
                        loadersField.setAccessible(true);
                        Collection<?> loaders = (Collection<?>) loadersField.get(ucp);
                        
                        for (Object loader : loaders) {
                            try {
                                Field loaderField = loader.getClass().getDeclaredField("jar");
                                loaderField.setAccessible(true);
                                Object jarFile = loaderField.get(loader);
                                if (jarFile != null) {
                                    Method closeJarMethod = jarFile.getClass().getDeclaredMethod("close");
                                    closeJarMethod.setAccessible(true);
                                    closeJarMethod.invoke(jarFile);
                                }
                            } catch (Exception ex) {
                                LOGGER.log(Level.FINE, "Failed to close individual loader resource", ex);
                            }
                        }
                        loaders.clear();
                        
                        Field pathField = ucp.getClass().getDeclaredField("path");
                        pathField.setAccessible(true);
                        List<?> paths = (List<?>) pathField.get(ucp);
                        paths.clear();
                        
                        Field urlsField = ucp.getClass().getDeclaredField("urls");
                        urlsField.setAccessible(true);
                        List<URL> urls = (List<URL>) urlsField.get(ucp);
                        urls.clear();
                        
                        LOGGER.fine("Cleaned URLClassLoader resources via reflection: " + classLoader);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Error cleaning URLClassLoader resources: " + classLoader, ex);
                    }
                }
            }
            
            // 3. 尝试通过反射清理ClassLoader的缓存
            try {
                // 清理类缓存
                Field classesField = ClassLoader.class.getDeclaredField("classes");
                classesField.setAccessible(true);
                Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);
                classes.clear();
                
                // 尝试清理其它鍙兘鐨勭紦瀛樺瓧锟?
                clearFieldIfExists(classLoader, "resourceCache");
                clearFieldIfExists(classLoader, "packageMap");
                clearFieldIfExists(classLoader, "nativeLibraries");
                
                LOGGER.fine("Cleaned ClassLoader caches via reflection: " + classLoader);
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Could not clear ClassLoader caches: " + classLoader, e);
            }
            
        } catch (Throwable t) {
            // 捕获所有异常，确保不会影响其他操作
            LOGGER.log(Level.WARNING, "Unexpected error during ClassLoader cleanup: " + classLoader, t);
        }
    }
    
    /**
     * 尝试清理ClassLoader中的指定字段
     */
    private void clearFieldIfExists(ClassLoader classLoader, String fieldName) {
        try {
            Field field = ClassLoader.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object obj = field.get(classLoader);
            if (obj instanceof Map) {
                ((Map<?, ?>) obj).clear();
            } else if (obj instanceof Collection) {
                ((Collection<?>) obj).clear();
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not clear field '" + fieldName + "' from ClassLoader", e);
        }
    }
}
