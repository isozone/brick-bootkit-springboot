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

package com.zqzqq.bootkits.loader.classloader.resource.loader;

import com.zqzqq.bootkits.loader.classloader.resource.storage.ResourceStorage;
import com.zqzqq.bootkits.loader.utils.IOUtils;

import java.io.InputStream;
import java.net.URL;

/**
 * 插件的资源加载器
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public abstract class AbstractResourceLoader implements ResourceLoader{

    private static final String CLASS_FILE_EXTENSION = ".class";

    private boolean loaded = false;

    protected final URL baseUrl;

    protected AbstractResourceLoader(URL baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public URL getBaseUrl() {
        return baseUrl;
    }

    /**
     * 初始化Resource
     * @throws Exception 初始化异常
     */
    @Override
    public final synchronized void load(ResourceStorage resourceStorage) throws Exception{
        if(loaded){
            throw new Exception(this.getClass().getName()+": 已经初始化了, 不能再次初始化!");
        }
        try {
            // 添加root 资源
            resourceStorage.add(new DefaultResource("/", baseUrl, baseUrl));
            loadOfChild(resourceStorage);
        } finally {
            loaded = true;
        }
    }

    /**
     * 子类初始化实现
     * @param resourceStorage 资源存储
     * @throws Exception 初始化异常
     */
    protected abstract void loadOfChild(ResourceStorage resourceStorage) throws Exception;

    protected byte[] getClassBytes(String path, InputStream inputStream, boolean isClose) throws Exception{
        if(!isClass(path)){
            return null;
        }
        try {
            return IOUtils.read(inputStream);
        } finally {
            if(isClose){
                IOUtils.closeQuietly(inputStream);
            }
        }
    }

    @Override
    public void close() throws Exception {

    }

    private static boolean isClass(String path){
        if(path == null || "".equals(path)){
            return false;
        }
        return path.toLowerCase().endsWith(CLASS_FILE_EXTENSION);
    }

}