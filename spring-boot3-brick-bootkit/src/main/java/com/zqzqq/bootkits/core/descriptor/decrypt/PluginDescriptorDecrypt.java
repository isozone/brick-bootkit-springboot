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

package com.zqzqq.bootkits.core.descriptor.decrypt;

import com.zqzqq.bootkits.core.exception.PluginDecryptException;

import java.util.Properties;

/**
 * 插件描述文件解密器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.1
 */
public interface PluginDescriptorDecrypt {

    /**
     * 解密 properties
     *
     * @param pluginId 插件id
     * @param properties properties
     * @return 解密后的 Properties
     * @throws PluginDecryptException 插件解密异常
     */
    Properties decrypt(String pluginId, Properties properties) throws PluginDecryptException;

}

