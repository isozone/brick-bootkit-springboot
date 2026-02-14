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

package com.zqzqq.bootkits.loader.utils;

import com.zqzqq.bootkits.loader.LoaderConstant;

import java.io.File;
import java.io.IOException;

/**
 * 文件工具类
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.2
 */
public class FilesUtils {

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
     * 拼接file路径
     *
     * @param paths 拼接的路径
     * @return 拼接的路径
     */
    public static String joiningFilePath(String ...paths){
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
            if(i > 0){
                if(path.startsWith(File.separator) || path.startsWith("/") ||
                        path.startsWith("\\") || path.startsWith("//")){
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
     * @param rootPath 绝对路径
     * @param relativePath 以/开头的相对路径
     * @return 处理后的路径
     */
    public static String resolveRelativePath(String rootPath, String relativePath){
        if(ObjectUtils.isEmpty(relativePath)){
            return relativePath;
        }
        if(isRelativePath(relativePath)){
            return joiningFilePath(rootPath, relativePath.replaceFirst(LoaderConstant.RELATIVE_SIGN, ""));
        } else {
            return relativePath;
        }
    }

    /**
     * 是否是相对路径
     * @param path 路径
     * @return true 为相对路径 false 为非相对路径
     */
    public static boolean isRelativePath(String path){
        if(ObjectUtils.isEmpty(path)){
            return false;
        }
        return path.startsWith(LoaderConstant.RELATIVE_SIGN);
    }

}
