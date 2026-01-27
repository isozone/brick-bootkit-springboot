/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.common.cipher;

import com.zqzqq.bootkits.utils.Assert;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * AES 加密
 *
 * @author starBlues
 * @since 3.0.1
 * @version 3.0.1
 */
public class AesPluginCipher extends AbstractPluginCipher{

    public final static String SECRET_KEY = "secretKey";

    private static final String INSTANCE_KEY = "AES/ECB/PKCS5Padding";
    private static final String AES_KEY = "AES";

    @Override
    protected String encryptImpl(String sourceStr) throws Exception {
        Key convertSecretKey = getKey();
        Cipher cipher = Cipher.getInstance(INSTANCE_KEY);
        cipher.init(Cipher.ENCRYPT_MODE, convertSecretKey);
        byte[] result = cipher.doFinal(sourceStr.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(result);
    }

    @Override
    protected String decryptImpl(String cryptoStr) throws Exception {
        Key convertSecretKey = getKey();
        Cipher cipher = Cipher.getInstance(INSTANCE_KEY);
        cipher.init(Cipher.DECRYPT_MODE, convertSecretKey);
        byte[] decode = Base64.getDecoder().decode(cryptoStr);
        byte[] result = cipher.doFinal(decode);
        return new String(result, StandardCharsets.UTF_8);
    }


    private Key getKey() throws Exception{
        String secretKey = super.parameters.getString(SECRET_KEY);
        Assert.isNotEmpty(secretKey, SECRET_KEY + " 不能为空");
        byte[] keyBytes = Base64.getDecoder().decode(secretKey);
        return new SecretKeySpec(keyBytes, AES_KEY);
    }

    /**
     * 获取密钥
     * @return 密钥字符串
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    public static String generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_KEY);
        keyGenerator.init(128);
        Key secretKey = keyGenerator.generateKey();
        byte[] keyBytes = secretKey.getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }

}