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



package com.zqzqq.bootkits.bootstrap.listener;

import com.zqzqq.bootkits.bootstrap.PluginWebApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.ResolvableType;

/**
 * 插件Web应用事件监听器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public class PluginApplicationWebEventListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger log = LoggerFactory.getLogger(PluginApplicationWebEventListener.class);
    private final PluginWebApplicationContext applicationContext;

    public PluginApplicationWebEventListener(PluginWebApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent) {
            callWebServerInitializedEvent();
        }
    }

    @SuppressWarnings("all")
    protected void callWebServerInitializedEvent(){
        String[] beanNamesForType = applicationContext.getBeanNamesForType(ResolvableType.forClassWithGenerics(
                ApplicationListener.class, WebServerInitializedEvent.class
        ));
        PluginWebServerInitializedEvent pluginWebServerInitializedEvent =
                new PluginWebServerInitializedEvent(applicationContext);
        for (String beanName : beanNamesForType) {
            try {
                ApplicationListener<WebServerInitializedEvent> applicationListener =
                        (ApplicationListener<WebServerInitializedEvent>) applicationContext.getBean(beanName);
                applicationListener.onApplicationEvent(pluginWebServerInitializedEvent);
            } catch (Exception e){
                log.error("Failed to call web server initialized event for bean: {}", beanName, e);
            }
        }
    }

    public static class PluginWebServerInitializedEvent extends WebServerInitializedEvent{

        private final PluginWebApplicationContext pluginWebApplicationContext;

        protected PluginWebServerInitializedEvent(PluginWebApplicationContext pluginWebApplicationContext) {
            super(pluginWebApplicationContext.getWebServer());
            this.pluginWebApplicationContext = pluginWebApplicationContext;
        }

        @Override
        public WebServerApplicationContext getApplicationContext() {
            return pluginWebApplicationContext;
        }
    }

}