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



package com.zqzqq.bootkits.plugin.pack.prod;

import com.zqzqq.bootkits.common.ManifestKey;
import com.zqzqq.bootkits.common.PackageType;
import com.zqzqq.bootkits.plugin.pack.RepackageMojo;
import com.zqzqq.bootkits.plugin.pack.utils.PackageJar;
import com.zqzqq.bootkits.plugin.pack.utils.PackageZip;

import java.util.jar.Manifest;

/**
 * jar-outer打包生成器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class JarOuterProdRepackager extends ZipOuterProdRepackager {


    public JarOuterProdRepackager(RepackageMojo repackageMojo, ProdConfig prodConfig) {
        super(repackageMojo, prodConfig);
    }

    @Override
    protected PackageZip getPackageZip(String rootDir) throws Exception {
        return new PackageJar(rootDir, super.prodConfig.getFileName());
    }

    @Override
    protected Manifest getManifest() throws Exception {
        Manifest manifest = super.getManifest();
        manifest.getMainAttributes().putValue(ManifestKey.PLUGIN_PACKAGE_TYPE,
                PackageType.PLUGIN_PACKAGE_TYPE_JAR_OUTER);
        return manifest;
    }


}