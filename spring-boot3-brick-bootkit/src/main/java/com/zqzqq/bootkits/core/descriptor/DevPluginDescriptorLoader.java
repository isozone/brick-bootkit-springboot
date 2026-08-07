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
import com.zqzqq.bootkits.common.PackageType;
import com.zqzqq.bootkits.core.descriptor.decrypt.PluginDescriptorDecrypt;
import com.zqzqq.bootkits.utils.FilesUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 开发环境PluginDescriptorLoader 加载器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.2
 */
@Slf4j
public class DevPluginDescriptorLoader extends AbstractPluginDescriptorLoader{
    private static final Logger log = LoggerFactory.getLogger(DevPluginDescriptorLoader.class);


    public DevPluginDescriptorLoader(PluginDescriptorDecrypt pluginDescriptorDecrypt) {
        super(pluginDescriptorDecrypt);
    }

    @Override
    protected PluginMeta getPluginMetaInfo(Path location) throws Exception {
        String pluginMetaPath = null;
        File file = null;
        String[] candidates = new String[] {
                // 标准开发目录
                FilesUtils.joiningFilePath(location.toString(), PackageStructure.PLUGIN_META_NAME),
                FilesUtils.joiningFilePath(location.toString(),
                        PackageStructure.META_INF_NAME,
                        PackageStructure.PLUGIN_META_NAME),
                // 兼容旧目录结构
                FilesUtils.joiningFilePath(location.toString(),
                        "target",
                        PackageStructure.META_INF_NAME,
                        PackageStructure.PLUGIN_META_NAME),
                // 兼容 Maven 默认编译输出目录
                FilesUtils.joiningFilePath(location.toString(),
                        "target",
                        PackageStructure.CLASSES_NAME,
                        PackageStructure.META_INF_NAME,
                        PackageStructure.PLUGIN_META_NAME)
        };
        for (String candidate : candidates) {
            File candidateFile = new File(candidate);
            if (candidateFile.exists()) {
                pluginMetaPath = candidate;
                file = candidateFile;
                break;
            }
        }

        if (file == null || !file.exists()) {
            log.debug("Path: [{}] not exist.", location);
            return null;
        }
        Path path = Paths.get(pluginMetaPath);
        Properties properties = super.getDecryptProperties(Files.newInputStream(path));
        if (properties == null || properties.isEmpty()) {
            log.debug("Load plugin properties is empty from '{}'", path);
            return null;
        }
        return new PluginMeta(PackageType.PLUGIN_PACKAGE_TYPE_DEV, properties);
    }

    @Override
    protected String getLibPath(DefaultInsidePluginDescriptor descriptor, String configPluginLibDir, String index) {
        return index;
    }

    @Override
    protected DefaultInsidePluginDescriptor create(PluginMeta pluginMeta, Path path) throws Exception {
        final DefaultInsidePluginDescriptor descriptor = super.create(pluginMeta, path);
        descriptor.setType(PluginType.DEV);

        // dev 模式下 pluginClassPath 直接沿用 super.create 从 plugin.system.path 读取的值
        // (例如 "target/classes")。此前在此处硬编码 "classes", 在主服务配置
        // plugin.pluginPath 指向插件根目录时, PluginResourceLoaderFactoryProxy.addDirPluginClasspath
        // 会把 insidePluginPath 与 "classes" 拼成不存在的路径, 导致 "插件xxx未发现Classpath" 启动失败。
        String pluginClassPath = descriptor.getPluginClassPath();
        if (ObjectUtils.isEmpty(pluginClassPath)) {
            descriptor.setPluginClassPath(PackageStructure.CLASSES_NAME);
        }

        return descriptor;
    }

}

