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



package com.zqzqq.bootkits.bootstrap.launcher;

import com.zqzqq.bootkits.bootstrap.*;
import com.zqzqq.bootkits.bootstrap.annotation.AutowiredType;
import com.zqzqq.bootkits.bootstrap.coexist.CoexistResolveClassLoaderAspect;
import com.zqzqq.bootkits.bootstrap.processor.DefaultProcessorContext;
import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.bootstrap.processor.SpringPluginProcessor;
import com.zqzqq.bootkits.core.launcher.plugin.PluginInteractive;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Coexist 类型启动器
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.0
 * @see com.zqzqq.bootkits.loader.DevelopmentMode#COEXIST
 */
@AllArgsConstructor
public class CoexistBootstrapLauncher implements BootstrapLauncher{

    private final SpringPluginBootstrap bootstrap;
    private final SpringPluginProcessor pluginProcessor;
    private final PluginInteractive pluginInteractive;

    @Override
    public SpringPluginHook launch(Class<?>[] primarySources, String[] args) {
        ProcessorContext processorContext = new DefaultProcessorContext(
                bootstrap.getRunMode(), bootstrap, pluginInteractive, bootstrap.getClass()
        );
        SpringApplication springApplication = new CoexistSpringApplication(
                pluginProcessor,
                processorContext,
                primarySources);
        springApplication.run(args);
        return new DefaultSpringPluginHook(pluginProcessor, processorContext);
    }

    private static class CoexistSpringApplication extends PluginSpringApplication{

        public CoexistSpringApplication(SpringPluginProcessor pluginProcessor, ProcessorContext processorContext, Class<?>... primarySources) {
            super(pluginProcessor, processorContext, primarySources);
        }

        @Override
        protected DefaultListableBeanFactory getBeanFactory(ProcessorContext processorContext) {
            return new CoexistPluginListableBeanFactory(processorContext);
        }

        @Override
        protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
            super.configureEnvironment(environment, args);
        }

        @Override
        protected GenericApplicationContext getApplicationContext() {
            PluginApplicationContext applicationContext = (PluginApplicationContext) super.getApplicationContext();
            applicationContext.register(CoexistResolveClassLoaderAspect.class);
            return applicationContext;
        }
    }

    private static class CoexistPluginListableBeanFactory extends PluginListableBeanFactory{

        public CoexistPluginListableBeanFactory(ProcessorContext processorContext) {
            super(processorContext);
        }

        @Override
        protected AutowiredTypeResolver getAutowiredTypeResolver(ProcessorContext processorContext) {
            return new CoexistAutowiredTypeResolver();
        }
    }

    private static class CoexistAutowiredTypeResolver implements AutowiredTypeResolver{

        @Override
        public AutowiredType.Type resolve(DependencyDescriptor descriptor) {
            AutowiredType autowiredType = descriptor.getAnnotation(AutowiredType.class);
            if(autowiredType != null){
                return autowiredType.value();
            } else {
                return AutowiredType.Type.PLUGIN;
            }
        }
    }

}

