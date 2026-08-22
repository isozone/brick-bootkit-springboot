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



package com.zqzqq.bootkits.integration;

import com.zqzqq.bootkits.integration.listener.Knife4jSwaggerListener;
import com.zqzqq.bootkits.spring.ResolvePluginThreadClassLoader;
import com.zqzqq.bootkits.spring.web.PluginStaticResourceConfig;
import com.zqzqq.bootkits.spring.web.PluginStaticResourceWebMvcConfigurer;
import com.zqzqq.bootkits.spring.web.thymeleaf.PluginThymeleafInvolved;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * 系统web环境扩展配置
 * @author starBlues
 * @version 3.0.0
 */
@ConditionalOnWebApplication
@Import({
        ExtendPointWebConfiguration.PluginStaticResourceConfiguration.class,
        ExtendPointWebConfiguration.PluginThymeleafConfiguration.class,
        ExtendPointWebConfiguration.SwaggerListenerConfiguration.class,
})
public class ExtendPointWebConfiguration {


    @ConditionalOnClass(ResourceResolver.class)
    public static class PluginStaticResourceConfiguration{

        @Bean
        @ConditionalOnMissingBean
        public PluginStaticResourceWebMvcConfigurer pluginWebResourceResolver(PluginStaticResourceConfig resourceConfig){
            return new PluginStaticResourceWebMvcConfigurer(resourceConfig);
        }

        @Bean
        @ConditionalOnMissingBean
        public PluginStaticResourceConfig pluginStaticResourceConfig() {
            return new PluginStaticResourceConfig();
        }
    }

    @ConditionalOnClass({ TemplateMode.class, SpringTemplateEngine.class })
    @ConditionalOnProperty(name = "spring.thymeleaf.enabled", havingValue = "true", matchIfMissing = true)
    public static class PluginThymeleafConfiguration{

        @Bean
        @ConditionalOnMissingBean
        public PluginThymeleafInvolved pluginThymeleafInvolved(){
            return new PluginThymeleafInvolved();
        }
    }

    @ConditionalOnClass({ io.swagger.v3.oas.models.OpenAPI.class })
    @ConditionalOnProperty(name = "plugin.pluginSwaggerScan", havingValue = "true", matchIfMissing = true)
    public static class SwaggerListenerConfiguration {

        private final GenericApplicationContext applicationContext;

        public SwaggerListenerConfiguration(GenericApplicationContext applicationContext) {
            this.applicationContext = applicationContext;
        }

        @Bean
        @ConditionalOnMissingBean
        public Knife4jSwaggerListener swaggerListener(){
            return new Knife4jSwaggerListener(applicationContext);
        }

    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(){
        return new ResolvePluginThreadClassLoader();
    }

}

