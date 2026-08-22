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



package com.zqzqq.bootkits.common.cipher;

import com.zqzqq.bootkits.utils.Assert;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 非对称插件加解密
 *
 * @author starBlues
 * @since 3.0.1
 * @version 3.0.1
 */
public class RsaPluginCipher extends AbstractPluginCipher{

    public final static String PUBLIC_KEY = "publicKey";
    public final static String PRIVATE_KEY = "privateKey";

    public RsaPluginCipher(){
    }

    @Override
    protected String encryptImpl(String sourceStr) throws Exception {
        String publicKey = super.parameters.getString(PUBLIC_KEY);
        Assert.isNotEmpty(publicKey, PUBLIC_KEY + " 不能为空");
        byte[] decoded = Base64.getDecoder().decode(publicKey);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory
                .getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(decoded));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(sourceStr.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    protected String decryptImpl(String cryptoStr) throws Exception {
        String privateKey = super.parameters.getString(PRIVATE_KEY);
        Assert.isNotEmpty(privateKey, PRIVATE_KEY + " 不能为空");
        byte[] inputByte = Base64.getDecoder().decode(cryptoStr.getBytes(StandardCharsets.UTF_8));
        byte[] decoded = Base64.getDecoder().decode(privateKey);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        return new String(cipher.doFinal(inputByte), StandardCharsets.UTF_8);
    }

    /**
     * 生成 512大小 密钥对
     * @return 密钥对对象
     * @throws NoSuchAlgorithmException 生成异常
     */
    public static RsaKey generateKey() throws NoSuchAlgorithmException {
        return generateKey(512);
    }

    /**
     * 生成密钥对
     * @param keySize 密钥对大小
     * @return 密钥对对象
     * @throws NoSuchAlgorithmException 生成异常
     */
    public static RsaKey generateKey(Integer keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(keySize);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RsaKey(
                Base64.getEncoder().encodeToString(rsaPublicKey.getEncoded()),
                Base64.getEncoder().encodeToString(rsaPrivateKey.getEncoded())
        );
    }

    public static class RsaKey{
        private final String publicKey;
        private final String privateKey;

        public RsaKey(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }
    }

}
