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

package com.zqzqq.bootkits.utils;

import com.zqzqq.bootkits.core.RuntimeMode;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;

/**
 * 插件配置工具类
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class PluginConfigUtils {

    private static final String CONFIG_NAME_SEPARATOR = "-";

    private PluginConfigUtils(){}

    /**
     * 根据椤圭洰杩愯鐜妯″紡鏉ヨ幏鍙栭厤缃枃浠跺悕绉?
     * @param fileName 文件鍚嶇О
     * @param prodSuffix 鐢熶骇鐜鍓嶇紑
     * @param devSuffix 寮€鍙戠幆澧冨墠缂€
     * @param runtimeMode 杩愯妯″紡
     * @return 文件鍚嶇О
     */
    public static FileNamePack getConfigFileName(String fileName,
                                                 String prodSuffix,
                                                 String devSuffix,
                                                 RuntimeMode runtimeMode){
        if(ObjectUtils.isEmpty(fileName)){
            return null;
        }
        String suffix = "";
        if(runtimeMode == RuntimeMode.PROD){
            // 鐢熶骇鐜
            suffix = prodSuffix;
        } else if(runtimeMode == RuntimeMode.DEV){
            // 寮€鍙戠幆澧?
            suffix = devSuffix;
        }

        return new FileNamePack(fileName, suffix);
    }

    public static String joinConfigFileName(FileNamePack fileNamePack){
        if(fileNamePack == null){
            return null;
        }
        return joinConfigFileName(fileNamePack.getSourceFileName(), fileNamePack.getFileSuffix());
    }

    public static String joinConfigFileName(String fileName, String suffix){
        if(ObjectUtils.isEmpty(fileName)){
            return null;
        }
        String fileNamePrefix;
        String fileNamePrefixSuffix;

        if(fileName.lastIndexOf(".") == -1) {
            fileNamePrefix = fileName;
            fileNamePrefixSuffix = "";
        } else {
            int index = fileName.lastIndexOf(".");
            fileNamePrefix = fileName.substring(0, index);
            fileNamePrefixSuffix = fileName.substring(index);
        }
        if(suffix == null){
            suffix = "";
        }
        if(ObjectUtils.isEmpty(suffix) && !suffix.startsWith(CONFIG_NAME_SEPARATOR)){
            suffix = CONFIG_NAME_SEPARATOR + suffix;
        }
        return fileNamePrefix + suffix + fileNamePrefixSuffix;
    }

    /**
     * 得到插件接口鍓嶇紑
     * @param configuration 配置
     * @param pluginId 插件id
     * @return 接口鍓嶇紑
     */
    public static String getPluginRestPrefix(IntegrationConfiguration configuration, String pluginId){
        String pathPrefix = configuration.pluginRestPathPrefix();
        if(configuration.enablePluginIdRestPathPrefix()){
            if(pathPrefix != null && !"".equals(pathPrefix)){
                pathPrefix = UrlUtils.restJoiningPath(pathPrefix, pluginId);
            } else {
                pathPrefix = pluginId;
            }
            return pathPrefix;
        } else {
            if(pathPrefix == null || "".equals(pathPrefix)){
                // 不启用插件ID作为路径前缀，并且路径前缀为空, 则直接返回
                return null;
            }
        }
        return pathPrefix;
    }

    public static class FileNamePack {
        private final String sourceFileName;
        private final String fileSuffix;

        public FileNamePack(String sourceFileName, String fileSuffix) {
            this.sourceFileName = sourceFileName;
            this.fileSuffix = fileSuffix;
        }

        public String getSourceFileName() {
            return sourceFileName;
        }

        public String getFileSuffix() {
            return fileSuffix;
        }
    }

}

