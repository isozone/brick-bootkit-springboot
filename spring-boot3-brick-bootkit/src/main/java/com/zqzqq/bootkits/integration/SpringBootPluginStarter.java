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

import com.zqzqq.bootkits.integration.application.AutoPluginApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * spring-boot-starter
 *
 * @author starBlues
 * @version 3.0.0
 */
@ConditionalOnExpression("${" + AutoIntegrationConfiguration.ENABLE_KEY + ":true}")
@Import(AutoPluginApplication.class)
@AutoConfiguration // 添加杩欎釜注解
@EnableConfigurationProperties(AutoIntegrationConfiguration.class)
public class SpringBootPluginStarter {

}

