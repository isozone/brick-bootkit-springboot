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



package com.zqzqq.bootkits.bootstrap.processor;

import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;
import com.zqzqq.bootkits.bootstrap.annotation.DisablePluginWeb;
import com.zqzqq.bootkits.bootstrap.processor.oneself.OneselfProcessor;
import com.zqzqq.bootkits.bootstrap.processor.web.PluginControllerProcessor;
import com.zqzqq.bootkits.bootstrap.processor.web.PluginInterceptorsProcessor;
import com.zqzqq.bootkits.bootstrap.processor.web.PluginSpringDocControllerProcessor;
import com.zqzqq.bootkits.bootstrap.processor.web.PluginStaticResourceProcessor;
import com.zqzqq.bootkits.bootstrap.processor.web.thymeleaf.PluginThymeleafProcessor;
import com.zqzqq.bootkits.bootstrap.utils.AnnotationUtils;
import com.zqzqq.bootkits.bootstrap.utils.ProcessorUtils;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.OrderPriority;
import com.zqzqq.bootkits.utils.OrderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 组合的处理器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class ComposeSpringPluginProcessor implements SpringPluginProcessor {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ProcessorContext.RunMode runMode;

    private List<SpringPluginProcessor> processors;

    public ComposeSpringPluginProcessor(ProcessorContext.RunMode runMode) {
        this(runMode, null);
    }

    public ComposeSpringPluginProcessor(ProcessorContext.RunMode runMode, List<SpringPluginProcessor> processors) {
        this.runMode = runMode;
        if(!ObjectUtils.isEmpty(processors)){
            this.processors = processors;
        } else {
            this.processors = new ArrayList<>();
        }
    }

    public void addSpringPluginProcessor(SpringPluginProcessor springPluginProcessor){
        if(springPluginProcessor != null){
            this.processors.add(springPluginProcessor);
        }
    }

    @Override
    public void initialize(ProcessorContext context) throws ProcessorException {
        List<SpringPluginProcessor> processors = new ArrayList<>();
        addDefaultProcessors(context, processors);
        addDefaultWebEnvProcessors(context, processors);
        addOneselfProcessors(context, processors);
        processors.addAll(this.processors);
        this.processors = processors.stream()
                .filter(p->{
                    ProcessorContext.RunMode runMode = p.runMode();
                    return runMode == ProcessorContext.RunMode.ALL || runMode == this.runMode;
                })
                .sorted(OrderUtils.orderPriority(SpringPluginProcessor::order))
                .collect(Collectors.toList());
        for (SpringPluginProcessor processor : this.processors) {
            try {
                processor.initialize(context);
            } catch (Throwable e){
                processException(processor, "initialize", e, true);
            }
        }
    }

    @Override
    public void refreshBefore(ProcessorContext context) throws ProcessorException {
        for (SpringPluginProcessor processor : processors) {
            try {
                processor.refreshBefore(context);
            } catch (Throwable e){
                processException(processor, "refreshBefore", e, true);
            }
        }
    }

    @Override
    public void refreshAfter(ProcessorContext context) throws ProcessorException {
        for (SpringPluginProcessor processor : processors) {
            try {
                processor.refreshAfter(context);
            } catch (Throwable e){
                processException(processor, "refreshAfter", e, true);
            }
        }
    }

    @Override
    public void failure(ProcessorContext context) throws ProcessorException {
        for (SpringPluginProcessor processor : processors) {
            try {
                processor.failure(context);
            } catch (Throwable e){
                processException(processor, "failure", e, false);
            }
        }
    }

    @Override
    public void close(ProcessorContext context) throws ProcessorException {
        for (SpringPluginProcessor processor : processors) {
            try {
                processor.close(context);
            } catch (Throwable e){
                processException(processor, "close", e, false);
            }
        }
    }

    @Override
    public OrderPriority order() {
        return OrderPriority.getHighPriority();
    }

    @Override
    public ProcessorContext.RunMode runMode() {
        return ProcessorContext.RunMode.ALL;
    }

    /**
     * 获取默认的处理者
     * @param context ProcessorContext
     * @param processors 处理者容器集
     */
    protected void addDefaultProcessors(ProcessorContext context, List<SpringPluginProcessor> processors){
        processors.add(new FrameDefineBeanProcessor());
        processors.add(new ExtractBeanProcessor());
        processors.add(new InvokeOtherPluginProcessor());
    }

    /**
     * 添加默认web环境处理者
     * @param context ProcessorContext
     * @param processors 处理者容器集
     */
    protected void addDefaultWebEnvProcessors(ProcessorContext context, List<SpringPluginProcessor> processors){
        MainApplicationContext mainApplicationContext = context.getMainApplicationContext();
        if(!mainApplicationContext.isWebEnvironment()){
            // 主程序容器不是Web类型, 则不进行注册
            return;
        }
        SpringPluginBootstrap springPluginBootstrap = context.getSpringPluginBootstrap();
        DisablePluginWeb disablePluginWeb = AnnotationUtils.findAnnotation(springPluginBootstrap.getClass(),
                DisablePluginWeb.class);
        if(disablePluginWeb != null){
            return;
        }
        context.getWebConfig().setEnable(true);
        if(mainApplicationContext.isRegisterController()){
            processors.add(new PluginControllerProcessor());
        }
        processors.add(new PluginInterceptorsProcessor());
        processors.add(new PluginStaticResourceProcessor());
        processors.add(new PluginThymeleafProcessor());
        ProcessorUtils.add(processors, PluginSpringDocControllerProcessor::new);
    }

    /**
     * 添加 Oneself 模式处理者
     * @param context ProcessorContext
     * @param processors 处理者容器集
     */
    private void addOneselfProcessors(ProcessorContext context, List<SpringPluginProcessor> processors) {
        processors.add(new OneselfProcessor());
    }

    private void processException(SpringPluginProcessor processor, String executeType,
                                  Throwable e, boolean isThrow) throws ProcessorException{
        String error =  "Processor[" + processor.getClass().getName() + "] execute[" + executeType + "] failure : "
                + e.getMessage();
        logger.error(error, e);
        if(isThrow){
            throw new ProcessorException(error, e);
        }
    }
}

