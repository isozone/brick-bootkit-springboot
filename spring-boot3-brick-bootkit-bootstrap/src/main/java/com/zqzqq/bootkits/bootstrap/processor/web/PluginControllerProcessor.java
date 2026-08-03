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

package com.zqzqq.bootkits.bootstrap.processor.web;

import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.bootstrap.processor.ProcessorException;
import com.zqzqq.bootkits.bootstrap.processor.SpringPluginProcessor;
import com.zqzqq.bootkits.bootstrap.utils.AnnotationUtils;
import com.zqzqq.bootkits.bootstrap.utils.DestroyUtils;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.PluginConfigUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * 插件Controller处理器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class PluginControllerProcessor implements SpringPluginProcessor {

    private final static Logger LOG = LoggerFactory.getLogger(PluginControllerProcessor.class);

    static final String PROCESS_CONTROLLERS = "PROCESS_SUCCESS";


    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    private RequestMappingHandlerAdapter handlerAdapter;

    private final AtomicBoolean canRegistered = new AtomicBoolean(false);


    @Override
    public void initialize(ProcessorContext processorContext) throws ProcessorException {
        MainApplicationContext mainApplicationContext = processorContext.getMainApplicationContext();
        this.requestMappingHandlerMapping = mainApplicationContext.getRequestMappingHandlerMapping();
        this.handlerAdapter = mainApplicationContext.getRequestMappingHandlerAdapter();
        canRegistered.set(true);
    }


    @Override
    public void refreshBefore(ProcessorContext processorContext) throws ProcessorException {
        if(!canRegistered.get()){
            return;
        }
        GenericApplicationContext applicationContext = processorContext.getApplicationContext();
        applicationContext.getDefaultListableBeanFactory()
                .addBeanPostProcessor(new ControllerPostProcessor(processorContext));
    }

    @Override
    public void refreshAfter(ProcessorContext processorContext) throws ProcessorException {
        if(!canRegistered.get()){
            return;
        }
        String pluginId = processorContext.getPluginDescriptor().getPluginId();
        List<ControllerWrapper> controllerWrappers = processorContext.getRegistryInfo(PROCESS_CONTROLLERS);
        if(ObjectUtils.isEmpty(controllerWrappers)){
            return;
        }
        GenericApplicationContext applicationContext = processorContext.getApplicationContext();
        Iterator<ControllerWrapper> iterator = controllerWrappers.iterator();
        String pathPrefix = PluginConfigUtils.getPluginRestPrefix(processorContext.getConfiguration(), pluginId);
        PluginRequestMappingHandlerMapping pluginHandlerMapping = new PluginRequestMappingHandlerMapping(pathPrefix);
        while (iterator.hasNext()){
            ControllerWrapper controllerWrapper = iterator.next();
            if(!applicationContext.containsBean(controllerWrapper.getBeanName())){
                iterator.remove();
            }
            Object controllerBean = applicationContext.getBean(controllerWrapper.getBeanName());
            pluginHandlerMapping.registerHandler(controllerBean);
            List<RegisterMappingInfo> registerMappingInfo = pluginHandlerMapping.getAndClear();
            Set<RequestMappingInfo> requestMappingInfoSet = new HashSet<>(registerMappingInfo.size());
            for (RegisterMappingInfo mappingInfo : registerMappingInfo) {
                RequestMappingInfo requestMappingInfo = mappingInfo.getRequestMappingInfo();
                requestMappingHandlerMapping.registerMapping(
                        requestMappingInfo,
                        mappingInfo.getHandler(),
                        mappingInfo.getMethod()
                );
                requestMappingInfoSet.add(requestMappingInfo);
            }
            controllerWrapper.setRequestMappingInfo(requestMappingInfoSet);
            LOG.info("插件[{}]注册接口数量: {}", pluginId,requestMappingInfoSet.size());
        }


    }

    @Override
    public void close(ProcessorContext context) throws ProcessorException {
        List<ControllerWrapper> controllerWrappers = context.getRegistryInfo(PROCESS_CONTROLLERS);
        if(ObjectUtils.isEmpty(controllerWrappers)){
            return;
        }
        for (ControllerWrapper controllerWrapper : controllerWrappers) {
            unregister(controllerWrapper);
        }
        controllerWrappers.clear();
    }

    @Override
    public ProcessorContext.RunMode runMode() {
        return ProcessorContext.RunMode.PLUGIN;
    }

    /**
     * 卸载具体的Controller操作
     * @param controllerBeanWrapper controllerBean包装
     */
    private void unregister(ControllerWrapper controllerBeanWrapper) {
        Set<RequestMappingInfo> requestMappingInfoSet = controllerBeanWrapper.getRequestMappingInfo();
        if(requestMappingInfoSet != null && !requestMappingInfoSet.isEmpty()){
            for (RequestMappingInfo requestMappingInfo : requestMappingInfoSet) {
                requestMappingHandlerMapping.unregisterMapping(requestMappingInfo);
            }
        }
        if(handlerAdapter != null){
            Class<?> beanClass = controllerBeanWrapper.getBeanClass();
            DestroyUtils.destroyValue(handlerAdapter, "sessionAttributesHandlerCache", beanClass);
            DestroyUtils.destroyValue(handlerAdapter, "initBinderCache", beanClass);
            DestroyUtils.destroyValue(handlerAdapter, "modelAttributeCache", beanClass);
        }
    }

    private static class ControllerPostProcessor implements BeanPostProcessor {

        private final static Logger LOG = LoggerFactory.getLogger(ControllerPostProcessor.class);

        private final ProcessorContext processorContext;

        private ControllerPostProcessor(ProcessorContext processorContext) {
            this.processorContext = processorContext;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            Class<?> aClass = bean.getClass();
            RequestMapping requestMapping = AnnotationUtils.findAnnotation(aClass, RequestMapping.class);
            boolean isController = AnnotationUtils.existOr(aClass, new Class[]{
                Controller.class, RestController.class
            });
            if(requestMapping != null && isController){
                addControllerWrapper(beanName, aClass);
            }
            return bean;
        }

        private void addControllerWrapper(String beanName, Class<?> aClass){
            List<ControllerWrapper> controllerWrappers = processorContext.getRegistryInfo(PROCESS_CONTROLLERS);
            if(controllerWrappers == null){
                controllerWrappers = new ArrayList<>();
                processorContext.addRegistryInfo(PROCESS_CONTROLLERS, controllerWrappers);
            }
            ControllerWrapper controllerWrapper = new ControllerWrapper();
            controllerWrapper.setBeanName(beanName);
            controllerWrapper.setBeanClass(aClass);
            controllerWrappers.add(controllerWrapper);
        }

    }

    @Data
    static class ControllerWrapper{

        /**
         * controller bean 名称
         */
        private String beanName;

        /**
         * controller bean 类型
         */
        private Class<?> beanClass;

        /**
         * controller 的 RequestMappingInfo 集合
         */
        private Set<RequestMappingInfo> requestMappingInfo;
    }


    private static class PluginRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

        private final List<RegisterMappingInfo> registerMappingInfo = new ArrayList<>();

        public PluginRequestMappingHandlerMapping(){
            this(null);
        }

        public PluginRequestMappingHandlerMapping(String pathPrefix){
            if(!ObjectUtils.isEmpty(pathPrefix)){
                Map<String, Predicate<Class<?>>> prefixes = new HashMap<>();
                prefixes.put(pathPrefix, c->true);
                setPathPrefixes(prefixes);
            }
        }

        public void registerHandler(Object handler){
            detectHandlerMethods(handler);
        }

        @Override
        protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
            super.registerHandlerMethod(handler, method, mapping);
            registerMappingInfo.add(new RegisterMappingInfo(handler, method, mapping));
        }

        public List<RegisterMappingInfo> getAndClear(){
            List<RegisterMappingInfo> registerMappingInfo = new ArrayList<>(this.registerMappingInfo);
            this.registerMappingInfo.clear();
            return registerMappingInfo;
        }

    }

    @AllArgsConstructor
    @Getter
    private static class RegisterMappingInfo{
        private final Object handler;
        private final Method method;
        private final RequestMappingInfo requestMappingInfo;
    }

}