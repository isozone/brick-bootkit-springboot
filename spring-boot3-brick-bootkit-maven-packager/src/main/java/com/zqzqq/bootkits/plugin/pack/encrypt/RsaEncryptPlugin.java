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
import com.zqzqq.bootkits.common.cipher.RsaPluginCipher;
import com.zqzqq.bootkits.plugin.pack.PluginInfo;
import com.zqzqq.bootkits.utils.ObjectUtils;
import org.apache.maven.plugin.MojoExecutionException;

import java.util.HashMap;
import java.util.Map;

/**
 * RSA 算法插件加密
 *
 * @author starBlues
 * @since 3.0.1
 * @version 3.0.1
 */
public class RsaEncryptPlugin implements EncryptPlugin{

    @Override
    public PluginInfo encrypt(EncryptConfig encryptConfig, PluginInfo pluginInfo) throws Exception {
        RsaConfig rsaConfig = encryptConfig.getRsa();
        if(rsaConfig == null){
            return null;
        }

        String publicKey = rsaConfig.getPublicKey();
        if(ObjectUtils.isEmpty(publicKey)){
            throw new MojoExecutionException("encryptConfig.rsa.publicKey can't be empty");
        }
        AbstractPluginCipher pluginCipher = new RsaPluginCipher();
        Map<String, Object> params = new HashMap<>();
        params.put(RsaPluginCipher.PUBLIC_KEY, publicKey);
        pluginCipher.initParams(params);

        String bootstrapClass = pluginInfo.getBootstrapClass();
        pluginInfo.setBootstrapClass(pluginCipher.encrypt(bootstrapClass));
        return pluginInfo;
    }
}
