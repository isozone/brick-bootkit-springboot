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


/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.extension;

import com.zqzqq.bootkits.core.plugin.PluginManager;

/**
 * 扩展上下文。
 * 提供扩展运行所需的环境信息和服务。
 * <p>
 * 这里暴露的是 core 模块内部的插件管理模型，不是宿主 Spring Boot 集成入口。
 * 宿主项目应优先使用 {@code com.zqzqq.bootkits.core.PluginManager}。
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 4.0.4
 */
public interface ExtensionContext {

    /**
     * 获取插件管理器。
     *
     * @return core 内部插件管理器
     */
    PluginManager getPluginManager();

    /**
     * 获取扩展属性。
     *
     * @param key 属性键
     * @param defaultValue 默认值
     * @return 属性值
     */
    String getProperty(String key, String defaultValue);

    /**
     * 添加扩展生命周期监听器。
     *
     * @param listener 监听器
     */
    void addLifecycleListener(ExtensionLifecycleListener listener);
}
