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


package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 默认插件信息实现
 */
public class DefaultPluginInfo implements PluginInfo {
    private final InsidePluginDescriptor descriptor;
    private final PluginState pluginState;
    private final long startTime;
    private final long stopTime;
    private final boolean followSystem;
    private final Map<String, Object> extensionInfo;
    private final ClassLoader classLoader;

    public DefaultPluginInfo(InsidePluginDescriptor descriptor) {
        this(descriptor, null, 0L, 0L, false, Collections.emptyMap(), null);
    }

    public DefaultPluginInfo(PluginInfo pluginInfo) {
        this(Objects.requireNonNull(pluginInfo, "pluginInfo").getPluginDescriptor(),
                pluginInfo.getPluginState(),
                pluginInfo.getStartTime(),
                pluginInfo.getStopTime(),
                pluginInfo.isFollowSystem(),
                pluginInfo.getExtensionInfo(),
                pluginInfo.getClassLoader());
    }

    private DefaultPluginInfo(InsidePluginDescriptor descriptor,
                              PluginState pluginState,
                              long startTime,
                              long stopTime,
                              boolean followSystem,
                              Map<String, Object> extensionInfo,
                              ClassLoader classLoader) {
        this.descriptor = Objects.requireNonNull(descriptor);
        this.pluginState = pluginState;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.followSystem = followSystem;
        this.extensionInfo = snapshotExtensionInfo(extensionInfo);
        this.classLoader = classLoader;
    }

    @Override
    public String getPluginId() {
        return descriptor.getPluginId();
    }

    @Override
    public String getPluginPath() {
        return descriptor.getPluginPath();
    }

    @Override
    public PluginState getPluginState() {
        return pluginState;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getStopTime() {
        return stopTime;
    }

    @Override
    public boolean isFollowSystem() {
        return followSystem;
    }

    @Override
    public Map<String, Object> getExtensionInfo() {
        return extensionInfo;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public InsidePluginDescriptor getPluginDescriptor() {
        return descriptor;
    }

    private static Map<String, Object> snapshotExtensionInfo(Map<String, Object> extensionInfo) {
        if (extensionInfo == null || extensionInfo.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(extensionInfo));
    }
}
