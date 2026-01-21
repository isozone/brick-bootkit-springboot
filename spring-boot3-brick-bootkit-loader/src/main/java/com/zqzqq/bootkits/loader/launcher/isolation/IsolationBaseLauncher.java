/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.loader.launcher.isolation;

import com.zqzqq.bootkits.loader.classloader.GenericClassLoader;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoaderFactory;
import com.zqzqq.bootkits.loader.launcher.AbstractMainLauncher;
import com.zqzqq.bootkits.loader.launcher.runner.MethodRunner;
import com.zqzqq.bootkits.loader.utils.ObjectUtils;
import com.zqzqq.bootkits.loader.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 主程序搴忓惎鍔拷?
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class IsolationBaseLauncher extends AbstractMainLauncher {

    private static final Logger log = LoggerFactory.getLogger(IsolationBaseLauncher.class);
    
    private final MethodRunner methodRunner;

    public IsolationBaseLauncher(MethodRunner methodRunner) {
        this.methodRunner = methodRunner;
    }

    @Override
    protected boolean resolveThreadClassLoader() {
        return false;
    }

    @Override
    protected ClassLoader createClassLoader(String... args) throws Exception {
        return new GenericClassLoader(MAIN_CLASS_LOADER_NAME, getParentClassLoader(),
                getResourceLoaderFactory(args));
    }

    @Override
    protected ClassLoader launch(ClassLoader classLoader, String... args) throws Exception {
        methodRunner.run(classLoader);
        ResourceUtils.release(classLoader);
        return classLoader;
    }

    protected ResourceLoaderFactory getResourceLoaderFactory(String... args){
        return ResourceLoaderFactoryGetter.get(MAIN_CLASS_LOADER_NAME, args);
    }

    protected ClassLoader getParentClassLoader(){
        // 获取当前类的类加载器作为起始点
        ClassLoader classLoader = IsolationBaseLauncher.class.getClassLoader();
        
        // 回退到线程上下文类加载器
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        
        // 回退到系统类加载器
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }
        
        // 最终回退：确保至少有一个有效的类加载器
        if (classLoader == null) {
            classLoader = IsolationBaseLauncher.class.getClassLoader();
        }
        
        // 记录调试信息
        log.debug("隔离模式父类加载器: {}", classLoader);
        
        return classLoader;
    }

    protected Set<URL> getBaseResource() {
        Set<URL> urls = new HashSet<>();
        String classPath = ManagementFactory.getRuntimeMXBean().getClassPath();
        if(!ObjectUtils.isEmpty(classPath)){
            String[] classPathStr = classPath.split(";");
            for (String path : classPathStr) {
                try {
                    urls.add(new URL(path));
                } catch (MalformedURLException e) {
                    // 忽略
                }
            }
        }
        ClassLoader sourceClassLoader = Thread.currentThread().getContextClassLoader();
        if(sourceClassLoader instanceof URLClassLoader){
            URLClassLoader urlClassLoader = (URLClassLoader) sourceClassLoader;
            final URL[] urLs = urlClassLoader.getURLs();
            urls.addAll(Arrays.asList(urLs));
        }
        return urls;
    }

}

