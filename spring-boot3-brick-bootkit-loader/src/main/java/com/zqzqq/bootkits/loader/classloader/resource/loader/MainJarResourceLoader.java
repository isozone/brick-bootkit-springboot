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

import java.net.URL;

/**
 * 主程序内嵌 Jar 资源加载器
 *
 * @author starBlues
 * @version 4.0.0
 */
public class MainJarResourceLoader extends JarResourceLoader {

    private static final String PROD_CLASSES_PATH = "classes/";

    public MainJarResourceLoader(URL url) throws Exception {
        super(url);
    }

    @Override
    protected String resolveName(String name) {
        return name.replace(PROD_CLASSES_PATH, "");
    }
}
