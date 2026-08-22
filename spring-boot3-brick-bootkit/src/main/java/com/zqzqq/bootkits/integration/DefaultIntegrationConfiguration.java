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



package com.zqzqq.bootkits.integration;

import com.zqzqq.bootkits.common.Constants;
import com.zqzqq.bootkits.integration.decrypt.DecryptConfiguration;
import com.zqzqq.bootkits.utils.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 默认的插件集合配置。非必须配置设置了默认值
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
public abstract class DefaultIntegrationConfiguration implements IntegrationConfiguration{

    public static final String DEFAULT_PLUGIN_REST_PATH_PREFIX = "plugins";
    public static final Boolean DEFAULT_ENABLE_PLUGIN_ID_REST_PATH_PREFIX = Boolean.TRUE;

    private static final String DEFAULT_TEMP_FILE =
            new File(System.getProperty("java.io.tmpdir"), "spring-boot3-brick-bootkit-temp").getPath();

    @Override
    public Boolean enable() {
        return Boolean.TRUE;
    }

    @Override
    public List<String> pluginPath() {
        List<String> pluginPath = new ArrayList<>(1);
        pluginPath.add("~/plugins/");
        return pluginPath;
    }

    @Override
    public String uploadTempPath(){
        return DEFAULT_TEMP_FILE;
    }

    @Override
    public String backupPath(){
        return "backupPlugin";
    }

    @Override
    public String pluginRestPathPrefix(){
        return DEFAULT_PLUGIN_REST_PATH_PREFIX;
    }

    @Override
    public Boolean enablePluginIdRestPathPrefix() {
        return DEFAULT_ENABLE_PLUGIN_ID_REST_PATH_PREFIX;
    }

    @Override
    public Set<String> enablePluginIds() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> disablePluginIds() {
        return Collections.emptySet();
    }

    @Override
    public List<String> sortInitPluginIds() {
        return Collections.emptyList();
    }

    @Override
    public String version() {
        return Constants.ALLOW_VERSION;
    }

    @Override
    public Boolean exactVersion() {
        return Boolean.FALSE;
    }

    @Override
    public Boolean pluginSwaggerScan() {
        return Boolean.TRUE;
    }

    @Override
    public Boolean pluginFollowProfile() {
        return Boolean.FALSE;
    }

    @Override
    public Boolean pluginFollowLog() {
        return Boolean.FALSE;
    }

    @Override
    public DecryptConfiguration decrypt() {
        DecryptConfiguration decryptConfiguration = new DecryptConfiguration();
        decryptConfiguration.setEnable(false);
        return decryptConfiguration;
    }

    /**
     * 检查配置
     */
    @Override
    public void checkConfig(){
        Assert.isNotEmpty(mainPackage(), "插件配置: [plugin.mainPackage] 不能为空");
    }

}