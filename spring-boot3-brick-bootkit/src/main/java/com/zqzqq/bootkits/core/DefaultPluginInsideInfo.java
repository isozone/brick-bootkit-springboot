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
 * distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.utils.Assert;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 默认的内部PluginWrapperInside实现
 * @author starBlues
 *
 * @since 3.0.0
 * @version 3.1.1
 */
public class DefaultPluginInsideInfo implements PluginInsideInfo {

    private final String pluginId;
    private final InsidePluginDescriptor pluginDescriptor;
    private EnhancedPluginState pluginState;
    private boolean isFollowInitial = false;

    private Date startTime;
    private Date stopTime;

    private Supplier<Map<String, Object>> extensionInfoSupplier = Collections::emptyMap;

    private ClassLoader classLoader = null;

    public DefaultPluginInsideInfo(InsidePluginDescriptor pluginDescriptor) {
        this.pluginId = pluginDescriptor.getPluginId();
        this.pluginDescriptor = pluginDescriptor;
    }



    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public InsidePluginDescriptor getPluginDescriptor() {
        return pluginDescriptor;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void setPluginState(EnhancedPluginState state) {
        this.pluginState = state;
        
        // 记录时间戳
        if (state == EnhancedPluginState.STARTED) {
            this.startTime = new Date();
        } else if (state == EnhancedPluginState.STOPPED) {
            this.stopTime = new Date();
        }
    }

    @Override
    public EnhancedPluginState getPluginState() {
        return pluginState;
    }

    @Override
    public long getStartTime() {
        return startTime != null ? startTime.getTime() : 0;
    }

    @Override
    public long getStopTime() {
        return stopTime != null ? stopTime.getTime() : 0;
    }

    @Override
    public String getPluginId() {
        return pluginId;
    }

    @Override
    public String getPluginPath() {
        return pluginDescriptor.getPluginPath();
    }

    @Override
    public boolean isFollowSystem() {
        return isFollowInitial;
    }

    @Override
    public Map<String, Object> getExtensionInfo() {
        return extensionInfoSupplier.get();
    }

    @Override
    public void setFollowSystem(boolean follow) {
        this.isFollowInitial = follow;
    }

    public void setExtensionInfoSupplier(Supplier<Map<String, Object>> extensionInfoSupplier) {
        this.extensionInfoSupplier = extensionInfoSupplier != null ? extensionInfoSupplier : Collections::emptyMap;
    }
}
