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

/**
 * 插件密码接口
 *
 * @author starBlues
 * @since 3.0.1
 * @version 3.0.1
 */
public interface PluginCipher {

    /**
     * 加密
     * @param sourceStr 原始字符串
     * @return 加密后的字节
     * @throws Exception 加密异常
     */
    String encrypt(String sourceStr) throws Exception;

    /**
     * 解密
     * @param cryptoStr 加密的字符串
     * @return 解密后的字符
     * @throws Exception 解密异常
     */
    String decrypt(String cryptoStr) throws Exception;


}