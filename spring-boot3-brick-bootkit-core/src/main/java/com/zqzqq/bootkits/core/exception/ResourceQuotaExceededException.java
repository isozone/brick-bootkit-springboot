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


package com.zqzqq.bootkits.core.exception;

/**
 * 资源配额超限异常
 * 当插件使用的资源超过设定的配额时抛出此异常
 */
public class ResourceQuotaExceededException extends Exception {
    
    private final String errorCode;
    private final String resourceType;
    private final long currentUsage;
    private final long quotaLimit;
    
    public ResourceQuotaExceededException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.resourceType = "UNKNOWN";
        this.currentUsage = 0;
        this.quotaLimit = 0;
    }
    
    public ResourceQuotaExceededException(String errorCode, String resourceType, 
                                       long currentUsage, long quotaLimit, String message) {
        super(message);
        this.errorCode = errorCode;
        this.resourceType = resourceType;
        this.currentUsage = currentUsage;
        this.quotaLimit = quotaLimit;
    }
    
    public ResourceQuotaExceededException(String errorCode, String resourceType, 
                                       long currentUsage, long quotaLimit, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.resourceType = resourceType;
        this.currentUsage = currentUsage;
        this.quotaLimit = quotaLimit;
    }
    
    /**
     * 获取错误代码
     */
    public String getErrorCode() {
        return errorCode;
    }
    
    /**
     * 获取资源类型
     */
    public String getResourceType() {
        return resourceType;
    }
    
    /**
     * 获取当前使用量
     */
    public long getCurrentUsage() {
        return currentUsage;
    }
    
    /**
     * 获取配额限制
     */
    public long getQuotaLimit() {
        return quotaLimit;
    }
    
    /**
     * 检查是否为内存配额超限
     */
    public boolean isMemoryQuotaExceeded() {
        return "PLUGIN_MEMORY_QUOTA_EXCEEDED".equals(errorCode);
    }
    
    /**
     * 检查是否为线程配额超限
     */
    public boolean isThreadQuotaExceeded() {
        return "PLUGIN_THREAD_QUOTA_EXCEEDED".equals(errorCode);
    }
    
    /**
     * 检查是否为CPU配额超限
     */
    public boolean isCpuQuotaExceeded() {
        return "PLUGIN_CPU_QUOTA_EXCEEDED".equals(errorCode);
    }
    
    /**
     * 检查是否为网络配额超限
     */
    public boolean isNetworkQuotaExceeded() {
        return "PLUGIN_NETWORK_QUOTA_EXCEEDED".equals(errorCode);
    }
    
    /**
     * 检查是否为文件描述符配额超限
     */
    public boolean isFileDescriptorQuotaExceeded() {
        return "PLUGIN_FD_QUOTA_EXCEEDED".equals(errorCode);
    }
    
    /**
     * 检查是否为磁盘I/O配额超限
     */
    public boolean isDiskIOQuotaExceeded() {
        return "PLUGIN_DISK_IO_QUOTA_EXCEEDED".equals(errorCode);
    }
}