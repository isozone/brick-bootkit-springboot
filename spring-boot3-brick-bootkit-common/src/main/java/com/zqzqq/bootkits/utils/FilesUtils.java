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

package com.zqzqq.bootkits.utils;

import com.zqzqq.bootkits.common.Constants;
import com.zqzqq.bootkits.common.PackageStructure;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 文件工具类
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
public class FilesUtils {

    /**
     * 正斜杠
     */
    public static final String SLASH = "/";

    /**
     * 双正斜杠
     */
    public static final String DOUBLE_SLASH = "//";

    /**
     * 反斜杠
     */
    public static final String  BACKSLASH = "\\";


    /**
     * 获取存在的文件
     *
     * @param pathStr 文件路径
     * @return File
     */
    public static File getExistFile(String pathStr){
        File file = new File(pathStr);
        if(file.exists()){
            return file;
        }
        return null;
    }

    /**
     * 是否存在文件
     * @param path 文件路径
     * @return boolean
     */
    public static boolean existFile(String path){
        if(ObjectUtils.isEmpty(path)){
            return false;
        }
        return new File(path).exists();
    }


    /**
     * 拼接file路径
     *
     * @param paths 拼接的路徑
     * @return 拼接的路徑
     * @since 3.0.0
     */
    public static String joiningFilePath(String ...paths) {
        if (paths == null || paths.length == 0) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        int length = paths.length;
        for (int i = 0; i < length; i++) {
            String path = paths[i];
            if (ObjectUtils.isEmpty(path)) {
                continue;
            }
            if (i < length - 1) {
                if (path.endsWith(SLASH)) {
                    path = path.substring(0, path.lastIndexOf(SLASH));
                } else if (path.endsWith(BACKSLASH)) {
                    path = path.substring(0, path.lastIndexOf(BACKSLASH));
                } else if (path.endsWith(DOUBLE_SLASH)) {
                    path = path.substring(0, path.lastIndexOf(DOUBLE_SLASH));
                }
            }
            if (i > 0) {
                if (path.startsWith(File.separator) || path.startsWith(SLASH) ||
                        path.startsWith(DOUBLE_SLASH) || path.startsWith(BACKSLASH)) {
                    stringBuilder.append(path);
                } else {
                    stringBuilder.append(File.separator).append(path);
                }
            } else {
                stringBuilder.append(path);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * 拼接 zip /jar 路径
     *
     * @param paths 拼接的路徑
     * @return 拼接的路徑
     * @since 3.1.0
     */
    public static String joiningZipPath(String ...paths){
        if(paths == null || paths.length == 0){
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        int length = paths.length;
        for (int i = 0; i < length; i++) {
            String path = paths[i];
            if(ObjectUtils.isEmpty(path)) {
                continue;
            }
            if(i < length - 1){
                if(path.endsWith(SLASH)){
                    path = path.replace(SLASH, "");
                } else if(path.endsWith(BACKSLASH)){
                    path = path.replace(BACKSLASH, "");
                } else if(path.endsWith(DOUBLE_SLASH)){
                    path = path.replace(DOUBLE_SLASH, "");
                }
            }
            if(i > 0){
                if(path.startsWith(PackageStructure.SEPARATOR)){
                    stringBuilder.append(path);
                } else {
                    stringBuilder.append(PackageStructure.SEPARATOR).append(path);
                }
            } else {
                stringBuilder.append(path);
            }
        }

        return stringBuilder.toString();
    }

    public static File createFile(String path) throws IOException {
        try {
            File file = new File(path);
            if(file.exists()){
                return file;
            }
            File parentFile = file.getParentFile();
            if(parentFile != null && !parentFile.exists()){
                if(!parentFile.mkdirs()){
                    throw new IOException("Create " + parentFile + " dir error");
                }
            }
            if(file.createNewFile()){
                return file;
            }
            throw new IOException("Create " + path + " file error");
        } catch (Exception e){
            throw new IOException("Create " + path + " file error", e);
        }
    }

    /**
     * 解决相对路径
     * @param rootPath 根路徑
     * @param relativePath 以~开头的相对路径
     * @return 处理后的路径
     */
    public static String resolveRelativePath(String rootPath, String relativePath){
        if(ObjectUtils.isEmpty(relativePath)){
            return relativePath;
        }
        if(isRelativePath(relativePath)){
            String resolveRelativePath = relativePath.replaceFirst(Constants.RELATIVE_SIGN, "");
            return joiningFilePath(rootPath, resolveRelativePath);
        } else {
            return relativePath;
        }
    }

    /**
     * 解决存在的相对路径
     * @param rootPath 根路徑
     * @param path 以~开头的相对路径或者完整路径
     * @return File 或者 null(不存在)
     */
    public static File resolveExistRelativePathFile(String rootPath, String path){
        if(ObjectUtils.isEmpty(path)){
            return null;
        }
        if(isRelativePath(path)){
            String resolveRelativePath = path.replaceFirst(Constants.RELATIVE_SIGN, "");
            String joiningFilePath = joiningFilePath(rootPath, resolveRelativePath);
            return getExistFile(joiningFilePath);
        } else {
            File existFile = getExistFile(path);
            if(existFile != null){
                return existFile;
            }
            String joiningFilePath = joiningFilePath(rootPath, path);
            return getExistFile(joiningFilePath);
        }
    }

    /**
     * 是否是相对路径
     * @param path 路徑
     * @return true 为相对路径 false 为非相对路径
     */
    public static boolean isRelativePath(String path){
        if(ObjectUtils.isEmpty(path)){
            return false;
        }
        return path.startsWith(Constants.RELATIVE_SIGN);
    }

    /**
     * 判断两个文件是否在同一目录下
     * @param file1 文件1
     * @param file2 文件2
     * @return 所在目录
     */
    public static File sameParent(File file1, File file2){
        if(file1 == null || file2 == null){
            return null;
        }
        File parentFile = file1.getParentFile();
        if(parentFile.equals(file2.getParentFile())){
            return parentFile;
        }
        return null;
    }

    /**
     * 判断某个目录是否存在于根目录下
     * @param rootPath 根目录
     * @param comparePath 比较的目录
     * @return boolean
     */
    public static boolean includePath(Path rootPath, Path comparePath){
        if(rootPath == null || comparePath == null){
            return false;
        }
        return comparePath.toString().startsWith(rootPath.toString());
    }

    /**
     * 是否为子文件
     * @param rootFile rootFile
     * @param childFile 子文件
     * @return boolean
     */
    public static boolean isChildFile(List<String> rootFile, File childFile){
        if(ObjectUtils.isEmpty(rootFile) || childFile == null || !childFile.exists()){
            return false;
        }
        for (String fileStr : rootFile) {
            File file = new File(fileStr);
            if(!file.exists()){
                continue;
            }
            File[] files = file.listFiles();
            if(files == null){
                continue;
            }
            for (File f : files) {
                if(f.equals(childFile)){
                    return true;
                }
            }
        }
        return false;
    }
}
