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



package com.zqzqq.bootkits.loader.classloader.resource.loader;

import com.zqzqq.bootkits.loader.classloader.resource.storage.ResourceStorage;

import java.net.URL;

/**
 * 资源加载鍣?
 *
 * @author starBlues
 * @version 4.0.0
 */
public interface ResourceLoader extends AutoCloseable{

    /**
     * 获取资源鍩烘湰 URL
     * @return URL
     */
    URL getBaseUrl();


    /**
     * 瑁呰浇资源鍒癛esourceStorage
     * @param resourceStorage 资源存储
     * @throws Exception 瑁呰浇异常
     */
    void load(ResourceStorage resourceStorage) throws Exception;

}