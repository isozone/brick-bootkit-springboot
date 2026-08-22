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



package com.zqzqq.bootkits.loader.launcher.coexist;

import com.zqzqq.bootkits.loader.classloader.GeneralUrlClassLoader;
import com.zqzqq.bootkits.loader.launcher.AbstractMainLauncher;
import com.zqzqq.bootkits.loader.launcher.runner.MethodRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLClassLoader;


/**
 * coexist 模式 launcher
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 */
public class CoexistBaseLauncher extends AbstractMainLauncher {

    private static final Logger log = LoggerFactory.getLogger(CoexistBaseLauncher.class);
    private final MethodRunner methodRunner;

    public CoexistBaseLauncher(MethodRunner methodRunner) {
        this.methodRunner = methodRunner;
    }

    @Override
    protected ClassLoader createClassLoader(String... args) throws Exception {
        // 使用当前线程的上下文类加载器作为父类加载器，增强与Spring Boot 3.5.x的兼容性
        ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();
        if (parentClassLoader == null) {
            parentClassLoader = this.getClass().getClassLoader();
        }
        
        GeneralUrlClassLoader urlClassLoader = new GeneralUrlClassLoader(MAIN_CLASS_LOADER_NAME,
                parentClassLoader);
        addResource(urlClassLoader);
        return urlClassLoader;
    }

    @Override
    protected ClassLoader launch(ClassLoader classLoader, String... args) throws Exception {
        methodRunner.run(classLoader);
        return classLoader;
    }

    protected void addResource(GeneralUrlClassLoader classLoader) throws Exception {
        try {
            URL url = getClass().getProtectionDomain().getCodeSource().getLocation();
            if (url != null) {
                // 方法1：使用标称URLClassLoader创建新实例
                URLClassLoader tempLoader = new URLClassLoader(new URL[]{url}, classLoader.getParent());
                
                // 方法2：调用GeneralUrlClassLoader的mergeResources方法
                classLoader.mergeResources(tempLoader);
                
                log.info("Added resource to shared mode classloader: {}", url);
            }
        } catch (Exception e) {
            log.error("Failed to add resources to shared mode classloader: {}", e.getMessage(), e);
            // 方法3：最终回退方案
            System.setProperty("java.system.class.loader", 
                "com.zqzqq.bootkits.loader.classloader.GeneralUrlClassLoader");
            throw e;
        }
    }

}