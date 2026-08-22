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



package com.zqzqq.bootkits.loader.classloader.resource.storage;

import com.zqzqq.bootkits.loader.classloader.resource.Resource;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

/**
 * 绌虹殑资源存储
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.1
 */
public class EmptyResourceStorage implements ResourceStorage{

    @Override
    public void addBaseUrl(URL baseUrl) {

    }

    @Override
    public List<URL> getBaseUrl() {
        return null;
    }

    @Override
    public void add(Resource resource) throws Exception {

    }

    @Override
    public boolean exist(String name) {
        return false;
    }

    @Override
    public Resource getFirst(String name) {
        return null;
    }

    @Override
    public Enumeration<Resource> get(String name) {
        return null;
    }

    @Override
    public InputStream getFirstInputStream(String name) {
        return null;
    }

    @Override
    public Enumeration<InputStream> getInputStream(String name) {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}

