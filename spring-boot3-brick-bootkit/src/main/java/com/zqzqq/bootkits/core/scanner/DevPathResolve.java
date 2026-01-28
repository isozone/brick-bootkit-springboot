/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.core.scanner;

import com.zqzqq.bootkits.common.PackageStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 开发环境路径解析器
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class DevPathResolve implements PathResolve{

    private static final Logger log = LoggerFactory.getLogger(DevPathResolve.class);
    private final List<String> devCompilePackageNames = new ArrayList<>();

    public DevPathResolve() {
        addCompilePackageName();
    }

    protected void addCompilePackageName(){
        // 添加插件信息查询目标目录
        devCompilePackageNames.add("target".concat(File.separator).concat(PackageStructure.META_INF_NAME));
    }

    @Override
    public Path resolve(Path path) {
        // 检查是否是有效的插件目录结构
        if (isValidPluginDirectory(path)) {
            return path;
        }

        // 检查是否符合旧的target/META-INF结构
        for (String devCompilePackageName : devCompilePackageNames) {
            String compilePackagePathStr = path.toString() + File.separator + devCompilePackageName;
            Path compilePackagePath = Paths.get(compilePackagePathStr);
            if(Files.exists(compilePackagePath)){
                // 返回 target 目录，让 DevPluginDescriptorLoader 从 target/META-INF/PLUGIN.META 加载
                return path;
            }
        }
        return null;
    }

    /**
     * 检查是否是有效的插件目录
     */
    private boolean isValidPluginDirectory(Path path) {
        try {
            File file = path.toFile();
            if (!file.exists() || !file.isDirectory()) {
                return false;
            }
            
            // 检查是否存在插件描述符文件（manifest.properties或plugin.properties）
            File manifestFile = new File(file, "manifest.properties");
            File pluginFile = new File(file, "plugin.properties");
            
            if (manifestFile.exists() || pluginFile.exists()) {
                return true;
            }
            
            // 检查是否存在META-INF目录和插件描述符
            File metaInfDir = new File(file, "META-INF");
            if (metaInfDir.exists() && metaInfDir.isDirectory()) {
                File metaInfPluginFile = new File(metaInfDir, PackageStructure.PLUGIN_META_NAME);
                if (metaInfPluginFile.exists()) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            log.debug("Error checking valid plugin directory: {}", path, e);
            return false;
        }
    }
}

