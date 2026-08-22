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



package com.zqzqq.bootkits.core.security;

/**
 * 插件安全事件监听器
 *
 * @author starBlues
 * @since 4.0.0
 */
public interface PluginSecurityListener {

    /**
     * 安全验证完成事件
     *
     * @param pluginId 插件ID
     * @param result 验证结果
     */
    void onSecurityValidationCompleted(String pluginId, PluginSecurityValidationResult result);

    /**
     * 权限检查事件
     *
     * @param pluginId 插件ID
     * @param permission 权限
     * @param granted 是否授予
     */
    void onPermissionChecked(String pluginId, PluginPermission permission, boolean granted);

    /**
     * 权限违规事件
     *
     * @param pluginId 插件ID
     * @param permission 权限
     * @param reason 违规原因
     */
    void onPermissionViolation(String pluginId, PluginPermission permission, String reason);
}