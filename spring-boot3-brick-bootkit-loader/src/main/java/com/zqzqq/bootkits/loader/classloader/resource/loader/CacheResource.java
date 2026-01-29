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

import com.zqzqq.bootkits.loader.classloader.resource.ResourceByteGetter;

import java.net.URL;
import java.util.Arrays;

/**
 * 鍙紦瀛樼殑资源
 *
 * @author starBlues
 * @since 3.1.1
 * @version 3.1.1
 */
public class CacheResource extends DefaultResource {

    protected byte[] bytes;
    private ResourceByteGetter byteGetter;

    public CacheResource(String name, URL baseUrl, URL url) {
        this(name, baseUrl, url, null);
    }

    public CacheResource(String name, URL baseUrl, URL url, ResourceByteGetter byteGetter) {
        super(name, baseUrl, url);
        setBytes(byteGetter);
    }

    public void setBytes(ResourceByteGetter byteGetter) {
        this.byteGetter = byteGetter;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }

    @Override
    public void resolveByte() throws Exception{
        if(byteGetter != null && bytes == null){
            bytes = byteGetter.get();
        }
    }

    @Override
    public void close() throws Exception {
        release();
    }

    @Override
    public void release() {
        Arrays.fill(bytes, (byte) 0);
        bytes = null;
    }
}

