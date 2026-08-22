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

import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import com.zqzqq.bootkits.core.logging.PluginLogger;
import org.slf4j.Logger;

/**
 * 日志打印工具
 * 
 * @deprecated 推荐使用 {@link PluginLogger} 进行统一的日志记录
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
@Deprecated
public class LogUtils {

    private LogUtils(){}

    /**
     * @deprecated 使用 {@link PluginLogger#info(String, String, String, Object...)} 替代
     */
    @Deprecated
    public static void info(Logger logger, PluginDescriptor pluginDescriptor, String msg){
        logger.info("插件[{}]{}", MsgUtils.getPluginUnique(pluginDescriptor), msg);
    }

    /**
     * @deprecated 使用 {@link PluginLogger} 替代
     */
    @Deprecated
    public static String getMsg(PluginDescriptor pluginDescriptor, String msg, Object... args){
        return "插件[ " + MsgUtils.getPluginUnique(pluginDescriptor) + " ]" + String.format(msg, args);
    }
    
    // 新增方法，使用PluginLogger
    public static void infoWithPluginLogger(PluginDescriptor pluginDescriptor, String operation, String msg, Object... args) {
        PluginLogger logger = PluginLogger.getLogger(LogUtils.class);
        logger.info(MsgUtils.getPluginUnique(pluginDescriptor), operation, msg, args);
    }
    
    public static void warnWithPluginLogger(PluginDescriptor pluginDescriptor, String operation, String msg, Object... args) {
        PluginLogger logger = PluginLogger.getLogger(LogUtils.class);
        logger.warn(MsgUtils.getPluginUnique(pluginDescriptor), operation, msg, args);
    }
    
    public static void errorWithPluginLogger(PluginDescriptor pluginDescriptor, String operation, String msg, Throwable throwable) {
        PluginLogger logger = PluginLogger.getLogger(LogUtils.class);
        logger.error(MsgUtils.getPluginUnique(pluginDescriptor), operation, msg, throwable);
    }

}

