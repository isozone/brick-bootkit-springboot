/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.bootstrap;

import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.bootstrap.processor.SpringPluginProcessor;
import com.zqzqq.bootkits.bootstrap.processor.web.thymeleaf.PluginThymeleafProcessor;
import com.zqzqq.bootkits.bootstrap.realize.PluginCloseListener;
import com.zqzqq.bootkits.bootstrap.realize.StopValidator;
import com.zqzqq.bootkits.bootstrap.utils.DestroyUtils;
import com.zqzqq.bootkits.bootstrap.utils.SpringBeanUtils;
import com.zqzqq.bootkits.core.PluginCloseType;
import com.zqzqq.bootkits.core.exception.PluginProhibitStopException;
import com.zqzqq.bootkits.spring.ApplicationContext;
import com.zqzqq.bootkits.spring.ApplicationContextProxy;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import com.zqzqq.bootkits.spring.WebConfig;
import com.zqzqq.bootkits.spring.web.thymeleaf.ThymeleafConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;
import java.util.Map;

/**
 * 默认的插件钩子器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
public class DefaultSpringPluginHook implements SpringPluginHook {

    private static final Logger log = LoggerFactory.getLogger(DefaultSpringPluginHook.class);
    protected final SpringPluginProcessor pluginProcessor;
    protected final ProcessorContext processorContext;
    private final StopValidator stopValidator;

    public DefaultSpringPluginHook(SpringPluginProcessor pluginProcessor,
                                   ProcessorContext processorContext) {
        this.pluginProcessor = pluginProcessor;
        this.processorContext = processorContext;
        this.stopValidator = SpringBeanUtils.getExistBean(processorContext.getApplicationContext(),
                StopValidator.class);
    }

    /**
     * 先校验是否可卸载
     */
    @Override
    public void stopVerify() {
        if(stopValidator == null){
            return;
        }
        try {
            StopValidator.Result result = stopValidator.verify();
            if(result != null && !result.isVerify()){
                throw new PluginProhibitStopException(processorContext.getPluginDescriptor(),
                        result.getMessage());
            }
        } catch (Exception e){
            throw new PluginProhibitStopException(processorContext.getPluginDescriptor(),
                    e.getMessage());
        }
    }


    @Override
    public void close(PluginCloseType closeType) throws Exception{
        try {
            GenericApplicationContext applicationContext = processorContext.getApplicationContext();
            callPluginCloseListener(applicationContext, closeType);
            pluginProcessor.close(processorContext);
            applicationContext.close();
            processorContext.clearRegistryInfo();
            DestroyUtils.destroyAll(null, SpringFactoriesLoader.class, "cache", Map.class);
        } catch (Exception e){
            log.error("Failed to close plugin hook", e);
        } finally {
            SpringPluginBootstrapBinder.remove();
        }
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return new ApplicationContextProxy(processorContext.getApplicationContext().getBeanFactory());
    }

    @Override
    public WebConfig getWebConfig() {
        return processorContext.getWebConfig();
    }

    @Override
    public ThymeleafConfig getThymeleafConfig() {
        return processorContext.getRegistryInfo(PluginThymeleafProcessor.CONFIG_KEY);
    }

    private void callPluginCloseListener(GenericApplicationContext applicationContext, PluginCloseType closeType){
        List<PluginCloseListener> pluginCloseListeners = SpringBeanUtils.getBeans(
                applicationContext, PluginCloseListener.class);
        if(pluginCloseListeners.isEmpty()){
            return;
        }
        for (PluginCloseListener pluginCloseListener : pluginCloseListeners) {
            try {
                pluginCloseListener.close(applicationContext, processorContext.getPluginInfo(), closeType);
            } catch (Exception e){
                log.error("Plugin close listener error for: {}", processorContext.getPluginInfo().getPluginId(), e);
            }
        }
    }

}