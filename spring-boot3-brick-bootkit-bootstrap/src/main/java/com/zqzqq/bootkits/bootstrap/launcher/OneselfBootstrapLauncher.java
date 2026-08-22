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

import com.zqzqq.bootkits.bootstrap.DefaultSpringPluginHook;
import com.zqzqq.bootkits.bootstrap.PluginOneselfSpringApplication;
import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;
import com.zqzqq.bootkits.bootstrap.processor.DefaultProcessorContext;
import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.bootstrap.processor.SpringPluginProcessor;
import com.zqzqq.bootkits.core.launcher.plugin.PluginInteractive;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import lombok.AllArgsConstructor;
import org.springframework.boot.SpringApplication;

/**
 * 插件独自运行配置
 *
 * @author starBlues
 * @version 3.1.0
 * @since 3.0.4
 */
@AllArgsConstructor
public class OneselfBootstrapLauncher implements BootstrapLauncher{

    private final SpringPluginBootstrap bootstrap;
    private final SpringPluginProcessor pluginProcessor;
    private final PluginInteractive pluginInteractive;


    @Override
    public SpringPluginHook launch(Class<?>[] primarySources, String[] args) {
        ProcessorContext.RunMode runMode = bootstrap.getRunMode();

        ProcessorContext processorContext = new DefaultProcessorContext(
                runMode, bootstrap, pluginInteractive, bootstrap.getClass()
        );
        SpringApplication springApplication = new PluginOneselfSpringApplication(
                pluginProcessor,
                processorContext,
                primarySources);
        springApplication.run(args);
        return new DefaultSpringPluginHook(pluginProcessor, processorContext);
    }



}

