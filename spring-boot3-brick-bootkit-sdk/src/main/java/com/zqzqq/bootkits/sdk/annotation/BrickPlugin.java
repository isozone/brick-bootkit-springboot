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


package com.zqzqq.bootkits.sdk.annotation;

import java.lang.annotation.*;

/**
 * 插件注解
 * 标记一个类为 brick-bootkit 插件
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrickPlugin {

    /**
     * 插件唯一标识
     * 建议使用反向域名格式，如 com.example.myplugin
     */
    String id();

    /**
     * 插件名称
     */
    String name();

    /**
     * 插件描述
     */
    String description() default "";

    /**
     * 插件作者
     */
    String author() default "";

    /**
     * 插件版本
     */
    String version() default "1.0.0";

    /**
     * 插件依赖的其他插件 ID 列表
     */
    String[] dependsOn() default {};

    /**
     * 插件需要的最小框架版本
     */
    String minFrameworkVersion() default "4.0.0";

    /**
     * 插件是否需要 Spring 环境
     */
    boolean requiresSpring() default false;
}
