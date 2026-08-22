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



package com.zqzqq.bootkits.core.descriptor;

import com.zqzqq.bootkits.common.DependencyPlugin;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * 插件信息
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public interface PluginDescriptor {

    /**
     * 获取插件id
     * @return String
     */
    String getPluginId();

    /**
     * 获取插件版本
     * @return String
     */
    String getPluginVersion();

    /**
     * 获取插件引导类
     * @return String
     */
    String getPluginBootstrapClass();

    /**
     * 获取插件classpath
     * @return String
     */
    String getPluginPath();

    /**
     * 获取插件名称
     * @return String
     */
    default String getName() {
        return getPluginId();
    }

    /**
     * 获取插件主类
     * @return String
     */
    default String getMainClass() {
        return getPluginBootstrapClass();
    }

    /**
     * 获取插件描述
     * @return String
     */
    String getDescription();

    /**
     * 获取插件所能安装到主程序包的版本
     * @return String
     */
    String getRequires();

    /**
     * 获取插件提供者/开发者
     * @return String
     */
    String getProvider();

    /**
     * 获取插件 license
     * @return String
     */
    String getLicense();

    /**
     * 获取当前插件依赖
     * @return List
     */
    List<DependencyPlugin> getDependencyPlugin();

    /**
     * 得到插件类型
     * @return 插件类型
     */
    PluginType getType();

    /**
     * 主动设置插件其它属性
     * @return map
     */
    HashMap<String,Object> pluginExtensionInfo();

    /**
     * 设置授权码
     * @return void
     */
    void setLicenseCode(String code);
    /**
     * 设置描述
     * @return void
     */
    void setLicenseDesc(String desc);
    /**
     * 设置授权时间
     * @return void
     */
    void setLicenseDateMill(Long mills);

    String getLicenseCode();

    String getLicenseDesc();

    Long getLicenseDateMill();

    /**
     * 获取需要排除的自动配置类
     * @return 排除的自动配置类集合
     */
    default Set<String> getExcludeAutoConfigurations() {
        return Collections.emptySet();
    }

    /**
     * 设置需要排除的自动配置类
     * @param excludeClasses 排除的自动配置类集合
     */
    default void setExcludeAutoConfigurations(Set<String> excludeClasses) {
        // 默认空实现
    }

    /**
     * 转换为 InsidePluginDescriptor
     * @return InsidePluginDescriptor
     */
    default InsidePluginDescriptor toInsidePluginDescriptor() {
        try {
            // 使用Path.of()创建临时Path对象
            Path pluginPath = Path.of(getPluginPath());
            DefaultInsidePluginDescriptor insideDescriptor = new DefaultInsidePluginDescriptor(
                getPluginId(),
                getPluginVersion(),
                getPluginBootstrapClass(),
                pluginPath
            );
            // 设置其它必要属性
            insideDescriptor.setType(getType());
            insideDescriptor.setDescription(getDescription());
            insideDescriptor.setProvider(getProvider());
            insideDescriptor.setRequires(getRequires());
            insideDescriptor.setLicense(getLicense());
            insideDescriptor.setLicenseCode(getLicenseCode());
            insideDescriptor.setLicenseDesc(getLicenseDesc());
            insideDescriptor.setLicenseDateMill(getLicenseDateMill());
            
            // 传递排除的自动配置类
            if (this instanceof InsidePluginDescriptor) {
                insideDescriptor.setExcludeAutoConfigurations(
                    ((InsidePluginDescriptor) this).getExcludeAutoConfigurations()
                );
            }
            return insideDescriptor;
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PluginDescriptor to InsidePluginDescriptor", e);
        }
    }
}
