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



package com.zqzqq.bootkits.core.launcher.plugin;

import com.zqzqq.bootkits.core.classloader.MainResourcePatternDefiner;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.utils.ObjectUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Define main-resource include/exclude patterns used by plugins.
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public class PluginMainResourcePatternDefiner implements MainResourcePatternDefiner {

    private final InsidePluginDescriptor descriptor;

    public PluginMainResourcePatternDefiner(PluginInteractive pluginInteractive) {
        this.descriptor = pluginInteractive.getPluginDescriptor();
    }

    @Override
    public Set<String> getIncludePatterns() {
        Set<String> includeResourcePatterns = new HashSet<>();
        // Plugin-defined patterns loaded from descriptor.includeMainResourcePatterns.
        Set<String> includeMainResourcePatterns = descriptor.getIncludeMainResourcePatterns();
        if (!ObjectUtils.isEmpty(includeMainResourcePatterns)) {
            includeResourcePatterns.addAll(includeMainResourcePatterns);
        }
        return includeResourcePatterns;
    }

    @Override
    public Set<String> getExcludePatterns() {
        Set<String> excludeResourcePatterns = new HashSet<>();
        Set<String> excludeMainResourcePatterns = descriptor.getExcludeMainResourcePatterns();
        if (!ObjectUtils.isEmpty(excludeMainResourcePatterns)) {
            excludeResourcePatterns.addAll(excludeMainResourcePatterns);
        }
        return excludeResourcePatterns;
    }
}
