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

import com.zqzqq.bootkits.common.PackageStructure;
import com.zqzqq.bootkits.common.PackageType;
import com.zqzqq.bootkits.plugin.pack.utils.CommonUtils;
import com.zqzqq.bootkits.utils.FilesUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static com.zqzqq.bootkits.common.ManifestKey.*;

/**
 * jar 外部包
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.2
 */
public class JarOuterPackager extends JarNestPackager {

    private static final String LIB_INDEXES_SPLIT = " ";

    private final Set<String> dependencyIndexNames = new HashSet<>();

    public JarOuterPackager(MainRepackager mainRepackager) {
        super(mainRepackager);
    }

    @Override
    public void repackage() throws MojoExecutionException, MojoFailureException {
        // 生成依赖文件夹
        String rootDir = createRootDir();
        mainConfig.setOutputDirectory(rootDir);
        super.repackage();
    }

    @Override
    protected void writeClasses() throws Exception {
        String buildDir = repackageMojo.getProject().getBuild().getOutputDirectory();
        packageJar.copyDirToPackage(new File(buildDir), null);
    }

    private String createRootDir() throws MojoFailureException{
        String outputDirectory = mainConfig.getOutputDirectory();
        String fileName = mainConfig.getFileName();
        String rootDirPath = FilesUtils.joiningFilePath(outputDirectory, fileName);
        File rootFile = new File(rootDirPath);
        CommonUtils.deleteFile(rootFile);
        if(rootFile.mkdirs()){
            return rootDirPath;
        } else {
            throw new MojoFailureException("Create dir failure : " + rootDirPath);
        }
    }

    @Override
    protected Manifest getManifest() throws Exception {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(MANIFEST_VERSION, MANIFEST_VERSION_1_0);
        attributes.putValue(START_CLASS, mainConfig.getMainClass());
        attributes.putValue(MAIN_CLASS, MAIN_CLASS_VALUE);
        attributes.putValue(MAIN_PACKAGE_TYPE, PackageType.MAIN_PACKAGE_TYPE_JAR_OUTER);
        attributes.putValue(MAIN_LIB_DIR, getLibPath());
        attributes.putValue(DEVELOPMENT_MODE, mainConfig.getDevelopmentMode());

        // 添加jar包title和version属性
        MavenProject mavenProject = this.repackageMojo.getProject();
        attributes.putValue(IMPLEMENTATION_TITLE, mavenProject.getArtifactId());
        attributes.putValue(IMPLEMENTATION_VERSION, mavenProject.getVersion());
        return manifest;
    }


    private String getLibIndexes() throws Exception {
        if(dependencyIndexNames.isEmpty()){
            return "";
        }
        StringBuilder libName = new StringBuilder();
        for (String dependencyIndexName : dependencyIndexNames) {
            libName.append(dependencyIndexName).append(LIB_INDEXES_SPLIT);
        }
        return libName.toString();
    }

    @Override
    protected void writeDependencies() throws Exception {
        Set<Artifact> dependencies = repackageMojo.getSourceDependencies();
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
                File artifactFile = artifact.getFile();
                String libPath = getLibPath();
                if(FilesUtils.isRelativePath(libPath)){
                    libPath = FilesUtils.resolveRelativePath(mainConfig.getOutputDirectory(), getLibPath());
                } else {
                    libPath = FilesUtils.joiningFilePath(mainConfig.getOutputDirectory(), libPath);
                }
                String targetFilePath = FilesUtils.joiningFilePath(libPath, artifactFile.getName());
                FileUtils.copyFile(artifactFile, new File(targetFilePath));
                dependencyIndexNames.add(artifactFile.getName());
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

    private String getLibPath(){
        String libDir = PackageStructure.LIB_NAME;
        if(!ObjectUtils.isEmpty(mainConfig.getLibDir())){
            libDir = mainConfig.getLibDir();
        }
        return libDir;
    }

}

