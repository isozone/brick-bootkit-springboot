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



package com.zqzqq.bootkits.loader.launcher.coexist;

import com.zqzqq.bootkits.loader.classloader.GeneralUrlClassLoader;
import com.zqzqq.bootkits.loader.classloader.resource.loader.MainJarResourceLoader;
import com.zqzqq.bootkits.loader.launcher.classpath.ClasspathResource;
import com.zqzqq.bootkits.loader.launcher.classpath.JarOutClasspathResource;
import com.zqzqq.bootkits.loader.launcher.runner.MethodRunner;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static com.zqzqq.bootkits.loader.LoaderConstant.PROD_CLASSES_URL_SIGN;


/**
 * 主程序 Jar-outer 共存模式启动器
 *
 * @author starBlues
 * @since 3.1.2
 * @version 4.0.0
 */
public class CoexistJarOuterLauncher extends CoexistBaseLauncher {

    private final ClasspathResource classpathResource;

    public CoexistJarOuterLauncher(MethodRunner methodRunner, File rootJarFile) {
        super(methodRunner);
        Objects.requireNonNull(rootJarFile, "参数 rootJarFile 不能为空");
        this.classpathResource = new JarOutClasspathResource(rootJarFile);
    }

    @Override
    protected boolean resolveThreadClassLoader() {
        return true;
    }

    @Override
    protected void addResource(GeneralUrlClassLoader classLoader) throws Exception {
        super.addResource(classLoader);
        List<URL> classpath = classpathResource.getClasspath();
        for (URL url : classpath) {
            String path = url.getPath();
            if(path.contains(PROD_CLASSES_URL_SIGN)){
                classLoader.addResource(new MainJarResourceLoader(url));
            } else {
                classLoader.addResource(url);
            }
        }
    }


}
