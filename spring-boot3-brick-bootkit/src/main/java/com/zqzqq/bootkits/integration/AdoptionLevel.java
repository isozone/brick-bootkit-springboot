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

/**
 * 渐进式接入级别。
 *
 * <p>存量系统接入插件框架时，最大的顾虑不是"能不能接"，而是"接了之后系统会不会失控"。
 * 因此框架把接入拆成三个可回退的级别，允许宿主先以零影响的方式把框架装进去，
 * 验证通过后再逐级放开。
 *
 * <ul>
 *   <li>{@link #SHADOW} 影子模式：框架完成装配，但不扫描、不加载插件目录。</li>
 *   <li>{@link #OBSERVE} 观察模式：解析并校验插件包，但不自动启动。</li>
 *   <li>{@link #ACTIVE} 全量模式：加载并自动启动，等同于历史默认行为。</li>
 * </ul>
 *
 * @author brick-bootkit contributors
 * @since 4.0.12
 */
public enum AdoptionLevel {

    /** 影子模式：只装配框架 Bean，不触碰插件目录。用于验证依赖兼容性与启动链路。 */
    SHADOW("影子模式"),

    /** 观察模式：解析插件包并做准入校验，但不启动。用于验证插件包格式与依赖。 */
    OBSERVE("观察模式"),

    /** 全量模式：加载并自动启动插件，为框架默认行为。 */
    ACTIVE("全量模式");

    private final String description;

    AdoptionLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
