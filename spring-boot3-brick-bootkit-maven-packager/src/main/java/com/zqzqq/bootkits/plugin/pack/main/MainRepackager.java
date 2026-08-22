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
import com.zqzqq.bootkits.utils.ObjectUtils;
import com.zqzqq.bootkits.utils.ReflectionUtils;
import lombok.Getter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * Main application repackager.
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
@Getter
public class MainRepackager implements Repackager {

    private final RepackageMojo repackageMojo;
    private final MainConfig mainConfig;

    public MainRepackager(RepackageMojo repackageMojo) {
        this.repackageMojo = repackageMojo;
        this.mainConfig = repackageMojo.getMainConfig();
    }

    @Override
    public void repackage() throws MojoExecutionException, MojoFailureException {
        checkConfig();
        setDevelopmentMode();
        String packageType = mainConfig.getPackageType();
        Repackager repackager = null;
        if (PackageType.MAIN_PACKAGE_TYPE_JAR.equalsIgnoreCase(packageType)) {
            repackager = new JarNestPackager(this);
        } else if (PackageType.MAIN_PACKAGE_TYPE_JAR_OUTER.equalsIgnoreCase(packageType)) {
            repackager = new JarOuterPackager(this);
        } else {
            throw new MojoFailureException("Not found packageType : " + packageType);
        }
        repackager.repackage();
    }

    private void checkConfig() throws MojoFailureException {
        if (mainConfig == null) {
            throw new MojoFailureException("configuration.mainConfig config cannot be empty");
        }
        if (ObjectUtils.isEmpty(mainConfig.getMainClass())) {
            throw new MojoFailureException("configuration.mainConfig.mainClass config cannot be empty");
        }
        String fileName = mainConfig.getFileName();
        if (ObjectUtils.isEmpty(fileName)) {
            MavenProject project = repackageMojo.getProject();
            mainConfig.setFileName(project.getArtifactId() + "-" + project.getVersion() + "-repackage");
        }
        String packageType = mainConfig.getPackageType();
        if (ObjectUtils.isEmpty(packageType)) {
            mainConfig.setPackageType(PackageType.MAIN_PACKAGE_TYPE_JAR);
        }
        String outputDirectory = mainConfig.getOutputDirectory();
        if (ObjectUtils.isEmpty(outputDirectory)) {
            mainConfig.setOutputDirectory(repackageMojo.getOutputDirectory().getPath());
        }
    }

    private void setDevelopmentMode() throws MojoFailureException {
        String developmentMode = mainConfig.getDevelopmentMode();
        if (!ObjectUtils.isEmpty(developmentMode)) {
            return;
        }
        try {
            File file = new File(repackageMojo.getProject().getBuild().getOutputDirectory());
            Set<Artifact> artifacts = repackageMojo.getProject().getArtifacts();

            URL[] urls = new URL[artifacts.size() + 1];
            int i = 0;
            for (Artifact artifact : artifacts) {
                urls[i] = artifact.getFile().toURI().toURL();
                i++;
            }
            urls[i] = file.toURI().toURL();
            URLClassLoader urlClassLoader = new URLClassLoader(urls, null);

            String mainClass = repackageMojo.getMainConfig().getMainClass();
            if (ObjectUtils.isEmpty(mainClass)) {
                throw new Exception("mainConfig.mainClass config can't be empty");
            }
            Class<?> aClass = urlClassLoader.loadClass(mainClass);
            Method method = ReflectionUtils.findMethod(aClass, Constant.DEVELOPMENT_MODE_METHOD_NAME);
            String methodKey = aClass.getName() + "#" + Constant.DEVELOPMENT_MODE_METHOD_NAME + "()";
            if (method == null) {
                throw new Exception("Not found method : " + methodKey);
            }
            method.setAccessible(true);
            Object o = aClass.getConstructor().newInstance();
            Object result = method.invoke(o);
            if (ObjectUtils.isEmpty(result)) {
                throw new Exception(methodKey + " return value can't be empty");
            }
            getMainConfig().setDevelopmentMode(String.valueOf(result));
        } catch (Exception e) {
            throw new MojoFailureException("Set developmentMode failure:" + e.getMessage());
        }
    }
}
