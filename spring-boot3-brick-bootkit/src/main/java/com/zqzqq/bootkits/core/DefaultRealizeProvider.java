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
 * distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.checker.ComposePluginBasicChecker;
import com.zqzqq.bootkits.core.checker.PluginBasicChecker;
import com.zqzqq.bootkits.core.descriptor.ComposeDescriptorLoader;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptorLoader;
import com.zqzqq.bootkits.core.scanner.BasePluginScanner;
import com.zqzqq.bootkits.core.scanner.DevPathResolve;
import com.zqzqq.bootkits.core.scanner.PluginScanner;
import com.zqzqq.bootkits.core.scanner.ProdPathResolve;
import com.zqzqq.bootkits.core.version.SemverVersionInspector;
import com.zqzqq.bootkits.core.version.VersionInspector;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.utils.Assert;
import org.springframework.context.ApplicationContext;

/**
 * 默认的RealizeProvider实现
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.1
 */
public class DefaultRealizeProvider implements RealizeProvider {

    private PluginScanner pluginScanner;
    private PluginBasicChecker pluginBasicChecker;
    private PluginDescriptorLoader pluginDescriptorLoader;
    private VersionInspector versionInspector;

    protected final IntegrationConfiguration configuration;
    protected final ApplicationContext applicationContext;

    public DefaultRealizeProvider(IntegrationConfiguration configuration,
                                  ApplicationContext applicationContext){
        this.configuration = Assert.isNotNull(configuration, "参数 configuration 不能为空");
        this.applicationContext = Assert.isNotNull(applicationContext, "参数 configuration 不能为空");
    }

    @Override
    public void init() {
        BasePluginScanner basePluginScanner = new BasePluginScanner();
        // 设置根目录路径（jar 所在目录），用于解析 ~ 相对路径
        basePluginScanner.setRootPath(getRootPath());
        if(configuration.environment() == RuntimeMode.DEV){
            basePluginScanner.setPathResolve(new DevPathResolve());
        } else {
            basePluginScanner.setPathResolve(new ProdPathResolve());
        }
        setPluginScanner(basePluginScanner);
        setPluginBasicChecker(new ComposePluginBasicChecker(applicationContext));
        setPluginDescriptorLoader(new ComposeDescriptorLoader(applicationContext, pluginBasicChecker));
        setVersionInspector(new SemverVersionInspector());
    }

    /**
     * 获取 jar 所在目录
     * @return jar 所在目录绝对路径
     */
    protected String getRootPath() {
        try {
            java.security.ProtectionDomain protectionDomain = getClass().getProtectionDomain();
            if (protectionDomain != null) {
                java.security.CodeSource codeSource = protectionDomain.getCodeSource();
                if (codeSource != null) {
                    java.net.URI location = codeSource.getLocation().toURI();
                    if (location != null) {
                        String path = location.getSchemeSpecificPart();
                        if (path != null) {
                            java.io.File root = new java.io.File(path);
                            if (root.exists()) {
                                if (root.isFile()) {
                                    return root.getParent();
                                } else {
                                    return root.getAbsolutePath();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 获取失败时返回当前工作目录
        }
        return System.getProperty("user.dir");
    }

    public void setPluginScanner(PluginScanner pluginScanner) {
        this.pluginScanner = Assert.isNotNull(pluginScanner, "pluginScanner 不能为空");
    }

    public void setPluginBasicChecker(PluginBasicChecker pluginBasicChecker) {
        this.pluginBasicChecker =  Assert.isNotNull(pluginBasicChecker,
                "pluginBasicChecker 不能为空");
    }

    public void setPluginDescriptorLoader(PluginDescriptorLoader pluginDescriptorLoader) {
        this.pluginDescriptorLoader = Assert.isNotNull(pluginDescriptorLoader,
                "pluginDescriptorLoader 不能为空");
    }

    public void setVersionInspector(VersionInspector versionInspector) {
        this.versionInspector = Assert.isNotNull(versionInspector, "versionInspector 不能为空");
    }

    @Override
    public RuntimeMode getRuntimeMode() {
        return configuration.environment();
    }

    @Override
    public PluginScanner getPluginScanner() {
        return Assert.isNotNull(pluginScanner, "PluginScanner 实现不能为空");
    }

    @Override
    public PluginBasicChecker getPluginBasicChecker() {
        return Assert.isNotNull(pluginBasicChecker, "pluginBasicChecker 实现不能为空");
    }

    @Override
    public PluginDescriptorLoader getPluginDescriptorLoader() {
        return Assert.isNotNull(pluginDescriptorLoader, "PluginDescriptorLoader 实现不能为空");
    }

    @Override
    public VersionInspector getVersionInspector() {
        return Assert.isNotNull(versionInspector, "VersionInspector 实现不能为空");
    }
}