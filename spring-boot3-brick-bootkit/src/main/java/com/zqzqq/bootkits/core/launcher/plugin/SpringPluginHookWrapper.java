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

package com.zqzqq.bootkits.core.launcher.plugin;

import com.zqzqq.bootkits.core.PluginCloseType;
import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.core.exception.PluginProhibitStopException;
import com.zqzqq.bootkits.core.launcher.plugin.involved.PluginLaunchInvolved;
import com.zqzqq.bootkits.loader.PluginResourceStorage;
import com.zqzqq.bootkits.spring.ApplicationContext;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import com.zqzqq.bootkits.spring.WebConfig;
import com.zqzqq.bootkits.spring.web.thymeleaf.ThymeleafConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * SpringPluginHook-Wrapper
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
@Slf4j
public class SpringPluginHookWrapper implements SpringPluginHook {
    private static final Logger log = LoggerFactory.getLogger(SpringPluginHookWrapper.class);


    private final SpringPluginHook target;
    private final PluginInsideInfo pluginInsideInfo;
    private final PluginLaunchInvolved pluginLaunchInvolved;
    private final ClassLoader classLoader;

    public SpringPluginHookWrapper(SpringPluginHook target, PluginInsideInfo pluginInsideInfo,
                                   PluginLaunchInvolved pluginLaunchInvolved,
                                   ClassLoader classLoader) {
        this.target = target;
        this.pluginInsideInfo = pluginInsideInfo;
        this.pluginLaunchInvolved = pluginLaunchInvolved;
        this.classLoader = classLoader;
    }

    @Override
    public void stopVerify() throws PluginProhibitStopException {
        target.stopVerify();
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return target.getApplicationContext();
    }

    @Override
    public WebConfig getWebConfig() {
        return target.getWebConfig();
    }

    @Override
    public ThymeleafConfig getThymeleafConfig() {
        return null;
    }

    @Override
    public void close(PluginCloseType closeType) throws Exception {
        String pluginId = pluginInsideInfo.getPluginId();
        log.debug("开始关闭插件 {}", pluginId);
        
        // 1. 关闭 application 等信息
        try {
            target.close(closeType);
        } catch (Exception e){
            log.error("关闭插件应用程序上下文异常 {}", e.getMessage(), e);
        }
        
        // 2. 关闭 pluginLaunchInvolved
        try {
            pluginLaunchInvolved.close(pluginInsideInfo, classLoader);
        } catch (Exception e){
            log.error("关闭插件启动组件异常: {}", e.getMessage(), e);
        }
        
        // 3. 关闭classloader
        try {
            if (classLoader instanceof Closeable) {
                ((Closeable) classLoader).close();
            } else if (classLoader instanceof AutoCloseable) {
                ((AutoCloseable) classLoader).close();
            }
        } catch (Exception e) {
            log.error("关闭插件类加载器异常: {}", e.getMessage(), e);
        }
        
        // 4. 移除插件jar等信息
        try {
            PluginResourceStorage.removePlugin(pluginId);
        } catch (Exception e) {
            log.error("移除插件资源存储异常: {}", e.getMessage(), e);
        }
        
        // 5. 建议 JVM 进行垃圾回收
        try {
            System.gc();
            log.debug("插件[{}]关闭完成", pluginId);
        } catch (Exception e) {
            log.error("插件关闭后内存回收异常 {}", e.getMessage(), e);
        }
    }
}

