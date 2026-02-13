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

package com.zqzqq.bootkits.core.exception;

import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;

/**
 * 插件解密异常
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.1
 */
public class PluginDecryptException extends PluginException{

    public PluginDecryptException() {
        super();
    }

    public PluginDecryptException(String message) {
        super(message);
    }

    public PluginDecryptException(Throwable cause) {
        super(cause);
    }

    public PluginDecryptException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginDecryptException(PluginDescriptor pluginDescriptor, String opType, Throwable cause) {
        super(pluginDescriptor, opType, cause);
    }

    public PluginDecryptException(PluginDescriptor pluginDescriptor, String message) {
        super(pluginDescriptor, message);
    }

    public PluginDecryptException(String pluginId, String opType, Throwable cause) {
        super(pluginId, opType, cause);
    }

    public PluginDecryptException(String pluginId, String message) {
        super(pluginId, message);
    }
}

