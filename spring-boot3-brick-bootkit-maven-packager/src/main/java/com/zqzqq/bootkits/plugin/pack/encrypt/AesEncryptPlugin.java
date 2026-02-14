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

