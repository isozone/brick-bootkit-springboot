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

package com.zqzqq.bootkits.integration;

import com.zqzqq.bootkits.core.RuntimeMode;
import com.zqzqq.bootkits.integration.decrypt.DecryptConfiguration;
import com.zqzqq.bootkits.utils.ResourceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Set;

/**
 * 自动集成配置
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
@Configuration
@ConditionalOnMissingBean(IntegrationConfiguration.class)
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "plugin")
@Data
public class AutoIntegrationConfiguration extends DefaultIntegrationConfiguration{

    public static final String ENABLE_KEY = "plugin.enable";

    /**
     * 是否启用插件功能
     * 通过 plugin.enable 配置控制
     */
    private Boolean enable = true;

    /**
     * 运行模式
     * 开发环境: development, dev
     * 生产/部署环境: deployment, prod
     */
    private String runMode = RuntimeMode.DEV.getMode();

    /**
     * 主程序包名
     */
    private String mainPackage = "";

    /**
     * 插件的路径
     */
    private List<String> pluginPath;

    /**
     * 上传的插件所存储的临时目录
     */
    private String uploadTempPath = "";

    /**
     * 在卸载插件后, 备份插件的目录
     */
    private String backupPath = "backupPlugin";

    /**
     * 插件rest接口前缀. 默认: /plugins
     */
    private String pluginRestPathPrefix = "/plugins";

    /**
     * 是否启用插件id作为rest接口前缀，默认启用
     * 如果启用 地址为: /pluginRestPathPrefix/pluginId
     * pluginRestPathPrefix: 为pluginRestPathPrefix的配置
     * pluginId: 为插件id
     */
    private Boolean enablePluginIdRestPathPrefix = true;

    /**
     * 启用的插件id
     */
    private Set<String> enablePluginIds;

    /**
     * 禁用的插件id，禁用后系统不会启动该插件
     * 如果禁用所有插件，则set集合中返回一个字符 '*'
     */
    private Set<String> disablePluginIds;

    /**
     * 设置初始化时插件启动的顺序
     */
    private List<String> sortInitPluginIds;

    /**
     * 当前主程序包版本，用于验证插件是否可安装
     * 插件中可通过插件配置信息 requires 来指定可安装的主程序版本
     * 如果为 0.0.0 的话，表示不校验
     */
    private String version = "0.0.0";

    /**
     * 设置为true表示插件设置的requires的版本号完全匹配version版本后才能允许插件安装: requires=x.y.z
     * 设置为false表示插件设置的requires的版本号小于等于version时插件可安装，即 requires<=x.y.z
     * 默认false
     */
    private Boolean exactVersion = false;

    /**
     * 是否扫描swagger接口
     */
    private Boolean pluginSwaggerScan = true;


    /**
     * 插件的配置文件Profile是否跟随主程序的Profile配置动态切换
     */
    private Boolean pluginFollowProfile = false;

    /**
     * 插件日志打印是否跟随主程序
     */
    private Boolean pluginFollowLog = false;

    /**
     * 对插件启动时进行解密配置。默认不启用
     */
    private DecryptConfiguration decrypt;

    @Override
    public Boolean enable() {
        if(enable == null){
            return true;
        }
        return enable;
    }

    @Override
    public RuntimeMode environment() {
        return RuntimeMode.byName(runMode);
    }

    @Override
    public String mainPackage() {
        return ResourceUtils.replacePackage(mainPackage);
    }

    @Override
    public List<String> pluginPath() {
        if(ObjectUtils.isEmpty(pluginPath)){
            return super.pluginPath();
        }
        return pluginPath;
    }

    @Override
    public String uploadTempPath() {
        if(ObjectUtils.isEmpty(uploadTempPath)){
            return super.uploadTempPath();
        }
        return uploadTempPath;
    }

    @Override
    public String backupPath() {
        if(ObjectUtils.isEmpty(backupPath)){
            return super.backupPath();
        }
        return backupPath;
    }

    @Override
    public String pluginRestPathPrefix() {
        if(pluginRestPathPrefix == null){
            return super.pluginRestPathPrefix();
        } else {
            return pluginRestPathPrefix;
        }
    }

    @Override
    public Boolean enablePluginIdRestPathPrefix() {
        if(enablePluginIdRestPathPrefix == null){
            return super.enablePluginIdRestPathPrefix();
        } else {
            return enablePluginIdRestPathPrefix;
        }
    }

    @Override
    public Set<String> enablePluginIds() {
        return enablePluginIds;
    }

    @Override
    public Set<String> disablePluginIds() {
        return disablePluginIds;
    }

    @Override
    public List<String> sortInitPluginIds() {
        return sortInitPluginIds;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public Boolean exactVersion() {
        return exactVersion;
    }

    @Override
    public Boolean pluginSwaggerScan() {
        if(pluginSwaggerScan == null){
            return super.pluginSwaggerScan();
        }
        return pluginSwaggerScan;
    }

    @Override
    public Boolean pluginFollowProfile() {
        if(pluginFollowProfile == null){
            return super.pluginFollowProfile();
        }
        return pluginFollowProfile;
    }

    @Override
    public Boolean pluginFollowLog() {
        if(pluginFollowLog == null){
            return super.pluginFollowLog();
        }
        return pluginFollowLog;
    }

    @Override
    public DecryptConfiguration decrypt() {
        if(decrypt == null){
            return super.decrypt();
        }
        return decrypt;
    }
}

