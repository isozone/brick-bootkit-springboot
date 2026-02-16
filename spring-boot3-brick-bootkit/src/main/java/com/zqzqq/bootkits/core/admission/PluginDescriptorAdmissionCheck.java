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
