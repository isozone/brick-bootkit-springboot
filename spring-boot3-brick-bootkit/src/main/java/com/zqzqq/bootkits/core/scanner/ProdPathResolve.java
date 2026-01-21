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
import com.zqzqq.bootkits.utils.FilesUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 生产环境目录解析器
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class ProdPathResolve implements PathResolve{

    /**
     * 支持的插件包后缀名
     */
    private static final String PLUGIN_PACKAGE_SUFFIX_JAR = ".jar";
    private static final String PLUGIN_PACKAGE_SUFFIX_ZIP = ".zip";

    private final List<String> pluginPackageSuffixes = new ArrayList<>();

    public ProdPathResolve(){
        // jar包
        addPackageSuffix(PLUGIN_PACKAGE_SUFFIX_JAR);
        // zip包
        addPackageSuffix(PLUGIN_PACKAGE_SUFFIX_ZIP);
    }

    protected void addPackageSuffix(String name){
        if(ObjectUtils.isEmpty(name)){
            return;
        }
        pluginPackageSuffixes.add(name);
    }

    @Override
    public Path resolve(Path path) {
        File file = path.toFile();

        // 1. 如果是 JAR/ZIP 文件，直接返回
        String fileName = file.getName().toLowerCase();
        for (String suffixName : pluginPackageSuffixes) {
            if(fileName.endsWith(suffixName.toLowerCase())){
                return path;
            }
        }

        // 2. 如果是目录且包含 PLUGIN.META，直接作为目录插件返回
        if(file.isDirectory() && isDirPlugin(path)){
            return path;
        }

        // 3. 如果是目录，扫描目录内的 JAR 文件（包括子目录）
        if(file.isDirectory()) {
            File jarFile = findJarInDir(file);
            if(jarFile != null) {
                return jarFile.toPath();
            }
        }

        return null;
    }

    protected boolean isDirPlugin(Path path){
        File file = path.toFile();
        if(file.isFile()){
            return false;
        }

        file = new File(FilesUtils.joiningFilePath(path.toString(), PackageStructure.resolvePath(
                PackageStructure.PROD_MANIFEST_PATH
        )));
        return file.exists() && file.isFile();
    }

    /**
     * 递归查找目录内的 JAR/ZIP 文件
     */
    private File findJarInDir(File dir) {
        if(dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles();
        if(files == null) {
            return null;
        }

        for (File file : files) {
            if(file.isFile()) {
                String name = file.getName().toLowerCase();
                for (String suffix : pluginPackageSuffixes) {
                    if(name.endsWith(suffix.toLowerCase())) {
                        return file;
                    }
                }
            } else if(file.isDirectory()) {
                // 递归检查子目录
                File jarFile = findJarInDir(file);
                if(jarFile != null) {
                    return jarFile;
                }
            }
        }
        return null;
    }
}

