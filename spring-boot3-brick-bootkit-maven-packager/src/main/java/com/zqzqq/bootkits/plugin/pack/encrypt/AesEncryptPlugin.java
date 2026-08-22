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


package com.zqzqq.bootkits.plugin.pack.encrypt;

import com.zqzqq.bootkits.common.cipher.AbstractPluginCipher;
import com.zqzqq.bootkits.common.cipher.AesPluginCipher;
import com.zqzqq.bootkits.plugin.pack.PluginInfo;
import com.zqzqq.bootkits.utils.ObjectUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.HashMap;
import java.util.Map;

/**
 * AES 算法插件加密
 *
 * @author starBlues
 * @since 3.0.1
 * @version 3.0.1
 */
public class AesEncryptPlugin implements EncryptPlugin{


    @Override
    public PluginInfo encrypt(EncryptConfig encryptConfig, PluginInfo pluginInfo) throws Exception{
        AesConfig aesConfig = encryptConfig.getAes();
        if(aesConfig == null){
            return null;
        }

        String secretKey = aesConfig.getSecretKey();
        if(ObjectUtils.isEmpty(secretKey)){
            throw new MojoExecutionException("encryptConfig.aes.secretKey can't be empty");
        }
        AbstractPluginCipher pluginCipher = new AesPluginCipher();
        Map<String, Object> params = new HashMap<>();
        params.put(AesPluginCipher.SECRET_KEY, secretKey);
        pluginCipher.initParams(params);

        String bootstrapClass = pluginInfo.getBootstrapClass();
        String encrypt = pluginCipher.encrypt(bootstrapClass);
        pluginInfo.setBootstrapClass(encrypt);
        return pluginInfo;
    }
}

