/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.utils.Assert;

import java.util.Map;
import java.util.function.Supplier;

/**
 * 外部 PluginWrapperFace
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class PluginInfoFace implements PluginInfo {

    private final PluginDescriptor pluginDescriptor;
    private final EnhancedPluginState pluginState;
    private final boolean followSystem;
    private final Supplier<Map<String, Object>> extensionInfoSupplier;
    private final ClassLoader classLoader;

    private final long startTime;
    private final long stopTime;

    public PluginInfoFace(PluginInsideInfo pluginInsideInfo) {
        Assert.isNotNull(pluginInsideInfo, "参数 pluginInsideInfo 不能为空");
        this.pluginDescriptor = pluginInsideInfo.getPluginDescriptor().toPluginDescriptor();
        this.pluginState = pluginInsideInfo.getPluginState();
        this.followSystem = pluginInsideInfo.isFollowSystem();
        this.extensionInfoSupplier = pluginInsideInfo::getExtensionInfo;
        this.startTime = pluginInsideInfo.getStartTime();
        this.stopTime = pluginInsideInfo.getStopTime();
        this.classLoader = pluginInsideInfo.getClassLoader();
    }

    @Override
    public String getPluginId() {
        return pluginDescriptor.getPluginId();
    }

    @Override
    public InsidePluginDescriptor getPluginDescriptor() {
        if (pluginDescriptor == null) {
            return null;
        }
        return pluginDescriptor.toInsidePluginDescriptor();
    }

    @Override
    public String getPluginPath() {
        return pluginDescriptor.getPluginPath();
    }

    @Override
    public EnhancedPluginState getPluginState() {
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
        return extensionInfoSupplier.get();
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }
}

