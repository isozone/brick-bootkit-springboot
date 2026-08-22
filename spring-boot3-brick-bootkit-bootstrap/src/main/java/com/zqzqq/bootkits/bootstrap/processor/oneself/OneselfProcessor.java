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



package com.zqzqq.bootkits.bootstrap.processor.oneself;

import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.bootstrap.processor.ProcessorException;
import com.zqzqq.bootkits.bootstrap.processor.SpringPluginProcessor;
import com.zqzqq.bootkits.integration.operator.EmptyPluginOperator;
import com.zqzqq.bootkits.integration.user.DefaultPluginUser;
import com.zqzqq.bootkits.spring.extract.DefaultOpExtractFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;

/**
 * 子启动处理器
 *
 * @author starBlues
 * @version 3.1.0
 * @since 3.0.4
 */
public class OneselfProcessor implements SpringPluginProcessor {

    @Override
    public void initialize(ProcessorContext context) throws ProcessorException {
        registerMainBean(context.getApplicationContext());
    }

    @Override
    public ProcessorContext.RunMode runMode() {
        return ProcessorContext.RunMode.ONESELF;
    }

    private void registerMainBean(GenericApplicationContext applicationContext){
        DefaultListableBeanFactory beanFactory = applicationContext.getDefaultListableBeanFactory();
        beanFactory.registerSingleton("extractFactory", new DefaultOpExtractFactory());
        beanFactory.registerSingleton("pluginUser", new DefaultPluginUser(applicationContext));
        beanFactory.registerSingleton("pluginOperator", new EmptyPluginOperator());
    }

}

