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
 * 服务引用注解
 * 自动注入其他插件提供的服务
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrickServiceReference {

    /**
     * 服务接口
     */
    Class<?> value();

    /**
     * 服务版本范围，如 "[1.0,2.0)"
     */
    String version() default "";

    /**
     * 是否可选（找不到服务时不报错）
     */
    boolean optional() default false;
}
