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

package com.zqzqq.bootkits.integration.listener;

import com.zqzqq.bootkits.utils.SpringBeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * 插件初始化监听器工厂
 *
 * @author starBlues
 * @version 3.0.0
 */
public class PluginInitializerListenerFactory implements PluginInitializerListener {

    private static final Logger log = LoggerFactory.getLogger(PluginInitializerListenerFactory.class);
    private final List<PluginInitializerListener> pluginInitializerListeners = new ArrayList<>();

    public final ApplicationContext applicationContext;

    public PluginInitializerListenerFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        // 添加默认的初始化监听器
        pluginInitializerListeners.add(new DefaultInitializerListener(applicationContext));
        addExtendPluginListener(applicationContext);
    }

    private void addExtendPluginListener(ApplicationContext applicationContext){
        List<PluginInitializerListener> initializerListeners = SpringBeanUtils.getBeans(applicationContext,
                PluginInitializerListener.class);
        pluginInitializerListeners.addAll(initializerListeners);
    }

    @Override
    public void before() {
        try {
            for (PluginInitializerListener pluginInitializerListener : pluginInitializerListeners) {
                pluginInitializerListener.before();
            }
        } catch (Exception e){
            log.error("Plugin initializer before() error", e);
        }
    }

    @Override
    public void complete() {
        try {
            for (PluginInitializerListener pluginInitializerListener : pluginInitializerListeners) {
                pluginInitializerListener.complete();
            }
        } catch (Exception e){
            log.error("Plugin initializer complete() error", e);
        }
    }

    @Override
    public void failure(Throwable throwable) {
        try {
            for (PluginInitializerListener pluginInitializerListener : pluginInitializerListeners) {
                pluginInitializerListener.failure(throwable);
            }
        } catch (Exception e){
            log.error("Plugin initializer failure() error", e);
        }
    }

    /**
     * 添加监听器
     * @param pluginInitializerListener pluginInitializerListener
     */
    public void addListener(PluginInitializerListener pluginInitializerListener){
        if(pluginInitializerListener != null){
            pluginInitializerListeners.add(pluginInitializerListener);
        }
    }

}