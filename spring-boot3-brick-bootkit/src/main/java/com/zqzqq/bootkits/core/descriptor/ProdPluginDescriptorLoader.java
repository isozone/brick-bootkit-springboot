/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.descriptor;

import com.zqzqq.bootkits.common.PackageStructure;
import com.zqzqq.bootkits.core.descriptor.decrypt.PluginDescriptorDecrypt;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.utils.FilesUtils;
import com.zqzqq.bootkits.utils.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产环境插件描述加载器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.1
 */
public class ProdPluginDescriptorLoader implements PluginDescriptorLoader{

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private PluginDescriptorLoader target;

    private final PluginDescriptorDecrypt pluginDescriptorDecrypt;

    public ProdPluginDescriptorLoader(PluginDescriptorDecrypt pluginDescriptorDecrypt) {
        this.pluginDescriptorDecrypt = pluginDescriptorDecrypt;
    }

    @Override
    public InsidePluginDescriptor load(Path location) throws PluginException {
        if(ResourceUtils.isJarFile(location)){
            target = new ProdPackagePluginDescriptorLoader(pluginDescriptorDecrypt);
            return target.load(location);
        } else if(ResourceUtils.isZipFile(location)){
            target = new ProdPackagePluginDescriptorLoader(pluginDescriptorDecrypt);
            return target.load(location);
        } else if(ResourceUtils.isDirFile(location)){
            // 如果是目录且包含 PLUGIN.META，使用目录插件加载器
            if(isDirPlugin(location)) {
                target = new ProdDirPluginDescriptorLoader(pluginDescriptorDecrypt);
                return target.load(location);
            }
        }
        logger.warn("不能解析文件: {}", location);
        return null;
    }

    /**
     * 检查目录是否是目录插件（包含 META-INF/PLUGIN.META）
     */
    private boolean isDirPlugin(Path path) {
        File pluginMetaFile = new File(FilesUtils.joiningFilePath(
                path.toString(),
                PackageStructure.resolvePath(PackageStructure.PROD_MANIFEST_PATH)
        ));
        return pluginMetaFile.exists() && pluginMetaFile.isFile();
    }

    @Override
    public void close() throws Exception {
        if(target != null) {
            target.close();
        }
    }
}

