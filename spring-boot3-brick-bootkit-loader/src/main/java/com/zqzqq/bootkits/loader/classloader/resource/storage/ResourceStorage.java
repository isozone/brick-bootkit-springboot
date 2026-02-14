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

package com.zqzqq.bootkits.loader.classloader.resource.storage;

import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.utils.Release;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

/**
 * 资源存储器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public interface ResourceStorage extends AutoCloseable, Release {

    /**
     * 添加资源url
     * @param baseUrl url
     */
    void addBaseUrl(URL baseUrl);

    /**
     * 获取资源url
     * @return url list
     */
    List<URL> getBaseUrl();

    /**
     * 添加资源
     * @param resource 资源名称
     * @throws Exception 添加资源异常
     */
    void add(Resource resource) throws Exception;

    /**
     * 存在资源
     * @param name 资源名称
     * @return 存在 true, 不存在 false
     */
    boolean exist(String name);

    /**
     * 获取第一个资源
     * @param name 资源名称
     * @return Resource
     */
    Resource getFirst(String name);

    /**
     * 获取所有资源
     * @param name 资源名称
     * @return Resource
     */
    Enumeration<Resource> get(String name);

    /**
     * 获取第一个资源的 InputStream
     * @param name 资源名称
     * @return Resource
     */
    InputStream getFirstInputStream(String name);

    /**
     * 获取所有资源的 InputStream
     * @param name 资源名称
     * @return InputStream
     */
    Enumeration<InputStream> getInputStream(String name);

}
