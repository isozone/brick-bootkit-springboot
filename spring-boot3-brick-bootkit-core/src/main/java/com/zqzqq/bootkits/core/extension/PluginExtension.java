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

/**
 * 插件扩展点接口。
 * <p>
 * 通过 Java SPI 机制加载第三方扩展实现。
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 4.0.4
 */
public interface PluginExtension {

    /**
     * 获取扩展ID。
     *
     * @return 扩展唯一标识
     */
    String getExtensionId();

    /**
     * 获取扩展名称。
     *
     * @return 扩展名称
     */
    String getExtensionName();

    /**
     * 获取扩展版本。
     *
     * @return 扩展版本
     */
    String getVersion();

    /**
     * 扩展初始化。
     *
     * @param context 扩展上下文
     */
    void init(ExtensionContext context);

    /**
     * 扩展销毁。
     */
    void destroy();
}
