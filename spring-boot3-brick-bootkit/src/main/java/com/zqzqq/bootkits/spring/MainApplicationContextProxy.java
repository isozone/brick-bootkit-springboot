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



package com.zqzqq.bootkits.spring;

import com.zqzqq.bootkits.spring.environment.EnvironmentProvider;
import com.zqzqq.bootkits.spring.environment.MainSpringBootEnvironmentProvider;
import com.zqzqq.bootkits.utils.SpringBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 主应用程序 ApplicationContext 的实现类
 * @author starBlues
 * @version 3.0.1
 */
@Slf4j
public class MainApplicationContextProxy extends ApplicationContextProxy implements MainApplicationContext{
    private static final Logger log = LoggerFactory.getLogger(MainApplicationContextProxy.class);


    private final static String REQUEST_MAPPING_BANE_NAME = "requestMappingHandlerMapping";
    private final static String REQUEST_MAPPING_ADAPTER_BANE_NAME = "requestMappingHandlerAdapter";

    private final GenericApplicationContext applicationContext;
    private final boolean isWebEnvironment;

    private final boolean isRegisterController;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    public MainApplicationContextProxy(GenericApplicationContext applicationContext) {
        this(applicationContext, null);
    }

    public MainApplicationContextProxy(GenericApplicationContext applicationContext,
                                       AutoCloseable autoCloseable) {
        super(applicationContext.getBeanFactory(), autoCloseable);
        this.applicationContext = applicationContext;
        this.isWebEnvironment = getIsWebEnvironment(applicationContext);
        if(isWebEnvironment){
            this.requestMappingHandlerMapping = SpringBeanUtils.getExistBean(
                    applicationContext, REQUEST_MAPPING_BANE_NAME, RequestMappingHandlerMapping.class);
            this.requestMappingHandlerAdapter = SpringBeanUtils.getExistBean(
                    applicationContext, REQUEST_MAPPING_ADAPTER_BANE_NAME, RequestMappingHandlerAdapter.class);
            if(this.requestMappingHandlerMapping == null || this.requestMappingHandlerAdapter == null){
                log.error("主程序环境异常，插件不能注册 Controller 接口");
                isRegisterController = false;
            } else {
                isRegisterController = true;
            }
        } else {
            this.isRegisterController = false;
            this.requestMappingHandlerMapping = null;
            this.requestMappingHandlerAdapter = null;
        }
    }

    @Override
    public Map<String, Map<String, Object>> getConfigurableEnvironment() {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Map<String, Object>> environmentMap = new LinkedHashMap<>(propertySources.size());
        for (PropertySource<?> propertySource : propertySources) {
            if (!(propertySource instanceof EnumerablePropertySource)) {
                continue;
            }
            EnumerablePropertySource<?> enumerablePropertySource = (EnumerablePropertySource<?>) propertySource;
            String[] propertyNames = enumerablePropertySource.getPropertyNames();
            Map<String, Object> values = new HashMap<>(propertyNames.length);
            for (String propertyName : propertyNames) {
                values.put(propertyName, enumerablePropertySource.getProperty(propertyName));
            }
            if (!values.isEmpty()) {
                environmentMap.put(propertySource.getName(), values);
            }
        }
        return environmentMap;
    }

    @Override
    public EnvironmentProvider getEnvironmentProvider() {
        return new MainSpringBootEnvironmentProvider(applicationContext.getEnvironment());
    }

    @Override
    public String[] getActiveProfiles() {
        return applicationContext.getEnvironment().getActiveProfiles();
    }

    @Override
    public String[] getDefaultProfiles() {
        return applicationContext.getEnvironment().getDefaultProfiles();
    }

    @Override
    public Object resolveDependency(String requestingBeanName, Class<?> dependencyType) {
        try {
            return applicationContext.getBean(dependencyType);
        } catch (Exception e){
            return null;
        }
    }

    @Override
    public boolean isWebEnvironment() {
        return isWebEnvironment;
    }

    @Override
    public Object getSourceApplicationContext() {
        return applicationContext;
    }

    @Override
    public boolean isRegisterController() {
        return isRegisterController;
    }

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return requestMappingHandlerMapping;
    }

    @Override
    public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
        return requestMappingHandlerAdapter;
    }

    @Override
    public Object getSourceBeanFactory() {
        return applicationContext.getBeanFactory();
    }

    private boolean getIsWebEnvironment(GenericApplicationContext applicationContext){
        return applicationContext instanceof AnnotationConfigServletWebServerApplicationContext
                || applicationContext instanceof AnnotationConfigReactiveWebServerApplicationContext;
    }

}

