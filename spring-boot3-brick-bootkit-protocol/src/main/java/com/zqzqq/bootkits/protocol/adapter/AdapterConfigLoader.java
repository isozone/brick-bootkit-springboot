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


package com.zqzqq.bootkits.protocol.adapter;

import java.io.InputStream;

/**
 * 声明式映射加载器：从 {@code brick-adapter.json} 解析出 {@link AdapterConfig}。
 * <p>
 * 本类依赖 Jackson（provided/optional），非 Spring 环境也可使用。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public interface AdapterConfigLoader {

    /**
     * 从 classpath 资源加载。
     *
     * @param resourcePath 资源路径（如 "brick-adapter.json"）
     * @return 解析后的配置
     * @throws IllegalArgumentException 资源不存在或解析失败
     */
    AdapterConfig load(String resourcePath);

    /**
     * 从 InputStream 加载。
     *
     * @param inputStream 输入流
     * @return 解析后的配置
     * @throws IllegalArgumentException 解析失败
     */
    AdapterConfig load(InputStream inputStream);
}