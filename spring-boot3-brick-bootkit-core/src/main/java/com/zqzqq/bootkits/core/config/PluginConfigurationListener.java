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


package com.zqzqq.bootkits.core.config;

/**
 * 插件配置变更监听器
 * 
 * @author zqzqq
 * @since 4.1.0
 */
public interface PluginConfigurationListener {
    
    /**
     * 配置变更时调用
     * 
     * @param event 配置变更事件
     */
    void onConfigurationChanged(PluginConfigurationChangeEvent event);
    
    /**
     * 获取监听器优先级
     * 数值越小优先级越高
     * 
     * @return 优先级
     */
    default int getPriority() {
        return 0;
    }
    
    /**
     * 检查是否支持指定插件的配置变更
     * 
     * @param pluginId 插件ID
     * @return 是否支持
     */
    default boolean supports(String pluginId) {
        return true;
    }
}