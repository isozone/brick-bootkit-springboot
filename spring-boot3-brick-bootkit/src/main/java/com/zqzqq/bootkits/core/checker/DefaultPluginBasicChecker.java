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

import com.zqzqq.bootkits.common.PackageStructure;
import com.zqzqq.bootkits.common.PluginDescriptorKey;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.loader.utils.Assert;


import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 默认的基础检查器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class DefaultPluginBasicChecker implements PluginBasicChecker {


    @Override
    public void checkPath(Path path) throws Exception {
        if(path == null){
            throw new FileNotFoundException("path 文件路径不能为空");
        }
        if(Files.notExists(path)){
            throw new FileNotFoundException("不存在文件:" + path.toString());
        }
    }

    @Override
    public void checkDescriptor(PluginDescriptor descriptor) throws PluginException {
        Assert.isNotNull(descriptor, "PluginDescriptor 不能为空");

        Assert.isNotEmpty(descriptor.getPluginPath(), "pluginPath 不能为空");

        Assert.isNotNull(descriptor.getPluginId(),
                PluginDescriptorKey.PLUGIN_ID + "不能为空");

        Assert.isNotNull(descriptor.getPluginBootstrapClass(),
                PluginDescriptorKey.PLUGIN_BOOTSTRAP_CLASS + "不能为空");

        Assert.isNotNull(descriptor.getPluginVersion(),
                PluginDescriptorKey.PLUGIN_VERSION + "不能为空");

        String illegal = PackageStructure.getIllegal(descriptor.getPluginId());
        if(illegal != null){
            throw new PluginException(descriptor, "插件id不能包含:" + illegal);
        }
        illegal = PackageStructure.getIllegal(descriptor.getPluginVersion());
        if(illegal != null){
            throw new PluginException(descriptor, "插件版本号不能包含:" + illegal);
        }
    }

}