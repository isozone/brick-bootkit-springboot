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


package com.zqzqq.bootkits.core.security;

import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件签名验证器
 * 支持 RSA/ECDSA 数字签名验证，防止插件被篡改
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
public class PluginSignatureVerifier {

    private final Map<String, PublicKey> trustedKeys = new ConcurrentHashMap<>();
    private final Set<String> trustedSigners = new HashSet<>();
    private final List<SignatureListener> listeners = new ArrayList<>();
    private boolean requireSignature = false;

    public PluginSignatureVerifier() {
        // 初始化默认信任的签名者
        trustedSigners.add("default");
    }

    /**
     * 添加可信公钥
     */
    public void addTrustedKey(String signer, PublicKey publicKey) {
        trustedKeys.put(signer, publicKey);
        trustedSigners.add(signer);
    }

    /**
     * 添加可信签名者
     */
    public void addTrustedSigner(String signer) {
        trustedSigners.add(signer);
    }

    /**
     * 设置是否要求签名验证
     */
    public void setRequireSignature(boolean require) {
        this.requireSignature = require;
    }

    /**
     * 验证插件签名
     * 
     * @param pluginId 插件 ID
     * @param pluginPath 插件文件路径
     * @param signature 数字签名
     * @param signer 签名者
     * @return 验证结果
     */
    public PluginSecurityValidationResult verifySignature(
            String pluginId, 
            java.nio.file.Path pluginPath, 
            byte[] signature,
            String signer) {
        
        if (!requireSignature) {
            return PluginSecurityValidationResult.success(pluginId, "未启用签名验证");
        }

        // 检查签名者是否可信
        if (!trustedSigners.contains(signer)) {
            notifySignatureFailure(pluginId, "签名者不可信: " + signer);
            return PluginSecurityValidationResult.failure(
                    pluginId, "签名者不可信: " + signer);
        }

        // 检查是否有对应的公钥
        PublicKey publicKey = trustedKeys.get(signer);
        if (publicKey == null) {
            notifySignatureFailure(pluginId, "未找到签名者公钥: " + signer);
            return PluginSecurityValidationResult.failure(
                    pluginId, "未找到签名者公钥: " + signer);
        }

        try {
            // 计算文件摘要
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] fileContent = java.nio.file.Files.readAllBytes(pluginPath);
            byte[] fileHash = md.digest(fileContent);

            // 验证签名（RSA/ECDSA）
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(fileHash);
            
            if (!sig.verify(signature)) {
                notifySignatureFailure(pluginId, "签名验证失败");
                return PluginSecurityValidationResult.failure(
                        pluginId, "签名验证失败");
            }

            notifySignatureSuccess(pluginId, signer);
            return PluginSecurityValidationResult.success(pluginId, "签名验证通过");

        } catch (Exception e) {
            notifySignatureFailure(pluginId, "签名验证异常: " + e.getMessage());
            return PluginSecurityValidationResult.failure(
                    pluginId, "签名验证异常: " + e.getMessage());
        }
    }

    /**
     * 生成签名
     */
    public byte[] signPlugin(Path pluginPath, PrivateKey privateKey) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] fileContent = java.nio.file.Files.readAllBytes(pluginPath);
        byte[] fileHash = md.digest(fileContent);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(fileHash);
        return sig.sign();
    }

    /**
     * 添加签名监听器
     */
    public void addSignatureListener(SignatureListener listener) {
        listeners.add(listener);
    }

    private void notifySignatureSuccess(String pluginId, String signer) {
        for (SignatureListener listener : listeners) {
            listener.onSignatureSuccess(pluginId, signer);
        }
    }

    private void notifySignatureFailure(String pluginId, String reason) {
        for (SignatureListener listener : listeners) {
            listener.onSignatureFailure(pluginId, reason);
        }
    }

    /**
     * 签名监听器接口
     */
    public interface SignatureListener {
        void onSignatureSuccess(String pluginId, String signer);
        void onSignatureFailure(String pluginId, String reason);
    }
}
