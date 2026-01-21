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

package com.zqzqq.bootkits.plugin.pack;

import com.zqzqq.bootkits.common.AbstractDependencyPlugin;
import com.zqzqq.bootkits.common.ManifestKey;
import com.zqzqq.bootkits.common.PackageStructure;
import com.zqzqq.bootkits.common.PackageType;
import com.zqzqq.bootkits.plugin.pack.utils.CommonUtils;
import com.zqzqq.bootkits.utils.FilesUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static com.zqzqq.bootkits.common.PackageStructure.*;
import static com.zqzqq.bootkits.common.PluginDescriptorKey.*;

/**
 * 基础打包
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class BasicRepackager implements Repackager{

    @Getter
    private String rootDir;
    private String relativeManifestPath;
    private String relativePluginMetaPath;
    private String relativeResourcesDefinePath;

    protected File resourcesDefineFile;

    protected final RepackageMojo repackageMojo;
    protected JarFile sourceJarFile;

    public BasicRepackager(RepackageMojo repackageMojo) {
        this.repackageMojo = repackageMojo;
    }

    @Override
    public void repackage() throws MojoExecutionException, MojoFailureException {
        sourceJarFile = CommonUtils.getSourceJarFile(repackageMojo.getProject());
        checkPluginInfo();
        rootDir = createRootDir();
        relativeManifestPath = getRelativeManifestPath();
        relativePluginMetaPath = getRelativePluginMetaPath();
        relativeResourcesDefinePath = getRelativeResourcesDefinePath();
        try {
            Manifest manifest = getManifest();
            writeManifest(manifest);
        } catch (Exception e) {
            repackageMojo.getLog().error(e.getMessage(), e);
            throw new MojoFailureException(e);
        } finally {
            IOUtils.closeQuietly(sourceJarFile);
        }
    }

    private void checkPluginInfo() throws MojoExecutionException {
        PluginInfo pluginInfo = repackageMojo.getPluginInfo();
        if(pluginInfo == null){
            throw new MojoExecutionException("configuration.pluginInfo config cannot be empty");
        }
        if(ObjectUtils.isEmpty(pluginInfo.getId())){
            throw new MojoExecutionException("configuration.pluginInfo.id config cannot be empty");
        } else {
            String id = pluginInfo.getId();
            String illegal = PackageStructure.getIllegal(id);
            if(illegal != null){
                throw new MojoExecutionException("configuration.pluginInfo.id config can't contain: " + illegal);
            }
        }
        if(ObjectUtils.isEmpty(pluginInfo.getBootstrapClass())){
            throw new MojoExecutionException("configuration.pluginInfo.bootstrapClass config cannot be empty");
        }
        if(ObjectUtils.isEmpty(pluginInfo.getVersion())){
            throw new MojoExecutionException("configuration.pluginInfo.version config cannot be empty");
        } else {
            String version = pluginInfo.getVersion();
            String illegal = PackageStructure.getIllegal(version);
            if(illegal != null){
                throw new MojoExecutionException("configuration.pluginInfo.version config can't contain: " + illegal);
            }
        }
    }

    protected String getRelativeManifestPath(){
        return MANIFEST;
    }

    protected String getRelativeResourcesDefinePath(){
        return RESOURCES_DEFINE_NAME;
    }

    protected String getRelativePluginMetaPath(){
        return PLUGIN_META_NAME;
    }

    protected String createRootDir() throws MojoFailureException {
        String rootDirPath = getBasicRootDir();
        File rootDir = new File(rootDirPath);
        CommonUtils.deleteFile(rootDir);
        if(rootDir.mkdir()){
            return rootDirPath;
        }
        throw new MojoFailureException("Failed to create the plugin root directory. " + rootDirPath);
    }

    protected String getBasicRootDir(){
        File outputDirectory = repackageMojo.getOutputDirectory();
        return FilesUtils.joiningFilePath(outputDirectory.getPath(), PackageStructure.META_INF_NAME);
    }

    protected void writeManifest(Manifest manifest) throws Exception {
        String manifestPath = FilesUtils.joiningFilePath(rootDir, resolvePath(this.relativeManifestPath));
        File file = new File(manifestPath);
        FileUtils.forceMkdirParent(file);
        if(file.createNewFile()){
            try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
                manifest.write(outputStream);
            }
        }
    }

    protected Manifest getManifest() throws Exception{
        Manifest manifest = null;
        if(sourceJarFile != null){
            manifest = sourceJarFile.getManifest();
        } else {
            manifest = new Manifest();
        }
        Attributes attributes = manifest.getMainAttributes();
        attributes.putValue(ManifestKey.MANIFEST_VERSION, ManifestKey.MANIFEST_VERSION_1_0);
        attributes.putValue(ManifestKey.BUILD_TIME, CommonUtils.getDateTime());
        attributes.putValue(ManifestKey.PLUGIN_META_PATH, getPluginMetaInfoPath());
        attributes.putValue(ManifestKey.PLUGIN_PACKAGE_TYPE, PackageType.PLUGIN_PACKAGE_TYPE_DEV);
        // 添加jar包title和version属性，使用pluginInfo而非mavenProject
        PluginInfo pluginInfo = this.repackageMojo.getPluginInfo();
        attributes.putValue(ManifestKey.IMPLEMENTATION_TITLE, pluginInfo.getId());
        attributes.putValue(ManifestKey.IMPLEMENTATION_VERSION, pluginInfo.getVersion());
        return manifest;
    }

    /**
     * 得到插件信息存储文件路径
     * @return 插件信息存储文件路径
     * @throws Exception Exception
     */
    protected String getPluginMetaInfoPath() throws Exception {
        Properties pluginMetaInfo = createPluginMetaInfo();
        return writePluginMetaInfo(pluginMetaInfo);
    }

    /**
     * 创建插件信息
     * @return Properties
     * @throws Exception Exception
     */
    protected Properties createPluginMetaInfo() throws Exception {
        Properties properties = new Properties();
        PluginInfo pluginInfo = repackageMojo.getPluginInfo();
        properties.put(PLUGIN_ID, pluginInfo.getId());
        properties.put(PLUGIN_BOOTSTRAP_CLASS, pluginInfo.getBootstrapClass());
        properties.put(PLUGIN_VERSION, pluginInfo.getVersion());
        properties.put(PLUGIN_PATH, getPluginPath());

        String resourcesDefineFilePath = writeResourcesDefineFile(getResourcesDefineContent());
        if(!ObjectUtils.isEmpty(resourcesDefineFilePath)){
            properties.put(PLUGIN_RESOURCES_CONFIG, resourcesDefineFilePath);
        }
        String configFileName = pluginInfo.getConfigFileName();
        if(!ObjectUtils.isEmpty(configFileName)){
            properties.put(PLUGIN_CONFIG_FILE_NAME, configFileName);
        }
        String configFileLocation = pluginInfo.getConfigFileLocation();
        if(!ObjectUtils.isEmpty(configFileLocation)){
            properties.put(PLUGIN_CONFIG_FILE_LOCATION, configFileLocation);
        }
        String args = pluginInfo.getArgs();
        if(!ObjectUtils.isEmpty(args)){
            properties.put(PLUGIN_ARGS, args);
        }
        String provider = pluginInfo.getProvider();
        if(!ObjectUtils.isEmpty(provider)){
            properties.put(PLUGIN_PROVIDER, provider);
        }
        String requires = pluginInfo.getRequires();
        if(!ObjectUtils.isEmpty(requires)){
            properties.put(PLUGIN_REQUIRES, requires);
        }
        String dependencyPlugins = getDependencyPlugin(pluginInfo);
        if(!ObjectUtils.isEmpty(dependencyPlugins)){
            properties.put(PLUGIN_DEPENDENCIES, dependencyPlugins);
        }
        String description = pluginInfo.getDescription();
        if(!ObjectUtils.isEmpty(description)){
            properties.put(PLUGIN_DESCRIPTION, description);
        }
        String license = pluginInfo.getLicense();
        if(!ObjectUtils.isEmpty(license)){
            properties.put(PLUGIN_LICENSE, license);
        }
        return properties;
    }

    protected String getDependencyPlugin(PluginInfo pluginInfo){
        List<DependencyPlugin> dependencyPlugins = pluginInfo.getDependencyPlugins();
        return AbstractDependencyPlugin.toStr(dependencyPlugins);
    }

    /**
     * 写入插件信息
     * @param properties properties
     * @return String
     * @throws IOException IOException
     */
    protected String writePluginMetaInfo(Properties properties) throws Exception {
        File pluginMetaFile = createPluginMetaFile();
        try (OutputStreamWriter writer = new OutputStreamWriter(
                Files.newOutputStream(pluginMetaFile.toPath()), StandardCharsets.UTF_8)){
            properties.store(writer, Constant.PLUGIN_METE_COMMENTS);
            return pluginMetaFile.getPath();
        }
    }

    /**
     * 创建插件信息存储文件
     * @return File
     * @throws IOException 创建文件异常
     */
    protected File createPluginMetaFile() throws IOException {
        String path = FilesUtils.joiningFilePath(rootDir, resolvePath(relativePluginMetaPath));
        return FilesUtils.createFile(path);
    }

    /**
     * 获取插件路径
     * @return 插件路径
     */
    protected String getPluginPath(){
        return repackageMojo.getProject().getBuild().getOutputDirectory();
    }

    protected String writeResourcesDefineFile(String resourcesDefineContent) throws Exception{
        resourcesDefineFile = createResourcesDefineFile();
        FileUtils.write(resourcesDefineFile, resourcesDefineContent, CHARSET_NAME, true);
        return resourcesDefineFile.getPath();
    }

    protected File createResourcesDefineFile() throws IOException {
        String path = FilesUtils.joiningFilePath(rootDir, resolvePath(relativeResourcesDefinePath));
        return FilesUtils.createFile(path);
    }

    protected String getResourcesDefineContent() throws Exception {
        String dependenciesIndex = getDependenciesIndex();
        String loadMainResources = getLoadMainResources();
        boolean indexIsEmpty = ObjectUtils.isEmpty(dependenciesIndex);
        boolean resourceIsEmpty = ObjectUtils.isEmpty(loadMainResources);

        if(!indexIsEmpty && !resourceIsEmpty){
            return dependenciesIndex + "\n" + loadMainResources;
        } else if(!indexIsEmpty){
            return dependenciesIndex;
        } else if(!resourceIsEmpty){
            return loadMainResources;
        } else {
            return "";
        }
    }

    protected String getDependenciesIndex() throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(RESOURCES_DEFINE_DEPENDENCIES).append("\n");
        Set<String> libIndex = getDependenciesIndexSet();
        for (String index : libIndex) {
            stringBuilder.append(index).append("\n");
        }
        return stringBuilder.toString();
    }

    protected String getLoadMainResources(){
        LoadMainResourcePattern loadMainResourcePattern = repackageMojo.getLoadMainResourcePattern();
        if(loadMainResourcePattern == null){
            return null;
        }
        String[] includes = loadMainResourcePattern.getIncludes();
        String[] excludes = loadMainResourcePattern.getExcludes();
        StringBuilder stringBuilder = new StringBuilder();
        addLoadMainResources(stringBuilder, RESOURCES_DEFINE_LOAD_MAIN_INCLUDES, includes);
        addLoadMainResources(stringBuilder, RESOURCES_DEFINE_LOAD_MAIN_EXCLUDES, excludes);
        return stringBuilder.toString();
    }

    protected Set<String> getDependenciesIndexSet() throws Exception {
        Set<Artifact> dependencies = repackageMojo.getFilterDependencies();
        Set<String> libPaths = new HashSet<>(dependencies.size());
        for (Artifact artifact : dependencies) {
            if(filterArtifact(artifact)){
                continue;
            }
            libPaths.add(getLibIndex(artifact));
        }
        return libPaths;
    }

    protected String getLibIndex(Artifact artifact){
        return artifact.getFile().getPath() + repackageMojo.resolveLoadToMain(artifact);
    }

    private void addLoadMainResources(StringBuilder stringBuilder, String header, String[] patterns){
        if(ObjectUtils.isEmpty(patterns)){
           return;
        }
        Set<String> patternSet = new HashSet<>(Arrays.asList(patterns));
        stringBuilder.append(header).append("\n");
        for (String patternStr : patternSet) {
            if(ObjectUtils.isEmpty(patternStr)){
                continue;
            }
            stringBuilder.append(resolvePattern(patternStr)).append("\n");
        }
    }

    protected String resolvePattern(String patternStr){
        return patternStr.replace(".", "/");
    }

    /**
     * 过滤Artifact
     * @param artifact Artifact
     * @return 返回true表示被过滤掉
     */
    protected boolean filterArtifact(Artifact artifact){
        return Constant.filterMainTypeArtifact(artifact) ||
                Constant.filterArtifact(artifact, repackageMojo.getIncludeSystemScope());
    }


}