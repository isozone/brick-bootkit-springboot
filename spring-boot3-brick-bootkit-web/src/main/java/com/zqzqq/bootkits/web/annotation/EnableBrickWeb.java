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


package com.zqzqq.bootkits.web.annotation;

import com.zqzqq.bootkits.web.config.BrickWebAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 Brick Web 管理控制台
 * 可选使用，不使用则需在 application.yml 配置
 * 
 * @author brick-bootkit
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(BrickWebAutoConfiguration.class)
public @interface EnableBrickWeb {
    
    /**
     * 是否启用 UI 界面
     * 如果为 false，则只提供 REST API
     * @return 是否启用 UI
     */
    boolean enableUI() default true;
    
    /**
     * API 前缀
     * @return API 路径前缀
     */
    String apiPrefix() default "/plugins-web/api";
    
    /**
     * 页面路径前缀
     * @return 页面路径前缀
     */
    String pagePrefix() default "/plugins-web";
}
