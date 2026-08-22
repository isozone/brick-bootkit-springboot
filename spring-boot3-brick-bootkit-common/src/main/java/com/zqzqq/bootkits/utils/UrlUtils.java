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



package com.zqzqq.bootkits.utils;

/**
 * http url util
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.3
 */
public class UrlUtils {

    private UrlUtils(){}

    public static final String PATH_SEPARATOR = "/";
    public static final char PATH_SEPARATOR_CHAR = '/';

    public final static String SEPARATOR_DOT = ".";
    public final static String SEPARATOR_BACKSLASH = "\\";

    /**
     * rest接口拼接路径
     *
     * @param path1 路径1
     * @param path2 路径2
     * @return 拼接的路径
     * @since 3.0.0
     */
    public static String restJoiningPath(String path1, String path2){
        if(path1 != null && path2 != null){
            if(path1.endsWith(PATH_SEPARATOR) && path2.startsWith(PATH_SEPARATOR)){
                return path1 + path2.substring(1);
            } else if(!path1.endsWith(PATH_SEPARATOR) && !path2.startsWith(PATH_SEPARATOR)){
                return path1 + PATH_SEPARATOR + path2;
            } else {
                return path1 + path2;
            }
        } else if(path1 != null){
            return path1;
        } else if(path2 != null){
            return path2;
        } else {
            return "";
        }
    }


    /**
     * 拼接url路径
     *
     * @param paths 拼接的路径
     * @return 拼接的路径
     * @since 3.0.0
     */
    public static String joiningUrlPath(String ...paths){
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
            if((i < length - 1) && path.endsWith(PATH_SEPARATOR)){
                path = path.substring(path.lastIndexOf(PATH_SEPARATOR));
            }
            if(path.startsWith(PATH_SEPARATOR)){
                stringBuilder.append(path);
            } else {
                stringBuilder.append(PATH_SEPARATOR).append(path);
            }
        }

        return stringBuilder.toString();
    }

    /**
     * 格式化url
     * @param url 原始url
     * @return 格式化后的url
     * @since 3.0.0
     */
    public static String format(String url){
        if(ObjectUtils.isEmpty(url)){
            return url;
        }
        // 热路径优化: Spring 传入的 requestPath 通常已是归一化形式
        // (无连续分隔符、不以分隔符开头), 此时直接返回, 避免 split
        if (isAlreadyNormalized(url)) {
            return url;
        }
        String[] split = url.split(PATH_SEPARATOR);
        StringBuilder stringBuilder = new StringBuilder();
        int length = split.length;
        for (int i = 0; i < length; i++) {
            String str = split[i];
            if(ObjectUtils.isEmpty(str)){
                continue;
            }
            if(i < length - 1){
                stringBuilder.append(str).append(PATH_SEPARATOR);
            } else {
                stringBuilder.append(str);
            }
        }
        return stringBuilder.toString();
    }

    /**
     * 判断 url 是否已经是归一化形式: 不以分隔符开头, 且不含连续分隔符
     * @param url 待检测的 url, 调用方保证非空
     * @return 如果已经归一化返回 true
     * @since 3.0.3
     */
    private static boolean isAlreadyNormalized(String url){
        int length = url.length();
        if(length == 0){
            return true;
        }
        // 不以 '/' 开头
        if(url.charAt(0) == PATH_SEPARATOR_CHAR){
            return false;
        }
        // 不含连续 '/'
        for(int i = 1; i < length; i++){
            if(url.charAt(i) == PATH_SEPARATOR_CHAR && url.charAt(i - 1) == PATH_SEPARATOR_CHAR){
                return false;
            }
        }
        return true;
    }

    /**
     * 格式化匹配url时的格式
     * @param url url
     * @return 格式化后
     * @since 3.0.3
     */
    public static String formatMatchUrl(String url){
        if(url.contains(SEPARATOR_DOT)){
            url = url.replace(SEPARATOR_DOT, PATH_SEPARATOR);
        }
        if(url.contains(SEPARATOR_BACKSLASH)){
            url = url.replace(SEPARATOR_BACKSLASH, PATH_SEPARATOR);
        }
        if(url.startsWith(PATH_SEPARATOR)){
            url = url.substring(url.indexOf(PATH_SEPARATOR) + 1);
        }
        return url;
    }

}