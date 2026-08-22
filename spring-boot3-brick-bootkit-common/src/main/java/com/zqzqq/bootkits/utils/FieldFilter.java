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



package com.zqzqq.bootkits.utils;

import java.lang.reflect.Field;

/**
 * 字段过滤接口
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
@FunctionalInterface
public interface FieldFilter {

    /**
     * 过滤
     *
     * @param field 当前字段
     * @return true 允许, false 不可忽略
     */
    boolean filter(Field field);

}