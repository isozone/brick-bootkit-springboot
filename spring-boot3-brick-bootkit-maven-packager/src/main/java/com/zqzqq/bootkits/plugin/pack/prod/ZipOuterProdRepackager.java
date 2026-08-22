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
import com.zqzqq.bootkits.common.PluginDescriptorKey;
import com.zqzqq.bootkits.plugin.pack.Constant;
import com.zqzqq.bootkits.plugin.pack.RepackageMojo;
import com.zqzqq.bootkits.plugin.pack.utils.PackageZip;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static com.zqzqq.bootkits.common.PackageStructure.PROD_PLUGIN_META_PATH;
import static com.zqzqq.bootkits.common.PackageStructure.PROD_RESOURCES_DEFINE_PATH;

/**
 * zip-outer打包生成器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public class ZipOuterProdRepackager extends DirProdRepackager {

    protected PackageZip packageZip;

    public ZipOuterProdRepackager(RepackageMojo repackageMojo, ProdConfig prodConfig) {
        super(repackageMojo, prodConfig);
    }

    @Override
    public void repackage() throws MojoExecutionException, MojoFailureException {
        try {
            super.repackage();
        } catch (Exception e){
            throw new MojoFailureException(e);
        } finally {
            if(packageZip != null){
                IOUtils.closeQuietly(packageZip);
            }
        }
    }

    @Override
    protected String createRootDir() throws MojoFailureException {
        String rootDir = super.createRootDir();
        try {
            packageZip = getPackageZip(rootDir);
            return rootDir;
        } catch (Exception e) {
            throw new MojoFailureException(e);
        }
    }

    protected PackageZip getPackageZip(String rootDir) throws Exception {
        return new PackageZip(rootDir, super.prodConfig.getFileName());
    }

    @Override
    protected void resolveClasses() throws Exception {
        String buildDir = repackageMojo.getProject().getBuild().getOutputDirectory();
        packageZip.copyDirToPackage(new File(buildDir), "");
    }

    @Override
    protected Manifest getManifest() throws Exception {
        Manifest manifest = super.getManifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(ManifestKey.PLUGIN_META_PATH, PROD_PLUGIN_META_PATH);
        attributes.putValue(ManifestKey.PLUGIN_PACKAGE_TYPE, PackageType.PLUGIN_PACKAGE_TYPE_ZIP_OUTER);
        return manifest;
    }

    @Override
    protected Properties createPluginMetaInfo() throws Exception {
        Properties properties = super.createPluginMetaInfo();
        properties.put(PluginDescriptorKey.PLUGIN_PATH, packageZip.getFileName());
        return properties;
    }
    @Override
    protected void writeManifest(Manifest manifest) throws Exception {
        packageZip.writeManifest(manifest);
    }

    @Override
    protected String writePluginMetaInfo(Properties properties) throws Exception {
        packageZip.write(PROD_PLUGIN_META_PATH, outputStream->{
            properties.store(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                    Constant.PLUGIN_METE_COMMENTS);
        });
        return PROD_PLUGIN_META_PATH;
    }


    @Override
    protected String writeResourcesDefineFile(String resourcesDefineContent) throws Exception {
        packageZip.write(PROD_RESOURCES_DEFINE_PATH, resourcesDefineContent);
        return PROD_RESOURCES_DEFINE_PATH;
    }
}