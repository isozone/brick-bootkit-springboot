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



package com.zqzqq.bootkits.loader.launcher.classpath;

import com.zqzqq.bootkits.loader.archive.Archive;
import com.zqzqq.bootkits.loader.archive.ExplodedArchive;
import com.zqzqq.bootkits.loader.archive.JarFileArchive;
import com.zqzqq.bootkits.utils.FilesUtils;
import com.zqzqq.bootkits.loader.utils.ResourceUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Manifest;

import static com.zqzqq.bootkits.loader.LoaderConstant.*;

/**
 * jar out 类型的 classpath 获取器
 *
 * @author starBlues
 * @since 3.0.4
 * @version 3.1.2
 */

public class JarOutClasspathResource implements ClasspathResource{


    private final static Archive.EntryFilter ENTRY_FILTER = (entry)->{
        String name = entry.getName();
        return name.startsWith(PROD_CLASSES_PATH);
    };

    private final static Archive.EntryFilter INCLUDE_FILTER = (entry) -> {
        if (entry.isDirectory()) {
            return entry.getName().equals(PROD_CLASSES_PATH);
        }
        return false;
    };

    private final File rootJarFile;
    
    public JarOutClasspathResource(File rootJarFile) {
        this.rootJarFile = rootJarFile;
    }


    @Override
    public List<URL> getClasspath() throws Exception {
        Archive archive = getArchive();
        Iterator<Archive> archives = archive.getNestedArchives(ENTRY_FILTER, INCLUDE_FILTER);
        List<URL> urls = getEntryResource(archives);
        urls.addAll(getLibResource(archive));
        return urls;
    }

    private Archive getArchive() throws IOException {
        return (rootJarFile.isDirectory() ? new ExplodedArchive(rootJarFile) : new JarFileArchive(rootJarFile));
    }

    private List<URL> getEntryResource(Iterator<Archive> archives) throws Exception{
        List<URL> urls =  new ArrayList<>();
        while (archives.hasNext()){
            Archive archive = archives.next();
            URL url = archive.getUrl();
            String path = url.getPath();
            if(path.contains(PROD_CLASSES_URL_SIGN)){
                urls.add(url);
            }
        }
        return urls;
    }

    private List<URL> getLibResource(Archive archive) throws Exception{
        Manifest manifest = archive.getManifest();
        String libDir = manifest.getMainAttributes().getValue(MAIN_LIB_DIR);
        String relativePath = rootJarFile.isDirectory() ? rootJarFile.getPath() : rootJarFile.getParent();
        libDir = FilesUtils.resolveRelativePath(relativePath, libDir);
        File libJarDir = new File(libDir);
        if(!libJarDir.exists()){
            throw new IllegalStateException("主程序依赖目录不存在: " + libDir);
        }
        File[] libJarFile = getLibJarFile(libJarDir);
        List<URL> urls = new ArrayList<>(libJarFile.length);
        for (File file : libJarFile) {
            urls.add(file.toPath().toUri().toURL());
        }
        return urls;
    }

    private File[] getLibJarFile(File rootFile) {
        File[] listFiles = rootFile.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return ResourceUtils.isJarFile(pathname);
            }
        });
        if(listFiles == null || listFiles.length == 0){
            return new File[0];
        }
        return listFiles;
    }


}

