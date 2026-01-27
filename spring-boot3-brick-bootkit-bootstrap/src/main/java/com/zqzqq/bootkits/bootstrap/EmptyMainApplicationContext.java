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

import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.spring.SpringBeanFactory;
import com.zqzqq.bootkits.spring.environment.EmptyEnvironmentProvider;
import com.zqzqq.bootkits.spring.environment.EnvironmentProvider;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Collections;
import java.util.Map;

/**
 * 空的MainApplicationContext实现
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class EmptyMainApplicationContext implements MainApplicationContext {

    private final SpringBeanFactory springBeanFactory = new EmptySpringBeanFactory();

    @Override
    public SpringBeanFactory getSpringBeanFactory() {
        return springBeanFactory;
    }

    @Override
    public Object getSourceBeanFactory() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Map<String, Map<String, Object>> getConfigurableEnvironment() {
        return Collections.emptyMap();
    }

    @Override
    public EnvironmentProvider getEnvironmentProvider() {
        return new EmptyEnvironmentProvider();
    }

    @Override
    public String[] getActiveProfiles() {
        return new String[0];
    }

    @Override
    public String[] getDefaultProfiles() {
        return new String[0];
    }

    @Override
    public Object resolveDependency(String requestingBeanName, Class<?> dependencyType) {
        return null;
    }

    @Override
    public boolean isWebEnvironment() {
        return false;
    }

    @Override
    public Object getSourceApplicationContext() {
        return null;
    }

    @Override
    public boolean isRegisterController() {
        return false;
    }

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return null;
    }

    @Override
    public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
        return null;
    }

}

