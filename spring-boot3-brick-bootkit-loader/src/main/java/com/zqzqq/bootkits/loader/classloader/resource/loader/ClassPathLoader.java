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
import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.classloader.resource.storage.ResourceStorage;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * classpath 资源加载鍣?
 * @author starBlues
 * @since 3.0.0
 * @version 4.0.0
 */
public class ClassPathLoader extends AbstractResourceLoader {

    private final URL url;

    public ClassPathLoader(URL url) {
        super(url);
        this.url = Objects.requireNonNull(url, "url 不能为空");
    }

    public ClassPathLoader(File file) throws MalformedURLException {
        this(file.toPath());
    }

    public ClassPathLoader(Path path) throws MalformedURLException {
        super(path.toUri().toURL());
        this.url = super.baseUrl;
    }

    @Override
    protected void loadOfChild(ResourceStorage resourceStorage) throws Exception {
        File file = new File(url.toURI());
        load(resourceStorage, file, null);
    }

    private void load(ResourceStorage resourceStorage, File file, String currentPackageName) throws Exception {
        if(currentPackageName == null){
            // 类目录
            currentPackageName = "";
        } else {
            if("".equals(currentPackageName)){
                currentPackageName = file.getName();
            } else {
                currentPackageName = currentPackageName + Resource.PACKAGE_SPLIT + file.getName();
            }
            loadResource(resourceStorage, file, currentPackageName);
        }
        if(file.isDirectory()){
            File[] listFiles = file.listFiles();
            if(listFiles == null || listFiles.length == 0){
                return;
            }
            for (File subFile : listFiles) {
                load(resourceStorage, subFile, currentPackageName);
            }
        }
    }

    private void loadResource(ResourceStorage resourceStorage, File file, String packageName) throws Exception{
        if(file.isDirectory()){
            addResource(resourceStorage, file, packageName + Resource.PACKAGE_SPLIT);
        } else {
            addResource(resourceStorage, file, packageName);
        }
    }

    private void addResource(ResourceStorage resourceStorage, File file, String packageName) throws Exception {
        String fullUrl = url.toString();
        // 确保 baseUrl 和 packageName 之间有路径分隔符
        if (!fullUrl.endsWith("/") && !packageName.startsWith("/")) {
            fullUrl += "/";
        }
        URL resourceUrl = new URL(fullUrl + packageName);
        System.out.println("[ClassPathLoader] Adding resource - baseUrl: " + url + ", packageName: " + packageName + ", resourceUrl: " + resourceUrl);
        CacheResource cacheResource = new CacheResource(packageName, url, resourceUrl);
        cacheResource.setBytes(()->{
            if(file.exists() && file.isFile()){
                return getClassBytes(file.getPath(), Files.newInputStream(file.toPath()), true);
            } else {
                return null;
            }
        });
        resourceStorage.add(cacheResource);
    }


}