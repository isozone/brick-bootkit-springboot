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
 * 测试用 @BrickServiceReference 注解。
 * <p>
 * 与 SDK 模块中的注解保持同名同包（全名一致），
 * 使 {@code BrickServiceReferenceInjector} 按类型全名匹配时能识别测试字段。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrickServiceReference {

    /**
     * 服务接口
     */
    Class<?> value() default void.class;

    /**
     * 服务版本范围
     */
    String version() default "";

    /**
     * 是否可选（找不到服务时不报错）
     */
    boolean optional() default false;
}
