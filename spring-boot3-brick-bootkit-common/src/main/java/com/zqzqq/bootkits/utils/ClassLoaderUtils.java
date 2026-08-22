/**
 * Copyright 2019-Present starBlues and the brick-bootkit contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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
