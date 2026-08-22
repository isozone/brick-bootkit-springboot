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


package com.zqzqq.bootkits.core.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认生命周期监听器（审计日志）
 */
public class DefaultLifecycleListener implements PluginLifecycleListener {
    private static final Logger log = LoggerFactory.getLogger(DefaultLifecycleListener.class);

    @Override
    public void onEvent(PluginLifecycleEvent event) {
        log.info("插件状态变更：{}", event);
    }

    @Override
    public int getPriority() {
        return 0; // 默认优先级
    }
}