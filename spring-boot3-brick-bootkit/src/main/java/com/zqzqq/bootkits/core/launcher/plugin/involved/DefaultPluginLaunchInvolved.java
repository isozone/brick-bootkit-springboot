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

package com.zqzqq.bootkits.core.launcher.plugin.involved;

import com.zqzqq.bootkits.core.PluginExtensionInfo;
import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginLibInfo;
import com.zqzqq.bootkits.loader.PluginResourceStorage;
import com.zqzqq.bootkits.spring.ApplicationContext;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import com.zqzqq.bootkits.spring.web.PluginStaticResourceResolver;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.SpringBeanCustomUtils;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 默认的插件启动参与器
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
@Slf4j
public class DefaultPluginLaunchInvolved implements PluginLaunchInvolved{
    private static final Logger log = LoggerFactory.getLogger(DefaultPluginLaunchInvolved.class);


    @Override
    public void before(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader) throws Exception {
        InsidePluginDescriptor descriptor = pluginInsideInfo.getPluginDescriptor();
        Set<PluginLibInfo> pluginLibInfo = descriptor.getPluginLibInfo();
        List<String> libPath = null;
        if(pluginLibInfo != null){
            libPath = pluginLibInfo.stream().map(PluginLibInfo::getPath).collect(Collectors.toList());
        }
        PluginResourceStorage.addPlugin(descriptor.getPluginId(), descriptor.getPluginFileName(), libPath);
    }

    @Override
    public void after(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader, SpringPluginHook pluginHook) throws Exception {
        InsidePluginDescriptor descriptor = pluginInsideInfo.getPluginDescriptor();
        PluginStaticResourceResolver.parse(descriptor, classLoader, pluginHook.getWebConfig());
        setExtensionInfoSupplier(pluginInsideInfo, pluginHook);
    }

    @Override
    public void close(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader) throws Exception {
        InsidePluginDescriptor descriptor = pluginInsideInfo.getPluginDescriptor();
        String pluginId = descriptor.getPluginId();
        PluginStaticResourceResolver.remove(pluginId);
    }

    private void setExtensionInfoSupplier(PluginInsideInfo pluginInsideInfo, SpringPluginHook pluginHook){
        if(pluginInsideInfo instanceof com.zqzqq.bootkits.core.EnhancedPluginInsideInfo) {
            ((com.zqzqq.bootkits.core.EnhancedPluginInsideInfo) pluginInsideInfo).setExtensionInfoSupplier(()->{
                // 设置插件自有扩展信息
                ApplicationContext applicationContext = pluginHook.getApplicationContext();
                List<PluginExtensionInfo> beans = SpringBeanCustomUtils.getBeans(applicationContext,
                        PluginExtensionInfo.class);
                if(ObjectUtils.isEmpty(beans)){
                    return new HashMap<>(0);
                }
                Map<String, Object> extensionInfos = new HashMap<>();
                for (PluginExtensionInfo extensionInfoBean : beans) {
                    try {
                        Map<String, Object> extensionInfo = extensionInfoBean.extensionInfo();
                        if(!ObjectUtils.isEmpty(extensionInfo)){
                            extensionInfos.putAll(extensionInfo);
                        }
                    } catch (Exception e){
                        log.error(e.getMessage(), e);
                    }
                }
                return extensionInfos;
            });
        }
    }

}

