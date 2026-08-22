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
 * 插件异常处理器接口
 */
public interface PluginExceptionHandler {
    /**
     * 处理插件异常
     * @param exception 异常实例
     * @param phase 当前阶段（install/start/stop等）
     */
    void handle(EnhancedPluginException exception, String phase);

    /**
     * 是否可恢复异常?
     */
    boolean isRecoverable(EnhancedPluginException exception);

    /**
     * 获取错误处理建议
     */
    default String getAdvice(EnhancedPluginException exception) {
        return "请检查插件配置和运行时环境";
    }
}