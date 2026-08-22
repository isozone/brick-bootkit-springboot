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


package com.zqzqq.bootkits.core.resource;

import com.zqzqq.bootkits.core.PluginInsideInfo;

/**
 * 资源管理器接口
 */
public interface ResourceManager {
    /**
     * 注册资源
     */
    String register(PluginInsideInfo plugin, Object resource);
    
    /**
     * 增加资源引用计数
     */
    void retain(String resourceId);
    
    /**
     * 减少资源引用计数
     */
    void release(String resourceId);
    
    /**
     * 释放插件所有资源
     */
    void release(PluginInsideInfo plugin);
}
