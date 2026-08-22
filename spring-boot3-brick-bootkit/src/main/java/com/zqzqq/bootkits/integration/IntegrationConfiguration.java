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



package com.zqzqq.bootkits.integration;


import com.zqzqq.bootkits.common.Constants;
import com.zqzqq.bootkits.core.admission.PluginAdmissionMode;
import com.zqzqq.bootkits.core.RuntimeMode;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutMode;
import com.zqzqq.bootkits.integration.decrypt.DecryptConfiguration;
import com.zqzqq.bootkits.utils.ObjectUtils;

import java.util.List;
import java.util.Set;


/**
 * 插件集成时的配置接口。插件集成的配置接口
 * @author starBlues
 * @version 3.0.1
 */
public interface IntegrationConfiguration {

    /**
     * 是否启用该插件框架?
     * @return true 启用, false 禁用
     */
    Boolean enable();

    /**
     * 运行环境。运行项目时的模式。分为开发环境(Dev)、生产环境(Prod)
     * @return RuntimeMode.DEV、RuntimeMode.PROD
     */
    RuntimeMode environment();

    /**
     * 主程序包包含?
     * @return String
     */
    String mainPackage();

    /**
     * 插件的路径。可设置多个插件路径
     * 开发环境建议直接配置插件项目父目录，例如：plugins。
     * 若主程序启动时插件未被加载，请检查该配置。
     * @return 插件路径
     */
    List<String> pluginPath();

    /**
     * 上传插件临时存储的临时路径。默认: temp(相对于主程序jar路径)
     * @return 上传插件的临时保存路径
     */
    String uploadTempPath();

    /**
     * 插件备份路径。默认: backupPlugin (相对于主程序jar路径)
     * @return 插件备份路径
     */
    String backupPath();

    /**
     * 插件 RestController 统一请求的路径前缀
     * @return path
     */
    String pluginRestPathPrefix();

    /**
     * 是否启用插件id作为RestController的路径前缀?
     * 如果启用。则路径前缀在pluginRestPathPrefix() 返回的路径拼接插件id,
     * 即为: /pathPrefix/pluginId/**
     * @return boolean
     */
    Boolean enablePluginIdRestPathPrefix();

    /**
     * 启用的插件id
     * @return Set
     */
    Set<String> enablePluginIds();

    /**
     * 禁用的插件id, 禁用后系统不会启动该插件
     * 如果禁用所有插件。则在Set集合中返回返回一个字符串 *
     * @return Set
     */
    Set<String> disablePluginIds();

    /**
     * 设置初始化时插件启动的顺序
     * @return 有顺序的插件id
     */
    List<String> sortInitPluginIds();

    /**
     * 当前主程序包的版本可 用于检验插件是否可安装
     * 插件中可通过插件配置信息 requires 来指定可安装的主程序版本
     * @return 系统版本可 如果因为为空或者0.0.0 表示不校验
     */
    String version();

    /**
     * 设置为true表示插件设置的requires的版本号完全匹配version版本才允许插件安装。 即 [requires]=[x.y.z]
     * 设置为false表示插件设置的requires的版本号小于等于version值 插件就可安装, 即[requires]小于等于[x.y.z]
     * 默认为false
     * @return true or false
     */
    Boolean exactVersion();

    /**
     * 是否扫描插件 swagger 接口
     * @return true 启动, false 禁用。默认启用
     */
    Boolean pluginSwaggerScan();

    /**
     * 插件的配置文件Profile 是否跟随主程序包的Profile配置自动切换?
     * @return true: 跟随, false: 不跟随
     */
    Boolean pluginFollowProfile();

    /**
     * 插件日志打印是否跟随主程序包?
     * @return true: 跟随, false: 不跟随
     */
    Boolean pluginFollowLog();

    /**
     * 解密配置
     * @return DecryptConfiguration
     */
    DecryptConfiguration decrypt();


    /**
     * 检查配置
     */
    default void checkConfig(){};


    /**
     * 是否是开发环境?
     * @return boolean
     */
    default boolean isDev(){
        return environment() == RuntimeMode.DEV;
    }

    /**
     * 是否是生产环境?
     * @return boolean
     */
    default boolean isProd(){
        return environment() == RuntimeMode.PROD;
    }


    /**
     * 是否启用
     * @param pluginId 插件id
     * @return true: 启用, false: 未启动
     */
    default boolean isEnable(String pluginId){
        if(ObjectUtils.isEmpty(enablePluginIds())){
            return true;
        }
        if(isDisabled(pluginId)){
            return false;
        }
        return enablePluginIds().contains(pluginId);
    }


    /**
     * 是否禁用
     * @param pluginId 插件id
     * @return true: 禁用, false: 未禁用
     */
    default boolean isDisabled(String pluginId){
        if(ObjectUtils.isEmpty(disablePluginIds())){
            return false;
        }
        if(disablePluginIds().contains(Constants.DISABLED_ALL_PLUGIN)){
            return true;
        }
        return disablePluginIds().contains(pluginId);
    }

    /**
     * 是否启用集群协同锁。启用后插件生命周期操作会在跨实例间互斥执行。
     * @return true 启用，false 禁用
     */
    default Boolean clusterEnabled() {
        return Boolean.FALSE;
    }

    /**
     * 集群共享目录路径。用于存放跨实例锁文件和状态文件。
     * 支持相对路径（以 ~ 开头），相对当前工作目录解析。
     * @return 共享目录路径
     */
    default String clusterSharedPath() {
        return null;
    }

    /**
     * 集群锁获取超时时间（毫秒）。
     * @return 超时时间
     */
    default Long clusterLockTimeoutMs() {
        return 30000L;
    }

    /**
     * Optional custom cluster lock provider bean name.
     */
    default String clusterLockProviderBeanName() {
        return "";
    }

    /**
     * Admission mode for plugin install/start.
     */
    default PluginAdmissionMode pluginAdmissionMode() {
        return PluginAdmissionMode.WARN;
    }

    /**
     * Whether lifecycle SPI extension manager is enabled.
     */
    default Boolean pluginLifecycleExtensionsEnabled() {
        return Boolean.TRUE;
    }

    /**
     * Migration checksum validation switch.
     */
    default boolean migrationValidateChecksum() {
        return true;
    }

    /**
     * Continue migration when script execution fails.
     */
    default boolean migrationContinueOnError() {
        return false;
    }

    /**
     * Rollout mode for plugin upgrades.
     */
    default PluginRolloutMode pluginRolloutMode() {
        return PluginRolloutMode.DIRECT;
    }

    /**
     * Start new plugin automatically after install/upgrade.
     */
    default boolean pluginRolloutAutoStart() {
        return true;
    }

    /**
     * Rollback to previous plugin package when upgrade fails.
     */
    default boolean pluginRolloutRollbackOnFailure() {
        return true;
    }

}

