package com.zqzqq.bootkits.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.lang.reflect.Method;

/**
 * 类加载器工具类
 * @since 3.5.5
 */
public class ClassLoaderUtils {
    
    private static final Logger log = LoggerFactory.getLogger(ClassLoaderUtils.class);
    
    private ClassLoaderUtils() {
        // 私有构造函数
    }

    /**
     * 安全关闭类加载器
     * @param classLoader 要关闭的类加载器
     */
    public static void closeQuietly(ClassLoader classLoader) {
        if (classLoader == null) {
            return;
        }
        
        try {
            if (classLoader instanceof Closeable) {
                ((Closeable) classLoader).close();
            } else if (classLoader instanceof AutoCloseable) {
                ((AutoCloseable) classLoader).close();
            } else {
                Method closeMethod = classLoader.getClass().getMethod("close");
                closeMethod.invoke(classLoader);
            }
        } catch (NoSuchMethodException e) {
            // 类加载器没有close方法，忽略
        } catch (Exception e) {
            log.warn("Failed to close ClassLoader: {}", e.getMessage(), e);
        }
    }
}
