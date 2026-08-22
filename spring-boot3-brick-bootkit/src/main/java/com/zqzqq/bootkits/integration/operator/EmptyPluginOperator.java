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


package com.zqzqq.bootkits.integration.operator;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.integration.listener.PluginInitializerListener;
import com.zqzqq.bootkits.integration.operator.upload.UploadParam;

import java.nio.file.Path;
import java.util.List;

/**
 * 缁岀儤鎼锋担婊呮畱 PluginOperator
 *
 * @author starBlues
 * @version 3.1.0
 * @since 3.0.4
 */
public class EmptyPluginOperator implements PluginOperator{
    @Override
    public boolean initPlugins(PluginInitializerListener pluginInitializerListener) throws PluginException {
        return false;
    }

    @Override
    public boolean verify(Path pluginPath) throws PluginException {
        return false;
    }

    @Override
    public PluginInfo parse(Path pluginPath) throws PluginException {
        return null;
    }

    @Override
    public PluginInfo install(Path pluginPath, boolean unpackPlugin) throws PluginException {
        return null;
    }

    @Override
    public void uninstall(String pluginId, boolean isDelete, boolean isBackup) throws PluginException {

    }

    @Override
    public PluginInfo load(Path pluginPath, boolean unpackPlugin) throws PluginException {
        return null;
    }

    @Override
    public boolean unload(String pluginId) throws PluginException {
        return false;
    }

    @Override
    public boolean start(String pluginId) throws PluginException {
        return false;
    }

    @Override
    public boolean stop(String pluginId) throws PluginException {
        return false;
    }

    @Override
    public PluginInfo uploadPlugin(UploadParam uploadParam) throws PluginException {
        return null;
    }

    @Override
    public Path backupPlugin(Path backDirPath, String sign) throws PluginException {
        return null;
    }

    @Override
    public Path backupPlugin(String pluginId, String sign) throws PluginException {
        return null;
    }

    @Override
    public List<PluginInfo> getPluginInfo() {
        return null;
    }

    @Override
    public PluginInfo getPluginInfo(String pluginId) {
        return null;
    }
}

