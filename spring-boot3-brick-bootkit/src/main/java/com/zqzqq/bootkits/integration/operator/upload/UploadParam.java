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



package com.zqzqq.bootkits.integration.operator.upload;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 *
 * 上传插件参数
 * @author starBlues
 * @version 3.0.0
 */
public class UploadParam {

    /**
     * 上传后是否启动插件，默认启动
     */
    private boolean isStartPlugin = true;

    /**
     * 如果存在旧插件，是否备份旧插件
     */
    private boolean isBackOldPlugin = true;

    /**
     * 是否解压插件文件
     * 压缩包必须符合zip压缩包，压缩包不允许出现多级目录，建议将插件和依赖直接压缩为一个zip类型的压缩包
     */
    private boolean isUnpackPlugin = false;

    protected UploadParam(){}

    public static UploadByInputStreamParam byInputStream(String pluginFileName, InputStream inputStream){
        return new UploadByInputStreamParam(pluginFileName, inputStream);
    }

    public static UploadByMultipartFileParam byMultipartFile(MultipartFile pluginMultipartFile){
        return new UploadByMultipartFileParam(pluginMultipartFile);
    }

    /**
     * 设置上传后是否启动插件，默认 true
     * @param isStartPlugin true: 启动, false 不启动
     * @return UploadParam
     */
    public UploadParam setStartPlugin(boolean isStartPlugin) {
        this.isStartPlugin = isStartPlugin;
        return this;
    }

    /**
     * 设置是否备份旧插件，默认: true
     * @param isBackOldPlugin true: 备份, false 不备份
     * @return UploadParam
     */
    public UploadParam setBackOldPlugin(boolean isBackOldPlugin) {
        this.isBackOldPlugin = isBackOldPlugin;
        return this;
    }

    /**
     * 设置是否解压插件. 默认: false
     * @param isUnpackPlugin true: 解压, false 不解压
     * @return UploadParam
     */
    public UploadParam setUnpackPlugin(boolean isUnpackPlugin) {
        this.isUnpackPlugin = isUnpackPlugin;
        return this;
    }

    public boolean isStartPlugin() {
        return isStartPlugin;
    }

    public boolean isBackOldPlugin() {
        return isBackOldPlugin;
    }

    public boolean isUnpackPlugin() {
        return isUnpackPlugin;
    }

}