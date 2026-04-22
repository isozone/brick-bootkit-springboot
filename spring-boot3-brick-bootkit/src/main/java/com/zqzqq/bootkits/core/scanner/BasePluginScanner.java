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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(BasePluginScanner.class);

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
            log.warn("PathResolve 为空，无法扫描插件");
            return pluginPaths;
        }
        log.info("开始扫描插件目录，共 {} 个根目录", rootDir.size());
        for (String dir : rootDir) {
            if(ObjectUtils.isEmpty(dir)){
                continue;
            }
            File file = resolvePathToFile(dir);
            log.info("扫描插件目录: {}, 是否存在: {}", file.getAbsolutePath(), file.exists());

            if(!file.exists()){
                log.warn("插件目录不存在，跳过: {}", file.getAbsolutePath());
                continue;
            }
            int beforeCount = pluginPaths.size();
            resolve(file, pluginPaths);
            int foundCount = pluginPaths.size() - beforeCount;
            log.info("在目录 {} 中发现 {} 个插件", file.getAbsolutePath(), foundCount);
        }
        log.info("插件扫描完成，共发现 {} 个插件", pluginPaths.size());
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
        log.trace("正在检查路径: {}", currentPath);
        currentPath = pathResolve.resolve(currentPath);
        if(currentPath != null){
            log.info("发现插件: {}", currentPath);
            pluginPaths.add(currentPath);
        } else {
            log.trace("路径不是插件，尝试递归: {}", currentFile.getAbsolutePath());
            File[] files = currentFile.listFiles();
            if(files == null || files.length == 0){
                log.debug("目录为空: {}", currentFile.getAbsolutePath());
                return;
            }
            log.debug("递归扫描目录: {}, 包含 {} 个文件/目录", currentFile.getAbsolutePath(), files.length);
            for (File file : files) {
                resolve(file, pluginPaths);
            }
        }
    }
}

