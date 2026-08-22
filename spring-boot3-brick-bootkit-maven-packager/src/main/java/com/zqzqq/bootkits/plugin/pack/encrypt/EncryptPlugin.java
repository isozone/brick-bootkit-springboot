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



package com.zqzqq.bootkits.plugin.pack.encrypt;

import com.zqzqq.bootkits.plugin.pack.PluginInfo;

/**
 * 加密插件
 *
 * @author starBlues
 * @since 3.0.1
 * @version 3.0.1
 */
public interface EncryptPlugin {


    /**
     * 加密
     * @param pluginInfo 当前插件信息
     * @param encryptConfig 加密配置
     * @return 加密后得字符
     * @throws Exception 加密异常
     */
    PluginInfo encrypt(EncryptConfig encryptConfig, PluginInfo pluginInfo) throws Exception;

}