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


package com.zqzqq.bootkits.core.admission;

import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.utils.ObjectUtils;

/**
 * Basic plugin descriptor sanity check.
 */
public class PluginDescriptorAdmissionCheck implements PluginAdmissionCheck {

    @Override
    public String getName() {
        return "descriptor-sanity";
    }

    @Override
    public PluginAdmissionDecision check(PluginAdmissionContext context) {
        InsidePluginDescriptor descriptor = context.getDescriptor();
        if (descriptor == null) {
            return PluginAdmissionDecision.reject("Plugin descriptor is null");
        }
        if (ObjectUtils.isEmpty(descriptor.getPluginId())) {
            return PluginAdmissionDecision.reject("Plugin id is empty");
        }
        if (ObjectUtils.isEmpty(descriptor.getPluginVersion())) {
            return PluginAdmissionDecision.reject("Plugin version is empty");
        }
        if (ObjectUtils.isEmpty(descriptor.getMainClass())) {
            return PluginAdmissionDecision.reject("Plugin main class is empty");
        }
        return PluginAdmissionDecision.pass("ok");
    }

    public static PluginException rejected(String pluginId, String detail) {
        return new PluginException("Plugin admission rejected: " + pluginId + ", detail=" + detail);
    }
}
