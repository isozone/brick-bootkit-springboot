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



package com.zqzqq.bootkits.integration.application;

import com.zqzqq.bootkits.integration.ExtendPointConfiguration;
import com.zqzqq.bootkits.integration.ExtendPointWebConfiguration;
import com.zqzqq.bootkits.integration.listener.PluginInitializerListener;
import com.zqzqq.bootkits.integration.operator.PluginOperator;
import com.zqzqq.bootkits.integration.user.PluginUser;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;

/**
 * 自动初始化的 PluginApplication。该 PluginApplication 基于 Spring 生命周期自动初始化插件
 *
 * @author starBlues
 * @version 3.0.0
 */
@Import({ExtendPointConfiguration.class, ExtendPointWebConfiguration.class})
public class AutoPluginApplication extends DefaultPluginApplication
        implements PluginApplication, ApplicationContextAware, ApplicationListener<ApplicationStartedEvent> {

    private ApplicationContext applicationContext;
    private PluginInitializerListener pluginInitializerListener;


    /**
     * 设置插件初始化监听器
     * @param pluginInitializerListener 插件监听器
     */
    public void setPluginInitializerListener(PluginInitializerListener pluginInitializerListener) {
        this.pluginInitializerListener = pluginInitializerListener;
    }


    @Override
    public void initialize(ApplicationContext applicationContext,
                           PluginInitializerListener listener) {
        // 此处不可手动初始化
        throw new RuntimeException("Cannot be initialized manually");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void setBeanFactory(GenericApplicationContext applicationContext) {
        // 忽略
    }

    /**
     * Spring boot bean属性被Set完成后调用。会自动初始化插件
     */
    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        super.initialize(applicationContext, pluginInitializerListener);
    }

    @Override
    public PluginOperator getPluginOperator() {
        return createPluginOperator(applicationContext);
    }

    @Override
    public PluginUser getPluginUser() {
        return createPluginUser(applicationContext);
    }
}

