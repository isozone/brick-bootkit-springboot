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

import com.zqzqq.bootkits.common.Constants;
import com.zqzqq.bootkits.utils.FilesUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基础的插件扫描器
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class BasePluginScanner implements PluginScanner{

    private final PathResolve pathResolve;

    /**
     * 根目录路径（jar 所在目录），用于解析 ~ 相对路径
     */
    private final String rootPath;

    /**
     * 默认构造函数
     */
    public BasePluginScanner() {
        this(null, null);
    }

    /**
     * 构造函数
     * @param pathResolve 路径解析器
     * @param rootPath jar 所在目录
     */
    public BasePluginScanner(PathResolve pathResolve, String rootPath) {
        this.pathResolve = pathResolve;
        this.rootPath = rootPath;
    }

    @Override
    public List<Path> scan(List<String> rootDir) {
        if(ObjectUtils.isEmpty(rootDir)){
            return Collections.emptyList();
        }
        List<Path> pluginPaths = new ArrayList<>();
        if(pathResolve == null){
            return pluginPaths;
        }
        for (String dir : rootDir) {
            if(ObjectUtils.isEmpty(dir)){
                continue;
            }
            File file = resolvePathToFile(dir);

            if(!file.exists()){
                continue;
            }
            resolve(file, pluginPaths);
        }
        return pluginPaths;
    }

    /**
     * 解析路径为 File 对象
     * 处理 ~ 相对路径，将其解析为 jar 所在目录
     * @param dir 路径字符串
     * @return File 对象
     */
    protected File resolvePathToFile(String dir) {
        if (FilesUtils.isRelativePath(dir) && rootPath != null) {
            String resolveRelativePath = dir.replaceFirst(Constants.RELATIVE_SIGN, "");
            String joiningPath = FilesUtils.joiningFilePath(rootPath, resolveRelativePath);
            File file = FilesUtils.getExistFile(joiningPath);
            if (file != null) {
                return file;
            }
        }
        return new File(dir);
    }


    protected void resolve(File currentFile, List<Path> pluginPaths){
        if(currentFile == null || !currentFile.exists()){
            return;
        }
        Path currentPath = currentFile.toPath();
        currentPath = pathResolve.resolve(currentPath);
        if(currentPath != null){
            pluginPaths.add(currentPath);
        } else {
            File[] files = currentFile.listFiles();
            if(files == null || files.length == 0){
                return;
            }
            for (File file : files) {
                resolve(file, pluginPaths);
            }
        }
    }
}

