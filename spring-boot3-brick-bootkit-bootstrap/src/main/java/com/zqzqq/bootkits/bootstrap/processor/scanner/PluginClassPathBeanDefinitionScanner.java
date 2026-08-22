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



package com.zqzqq.bootkits.bootstrap.processor.scanner;

import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;


/**
 * 插件自定义classpath bean 扫描
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class PluginClassPathBeanDefinitionScanner extends ClassPathBeanDefinitionScanner {

    public PluginClassPathBeanDefinitionScanner(ProcessorContext processorContext) {
        this(processorContext, true);
    }

    public PluginClassPathBeanDefinitionScanner(ProcessorContext processorContext, boolean useDefaultFilters) {
        super(processorContext.getApplicationContext(), useDefaultFilters,
                processorContext.getApplicationContext().getEnvironment(),
                processorContext.getResourceLoader());
    }

}

