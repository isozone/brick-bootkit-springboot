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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ResourceLoader;

/**
 * 插件SpringApplication实现
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
public class PluginSpringApplication extends SpringApplication {

    private final Logger logger = LoggerFactory.getLogger(PluginSpringApplication.class);

    protected final SpringPluginProcessor pluginProcessor;
    protected final ProcessorContext processorContext;

    private final ConfigurePluginEnvironment configurePluginEnvironment;
    private final GenericApplicationContext applicationContext;
    private final ResourceLoader resourceLoader;


    public PluginSpringApplication(SpringPluginProcessor pluginProcessor,
                                   ProcessorContext processorContext,
                                   Class<?>... primarySources) {
        super(primarySources);
        this.pluginProcessor = pluginProcessor;
        this.processorContext = processorContext;
        this.resourceLoader = processorContext.getResourceLoader();
        this.configurePluginEnvironment = new ConfigurePluginEnvironment(processorContext);
        this.applicationContext = getApplicationContext();
        setDefaultPluginConfig();
    }

    protected GenericApplicationContext getApplicationContext(){
        DefaultListableBeanFactory beanFactory = getBeanFactory(processorContext);
        if(processorContext.getMainApplicationContext().isWebEnvironment()){
            return new PluginWebApplicationContext(beanFactory, processorContext);
        } else {
            return new PluginApplicationContext(beanFactory, processorContext);
        }
    }

    protected DefaultListableBeanFactory getBeanFactory(ProcessorContext processorContext){
        return new PluginListableBeanFactory(processorContext);
    }

    public void setDefaultPluginConfig(){
        setResourceLoader(resourceLoader);
        setBannerMode(Banner.Mode.OFF);
        setEnvironment(new StandardEnvironment());
        setWebApplicationType(WebApplicationType.NONE);
        setRegisterShutdownHook(false);
        setLogStartupInfo(false);
    }

    @Override
    protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
        super.configureEnvironment(environment, args);
        configurePluginEnvironment.configureEnvironment(environment, args);
    }

    @Override
    protected void bindToSpringApplication(ConfigurableEnvironment environment) {
        super.bindToSpringApplication(environment);
        configurePluginEnvironment.logProfiles(environment);
    }

    @Override
    protected ConfigurableApplicationContext createApplicationContext() {
        return this.applicationContext;
    }

    @Override
    public ConfigurableApplicationContext run(String... args) {
        try {
            processorContext.setApplicationContext(this.applicationContext);
            PluginContextHolder.initialize(processorContext);
            pluginProcessor.initialize(processorContext);
            return super.run(args);
        } catch (Exception e) {
            pluginProcessor.failure(processorContext);
            logger.debug("启动插件[{}]失败. {}",
                    processorContext.getPluginDescriptor().getPluginId(),
                    e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void refresh(ConfigurableApplicationContext applicationContext) {
        pluginProcessor.refreshBefore(processorContext);
        super.refresh(applicationContext);
        pluginProcessor.refreshAfter(processorContext);
    }

}

