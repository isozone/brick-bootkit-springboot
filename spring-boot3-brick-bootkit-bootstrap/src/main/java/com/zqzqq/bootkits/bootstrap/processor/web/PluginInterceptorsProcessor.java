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



package com.zqzqq.bootkits.bootstrap.processor.web;

import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.bootstrap.processor.ProcessorException;
import com.zqzqq.bootkits.bootstrap.processor.SpringPluginProcessor;
import com.zqzqq.bootkits.bootstrap.processor.interceptor.PluginInterceptorRegister;
import com.zqzqq.bootkits.bootstrap.processor.interceptor.PluginInterceptorRegistry;
import com.zqzqq.bootkits.utils.SpringBeanUtils;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.utils.ClassUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.PluginConfigUtils;
import com.zqzqq.bootkits.utils.SpringBeanCustomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.handler.WebRequestHandlerInterceptorAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 插件拦截器处理
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class PluginInterceptorsProcessor implements SpringPluginProcessor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final static String INTERCEPTORS = "pluginHandlerInterceptors";

    private List<AbstractHandlerMapping> handlerMappings;


    @Override
    public void initialize(ProcessorContext context) throws ProcessorException {
        MainApplicationContext applicationContext = context.getMainApplicationContext();
        handlerMappings = SpringBeanCustomUtils.getBeans(applicationContext,
                AbstractHandlerMapping.class);
        if(handlerMappings.isEmpty()){
            logger.warn("Not found AbstractHandlerMapping, Plugin interceptor can't use");
        }
    }

    @Override
    public void refreshAfter(ProcessorContext context) throws ProcessorException {
        if(handlerMappings.isEmpty()){
            return;
        }
        List<PluginInterceptorRegister> interceptorRegisters = SpringBeanUtils.getBeans(
                context.getApplicationContext(),
                PluginInterceptorRegister.class);

        Map<AbstractHandlerMapping, List<HandlerInterceptor>> handlerInterceptorMap = new HashMap<>();
        for (AbstractHandlerMapping handlerMapping : handlerMappings) {
            List<HandlerInterceptor> adaptedInterceptors = getAdaptedInterceptors(handlerMapping);
            if(!ObjectUtils.isEmpty(adaptedInterceptors)){
                handlerInterceptorMap.put(handlerMapping, adaptedInterceptors);
            }
        }
        if(handlerInterceptorMap.isEmpty()){
            logger.debug("handlerInterceptorMap is empty");
            return;
        }
        IntegrationConfiguration configuration = context.getConfiguration();
        String pluginId = context.getPluginDescriptor().getPluginId();
        String pluginRestPrefix = PluginConfigUtils.getPluginRestPrefix(configuration, pluginId);


        List<HandlerInterceptor> storeInterceptors = new ArrayList<>();
        for (PluginInterceptorRegister interceptorRegister : interceptorRegisters) {
            PluginInterceptorRegistry interceptorRegistry = new PluginInterceptorRegistry(pluginRestPrefix);
            interceptorRegister.registry(interceptorRegistry);
            List<Object> registryInterceptors = interceptorRegistry.getInterceptors();
            if(registryInterceptors == null || registryInterceptors.isEmpty()){
                continue;
            }
            for (Object interceptor : registryInterceptors) {
                HandlerInterceptor handlerInterceptor = adaptInterceptor(interceptor);
                for (List<HandlerInterceptor> value : handlerInterceptorMap.values()) {
                    value.add(handlerInterceptor);
                }
                storeInterceptors.add(handlerInterceptor);
            }
        }
        context.addRegistryInfo(INTERCEPTORS, storeInterceptors);
    }

    @Override
    public void close(ProcessorContext context) throws ProcessorException {
        if(handlerMappings.isEmpty()){
            return;
        }
        List<HandlerInterceptor> storeInterceptors = context.getRegistryInfo(INTERCEPTORS);
        if(ObjectUtils.isEmpty(storeInterceptors)){
            return;
        }
        for (HandlerInterceptor storeInterceptor : storeInterceptors) {
            for (AbstractHandlerMapping handlerMapping : handlerMappings) {
                List<HandlerInterceptor> adaptedInterceptors = getAdaptedInterceptors(handlerMapping);
                if(!ObjectUtils.isEmpty(adaptedInterceptors)){
                    adaptedInterceptors.remove(storeInterceptor);
                }
            }
        }
    }

    @Override
    public ProcessorContext.RunMode runMode() {
        return ProcessorContext.RunMode.PLUGIN;
    }

    /**
     * 得到拦截器存储器
     * @param handlerMapping AbstractHandlerMapping
     * @return List<HandlerInterceptor>
     */
    private List<HandlerInterceptor> getAdaptedInterceptors(AbstractHandlerMapping handlerMapping){
        try {
            return ClassUtils.getReflectionField(handlerMapping, "adaptedInterceptors", List.class);
        } catch (IllegalAccessException e) {
            logger.error("Can't get 'adaptedInterceptors' from AbstractHandlerMapping, so " +
                    "You can't use HandlerInterceptor. {} ", e.getMessage());
            return null;
        }
    }

    /**
     * 转换拦截器
     * @param interceptor interceptor
     * @return HandlerInterceptor
     */
    private HandlerInterceptor adaptInterceptor(Object interceptor) {
        if (interceptor instanceof HandlerInterceptor) {
            return (HandlerInterceptor) interceptor;
        } else if (interceptor instanceof WebRequestInterceptor) {
            return new WebRequestHandlerInterceptorAdapter((WebRequestInterceptor) interceptor);
        } else {
            throw new IllegalArgumentException("Interceptor type not supported: " + interceptor.getClass().getName());
        }
    }
}

