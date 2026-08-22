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


package com.zqzqq.bootkits.core.migration;

import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.utils.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Migration metadata parsed from plugin descriptor properties.
 */
public final class PluginMigrationPlan {

    public static final String KEY_UP = "plugin.migration.up";
    public static final String KEY_DOWN = "plugin.migration.down";
    public static final String KEY_DATASOURCE = "plugin.migration.datasource";

    private final String pluginId;
    private final String pluginVersion;
    private final List<String> upScripts;
    private final List<String> downScripts;
    private final String dataSourceRef;

    private PluginMigrationPlan(String pluginId,
                                String pluginVersion,
                                List<String> upScripts,
                                List<String> downScripts,
                                String dataSourceRef) {
        this.pluginId = pluginId;
        this.pluginVersion = pluginVersion;
        this.upScripts = upScripts;
        this.downScripts = downScripts;
        this.dataSourceRef = dataSourceRef;
    }

    public static PluginMigrationPlan from(InsidePluginDescriptor descriptor) {
        if (descriptor == null) {
            return new PluginMigrationPlan(null, null,
                    Collections.emptyList(), Collections.emptyList(), "main");
        }
        Properties properties = descriptor.getProperties();
        if (properties == null) {
            return new PluginMigrationPlan(descriptor.getPluginId(), descriptor.getPluginVersion(),
                    Collections.emptyList(), Collections.emptyList(), "main");
        }
        List<String> up = splitCsv(properties.getProperty(KEY_UP));
        List<String> down = splitCsv(properties.getProperty(KEY_DOWN));
        String ds = properties.getProperty(KEY_DATASOURCE);
        if (ObjectUtils.isEmpty(ds)) {
            ds = "main";
        }
        return new PluginMigrationPlan(descriptor.getPluginId(), descriptor.getPluginVersion(),
                up, down, ds.trim());
    }

    private static List<String> splitCsv(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return Collections.emptyList();
        }
        String[] parts = value.split(",");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (ObjectUtils.isEmpty(part)) {
                continue;
            }
            String path = part.trim();
            if (!path.isEmpty()) {
                result.add(path);
            }
        }
        return result;
    }

    public boolean hasMigrations() {
        return !upScripts.isEmpty();
    }

    public String getPluginId() {
        return pluginId;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public List<String> getUpScripts() {
        return upScripts;
    }

    public List<String> getDownScripts() {
        return downScripts;
    }

    public String getDataSourceRef() {
        return dataSourceRef;
    }
}
