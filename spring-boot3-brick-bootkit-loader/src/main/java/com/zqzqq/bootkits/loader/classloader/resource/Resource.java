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

package com.zqzqq.bootkits.loader.classloader.resource;

import com.zqzqq.bootkits.loader.utils.Release;

import java.net.URL;

/**
 * 资源信息
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public interface Resource extends AutoCloseable, Release {

    String PACKAGE_SPLIT = "/";

    /**
     * 得到资源名称
     * @return 全局唯一的资源名称
     */
    String getName();

    /**
     * 得到资源URL位置
     * @return URL
     */
    URL getBaseUrl();

    /**
     * 获取完整URL路径
     * @return URL
     */
    URL getUrl();

    /**
     * 得到资源字节数组
     * @return byte[]
     */
    byte[] getBytes();

    /**
     * 解决byte
     * @throws Exception 处理资源字节异常
     */
    void resolveByte() throws Exception;

}