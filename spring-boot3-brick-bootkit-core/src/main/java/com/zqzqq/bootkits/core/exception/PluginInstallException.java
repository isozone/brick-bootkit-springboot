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


package com.zqzqq.bootkits.core.exception;

/**
 * 插件安装异常
 */
public class PluginInstallException extends EnhancedPluginException {
    
    public PluginInstallException(PluginErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    public PluginInstallException(PluginErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message);
        initCause(cause);
    }
    
    public static PluginInstallException installFailed(String pluginId, String reason) {
        return new PluginInstallException(PluginErrorCode.INSTALL_FAILED, reason)
                .withPluginId(pluginId);
    }
    
    public static PluginInstallException dependencyFailed(String pluginId, String dependency) {
        return new PluginInstallException(PluginErrorCode.INSTALL_DEPENDENCY_FAILED, 
                "依赖检查失败: " + dependency)
                .withPluginId(pluginId)
                .withContext("dependency", dependency);
    }
    
    public static PluginInstallException invalidPlugin(String pluginId, String reason) {
        return new PluginInstallException(PluginErrorCode.INSTALL_INVALID_PLUGIN, reason)
                .withPluginId(pluginId);
    }
    
    public static PluginInstallException duplicatePlugin(String pluginId) {
        return new PluginInstallException(PluginErrorCode.INSTALL_DUPLICATE_PLUGIN, 
                "插件已存在")
                .withPluginId(pluginId);
    }
    
    public static PluginInstallException versionConflict(String pluginId, String currentVersion, String requiredVersion) {
        return new PluginInstallException(PluginErrorCode.INSTALL_VERSION_CONFLICT, 
                "版本冲突: 当前版本 " + currentVersion + ", 需要版本 " + requiredVersion)
                .withPluginId(pluginId)
                .withContext("currentVersion", currentVersion)
                .withContext("requiredVersion", requiredVersion);
    }
}