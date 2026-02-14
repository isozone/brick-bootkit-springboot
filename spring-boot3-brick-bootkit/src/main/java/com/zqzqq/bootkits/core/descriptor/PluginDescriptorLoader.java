/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.descriptor;

import com.zqzqq.bootkits.core.exception.PluginException;

import java.nio.file.Path;

/**
 * 插件描述加载器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public interface PluginDescriptorLoader extends AutoCloseable{



    /**
     * 加载 PluginDescriptor
     * @param location 引导配置文件路径
     * @return PluginDescriptor
     * @throws PluginException 加载异常
     */
    InsidePluginDescriptor load(Path location) throws PluginException;


}

