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



package com.zqzqq.bootkits.core.checker;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.exception.PluginException;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合插件检查器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class ComposePluginLauncherChecker implements PluginLauncherChecker {

    private final List<PluginLauncherChecker> pluginCheckers;

    public ComposePluginLauncherChecker() {
        this(new ArrayList<>());
    }

    public ComposePluginLauncherChecker(List<PluginLauncherChecker> pluginCheckers) {
        this.pluginCheckers = pluginCheckers;
    }

    public void add(PluginLauncherChecker pluginChecker){
        if(pluginChecker != null){
            this.pluginCheckers.add(pluginChecker);
        }
    }


    @Override
    public void checkCanStart(PluginInfo pluginInfo) throws PluginException {
        for (PluginLauncherChecker pluginChecker : pluginCheckers) {
            pluginChecker.checkCanStart(pluginInfo);
        }
    }

    @Override
    public void checkCanStop(PluginInfo pluginInfo) throws PluginException {
        for (PluginLauncherChecker pluginChecker : pluginCheckers) {
            pluginChecker.checkCanStop(pluginInfo);
        }
    }

}