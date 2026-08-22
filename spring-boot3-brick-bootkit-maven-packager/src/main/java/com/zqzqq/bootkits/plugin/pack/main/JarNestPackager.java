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



package com.zqzqq.bootkits.plugin.pack.main;

import com.zqzqq.bootkits.common.PackageType;
import com.zqzqq.bootkits.plugin.pack.Constant;
import com.zqzqq.bootkits.plugin.pack.RepackageMojo;
import com.zqzqq.bootkits.plugin.pack.Repackager;
import com.zqzqq.bootkits.plugin.pack.utils.CommonUtils;
import com.zqzqq.bootkits.plugin.pack.utils.PackageJar;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static com.zqzqq.bootkits.common.ManifestKey.*;
import static com.zqzqq.bootkits.common.PackageStructure.*;

/**
 * 嵌套jar打包
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class JarNestPackager implements Repackager {

    protected final MainConfig mainConfig;
    protected final RepackageMojo repackageMojo;

    protected PackageJar packageJar;

    private JarFile sourceJarFile;

    public JarNestPackager(MainRepackager mainRepackager) {
        this.mainConfig = mainRepackager.getMainConfig();
        this.repackageMojo = mainRepackager.getRepackageMojo();
    }

    @Override
    public void repackage() throws MojoExecutionException, MojoFailureException {
        try {
            sourceJarFile = CommonUtils.getSourceJarFile(repackageMojo.getProject());
            packageJar = new PackageJar(mainConfig.getOutputDirectory(), mainConfig.getFileName());
            writeClasses();
            writeDependencies();
            writeManifest();
        } catch (Exception e) {
            repackageMojo.getLog().error(e.getMessage(), e);
            throw new MojoFailureException(e);
        } finally {
            IOUtils.closeQuietly(packageJar);
            IOUtils.closeQuietly(sourceJarFile);
        }
    }

    protected void writeManifest() throws Exception {
        Manifest manifest = getManifest();
        packageJar.putDirEntry(META_INF_NAME + SEPARATOR);
        packageJar.write(PROD_MANIFEST_PATH, manifest::write);
    }

    protected Manifest getManifest() throws Exception{
        Manifest manifest = null;
        if(sourceJarFile != null){
            manifest = sourceJarFile.getManifest();
        } else {
            manifest = new Manifest();
        }
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(MANIFEST_VERSION, MANIFEST_VERSION_1_0);
        attributes.putValue(BUILD_TIME, CommonUtils.getDateTime());
        attributes.putValue(START_CLASS, mainConfig.getMainClass());
        attributes.putValue(MAIN_CLASS, MAIN_CLASS_VALUE);
        attributes.putValue(MAIN_PACKAGE_TYPE, PackageType.MAIN_PACKAGE_TYPE_JAR);
        attributes.putValue(DEVELOPMENT_MODE, mainConfig.getDevelopmentMode());

        // 添加jar包title和version属性
        MavenProject mavenProject = this.repackageMojo.getProject();
        attributes.putValue(IMPLEMENTATION_TITLE, mavenProject.getArtifactId());
        attributes.putValue(IMPLEMENTATION_VERSION, mavenProject.getVersion());
        return manifest;
    }

    protected void writeClasses() throws Exception {
        String buildDir = repackageMojo.getProject().getBuild().getOutputDirectory();
        packageJar.copyDirToPackage(new File(buildDir), null);
    }

    protected void writeDependencies() throws Exception {
        Set<Artifact> dependencies = repackageMojo.getSourceDependencies();
        String libDirEntryName = createLibEntry();
        boolean loaderExtracted = false;
        for (Artifact artifact : dependencies) {
            if(filterArtifact(artifact)){
                continue;
            }
            if(CommonUtils.isPluginFrameworkLoader(artifact)){
                // 非核心loader依赖
                packageJar.copyZipToPackage(artifact.getFile());
                loaderExtracted = true;
            } else {
                packageJar.writeDependency(artifact.getFile(), libDirEntryName);
            }
        }
        // 诊断: loader 未被识别/提取时, 启动会报找不到 SpringMainProdBootstrap
        if (!loaderExtracted && repackageMojo.getLog() != null) {
            repackageMojo.getLog().error(
                    "[brick-bootkit] 未识别到框架 loader 依赖 (期望坐标: "
                            + CommonUtils.PLUGIN_FRAMEWORK_GROUP_ID + ":"
                            + CommonUtils.PLUGIN_FRAMEWORK_LOADER_ARTIFACT_ID
                            + ")。prod 启动将报 '找不到或无法加载主类 "
                            + "com.zqzqq.bootkits.loader.launcher.SpringMainProdBootstrap'。"
                            + "请检查: 1) 主程序是否声明该依赖; 2) fork 改包名时通过 "
                            + "-DbrickBootkit.framework.groupId / "
                            + "-DbrickBootkit.framework.loaderArtifactId 覆盖默认坐标。");
        }
    }

    protected boolean filterArtifact(Artifact artifact) {
        return Constant.filterArtifact(artifact, repackageMojo.getIncludeSystemScope());
    }

    protected String createLibEntry() throws Exception {
        String libDirEntryName = PROD_LIB_PATH;
        packageJar.putDirEntry(libDirEntryName);
        return libDirEntryName;
    }

}

