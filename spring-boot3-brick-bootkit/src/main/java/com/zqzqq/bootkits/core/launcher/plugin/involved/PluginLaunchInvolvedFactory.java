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



package com.zqzqq.bootkits.core.launcher.plugin.involved;

import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.OrderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 插件启动参与工厂
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
public class PluginLaunchInvolvedFactory implements PluginLaunchInvolved{

    private static final Logger logger = LoggerFactory.getLogger(PluginLaunchInvolvedFactory.class);

    private List<PluginLaunchInvolved> pluginLaunchInvolvedList;


    @Override
    public void initialize(GenericApplicationContext applicationContext, IntegrationConfiguration configuration) {
        this.pluginLaunchInvolvedList = getPluginLaunchInvolvedList(applicationContext);
        for (PluginLaunchInvolved pluginLaunchInvolved : pluginLaunchInvolvedList) {
            try {
                pluginLaunchInvolved.initialize(applicationContext, configuration);
            } catch (Exception e){
                logger.error("[{}] execute initialize exception : {}",
                        pluginLaunchInvolved.getClass().getName(), e.getMessage(), e);
            }
        }
    }

    protected List<PluginLaunchInvolved> getPluginLaunchInvolvedList(GenericApplicationContext applicationContext){
        List<PluginLaunchInvolved> pluginLaunchInvolvedList = getDefaultPluginLaunchInvolved();
        if(pluginLaunchInvolvedList == null){
            pluginLaunchInvolvedList = new ArrayList<>();
        }
        Map<String, PluginLaunchInvolved> pluginLaunchInvolvedMap = applicationContext.getBeansOfType(PluginLaunchInvolved.class);
        if(!ObjectUtils.isEmpty(pluginLaunchInvolvedMap)){
            pluginLaunchInvolvedList.addAll(pluginLaunchInvolvedMap.values());
        }
        pluginLaunchInvolvedList.sort(OrderUtils.orderPriority(PluginLaunchInvolved::order));
        return pluginLaunchInvolvedList;
    }

    protected List<PluginLaunchInvolved> getDefaultPluginLaunchInvolved(){
        List<PluginLaunchInvolved> defaultPluginLaunchInvolved = new ArrayList<>();
        defaultPluginLaunchInvolved.add(new DefaultPluginLaunchInvolved());
        defaultPluginLaunchInvolved.add(new PluginApplicationContextGetter());
        return defaultPluginLaunchInvolved;
    }

    @Override
    public void before(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader) throws Exception {
        for (PluginLaunchInvolved pluginLaunchInvolved : pluginLaunchInvolvedList) {
            pluginLaunchInvolved.before(pluginInsideInfo, classLoader);
        }
    }

    @Override
    public void after(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader, SpringPluginHook pluginHook) throws Exception {
        for (PluginLaunchInvolved pluginLaunchInvolved : pluginLaunchInvolvedList) {
            pluginLaunchInvolved.after(pluginInsideInfo, classLoader, pluginHook);
        }
    }

    @Override
    public void failure(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader, Throwable throwable) throws Exception {
        for (PluginLaunchInvolved pluginLaunchInvolved : pluginLaunchInvolvedList) {
            try {
                pluginLaunchInvolved.failure(pluginInsideInfo, classLoader, throwable);
            } catch (Exception e){
                logger.error("[{}] execute failure exception : {}",
                        pluginLaunchInvolved.getClass().getName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void close(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader) throws Exception {
        for (PluginLaunchInvolved pluginLaunchInvolved : pluginLaunchInvolvedList) {
            try {
                pluginLaunchInvolved.close(pluginInsideInfo, classLoader);
            } catch (Exception e){
                logger.error("[{}] execute close exception : {}",
                        pluginLaunchInvolved.getClass().getName(), e.getMessage(), e);
            }
        }
    }
}

