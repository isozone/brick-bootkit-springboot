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


package com.zqzqq.bootkits.core.descriptor.decrypt;

import com.zqzqq.bootkits.common.PluginDescriptorKey;
import com.zqzqq.bootkits.common.cipher.AbstractPluginCipher;
import com.zqzqq.bootkits.common.cipher.PluginCipher;
import com.zqzqq.bootkits.core.exception.PluginDecryptException;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.integration.decrypt.DecryptConfiguration;
import com.zqzqq.bootkits.integration.decrypt.DecryptPluginConfiguration;
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.PropertiesUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.util.*;

/**
 * 默认PluginDescriptorDecrypt
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.1
 */
public class DefaultPluginDescriptorDecrypt implements PluginDescriptorDecrypt{

    private final ApplicationContext applicationContext;

    private final DecryptConfiguration decryptConfig;
    private final Map<String, DecryptPluginConfiguration> pluginDecryptConfig;

    public DefaultPluginDescriptorDecrypt(ApplicationContext applicationContext,
                                          IntegrationConfiguration configuration) {
        this.applicationContext = applicationContext;

        this.decryptConfig = configuration.decrypt();
        List<DecryptPluginConfiguration> plugins = decryptConfig.getPlugins();
        if(ObjectUtils.isEmpty(plugins)){
            this.pluginDecryptConfig = Collections.emptyMap();
        } else {
            this.pluginDecryptConfig = new HashMap<>(plugins.size());
            for (DecryptPluginConfiguration plugin : plugins) {
                pluginDecryptConfig.put(plugin.getPluginId(), plugin);
            }
        }
    }

    @Override
    public Properties decrypt(String pluginId, Properties properties) {
        PluginCipher pluginCipher = getPluginCipher(pluginId);
        if(pluginCipher == null){
            return properties;
        }
        try {
            String bootstrapClass = PropertiesUtils.getValue(properties, PluginDescriptorKey.PLUGIN_BOOTSTRAP_CLASS);
            String decrypt = pluginCipher.decrypt(bootstrapClass);
            properties.setProperty(PluginDescriptorKey.PLUGIN_BOOTSTRAP_CLASS, decrypt);
            return properties;
        } catch (Exception e) {
            throw new PluginDecryptException("插件[" + pluginId + "]瑙ｅ瘑失败. " + e.getMessage());
        }
    }

    protected PluginCipher getPluginCipher(String pluginId){
        if(decryptConfig == null){
            return null;
        }
        Boolean enable = decryptConfig.getEnable();
        if(enable == null || !enable){
            // 没有启用
            return null;
        }
        Map<String, Object> props = decryptConfig.getProps();
        if(props == null){
            props = new HashMap<>();
            decryptConfig.setProps(props);
        }
        String className = decryptConfig.getClassName();
        if(ObjectUtils.isEmpty(pluginDecryptConfig)){
            // 没有配置具体插件的解密配置
            return getPluginCipherBean(className, props);
        }
        DecryptPluginConfiguration decryptPluginConfiguration = pluginDecryptConfig.get(pluginId);
        if(decryptPluginConfiguration == null){
            // 当前插件没有配置解密配置, 说明不启用解密
            return null;
        }
        Map<String, Object> pluginParam = decryptPluginConfiguration.getProps();
        if(!ObjectUtils.isEmpty(pluginParam)){
            props.putAll(pluginParam);
        }
        return getPluginCipherBean(className, props);
    }


    protected PluginCipher getPluginCipherBean(String className, Map<String, Object> params){
        ClassLoader defaultClassLoader = ClassUtils.getDefaultClassLoader();
        try {
            if(defaultClassLoader == null){
                defaultClassLoader = this.getClass().getClassLoader();
            }
            Class<?> aClass = defaultClassLoader.loadClass(className);

            String error = "解密实现类[" + className + "]没有继承 [" + AbstractPluginCipher.class.getName() + "]";

            if(aClass.isAssignableFrom(AbstractPluginCipher.class)){
                throw new PluginDecryptException(error);
            }
            Object bean = getBean(aClass);
            if(bean instanceof AbstractPluginCipher){
                AbstractPluginCipher pluginCipher = (AbstractPluginCipher) bean;
                pluginCipher.initParams(params);
                return pluginCipher;
            } else {
                throw new PluginDecryptException(error);
            }
        } catch (ClassNotFoundException e) {
            throw new PluginDecryptException("没有发现解密实现类: " + className);
        }
    }

    protected Object getBean(Class<?> aClass){
        try {
            return applicationContext.getBean(aClass);
        } catch (Exception e1){
            try {
                return aClass.getConstructor().newInstance();
            } catch (Exception e2){
                throw new PluginDecryptException(e2);
            }
        }
    }

}

