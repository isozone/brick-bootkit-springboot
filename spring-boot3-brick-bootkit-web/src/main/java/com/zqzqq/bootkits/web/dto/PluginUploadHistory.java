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


package com.zqzqq.bootkits.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 插件上传历史记录
 * 
 * @author brick-bootkit
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginUploadHistory implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 上传ID（唯一标识）
     */
    private String uploadId;
    
    /**
     * 插件ID
     */
    private String pluginId;
    
    /**
     * 插件名称
     */
    private String pluginName;
    
    /**
     * 版本号
     */
    private String version;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 上传状态
     */
    private UploadStatus status;
    
    /**
     * 文件路径
     */
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 是否自动启动
     */
    private Boolean autoStart;
    
    /**
     * 备份路径
     */
    private String backupPath;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 上传状态枚举
     */
    public enum UploadStatus {
        /**
         * 上传成功
         */
        SUCCESS,
        /**
         * 上传失败
         */
        FAILED
    }
}