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

package com.zqzqq.bootkits.plugin.pack.utils;

import com.zqzqq.bootkits.plugin.pack.filter.Exclude;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.jar.JarFile;

/**
 * Object 工具类
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.1
 */
public class CommonUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonUtils.class);
    public final static String PLUGIN_FRAMEWORK_GROUP_ID = "com.zqzqq";
    public final static String PLUGIN_FRAMEWORK_ARTIFACT_ID = "spring-boot3-brick-bootkit";

    public final static String PLUGIN_FRAMEWORK_LOADER_ARTIFACT_ID = "spring-boot3-brick-bootkit-loader";

    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(PATTERN);

    private CommonUtils(){}

    public static Exclude getPluginFrameworkExclude(){
        return Exclude.get(PLUGIN_FRAMEWORK_GROUP_ID, PLUGIN_FRAMEWORK_ARTIFACT_ID);
    }

    public static boolean isPluginFramework(Artifact artifact){
        return Objects.equals(artifact.getGroupId(), PLUGIN_FRAMEWORK_GROUP_ID)
                && Objects.equals(artifact.getArtifactId(), PLUGIN_FRAMEWORK_ARTIFACT_ID);
    }

    public static boolean isPluginFrameworkLoader(Artifact artifact){
        return Objects.equals(artifact.getGroupId(), PLUGIN_FRAMEWORK_GROUP_ID)
                && Objects.equals(artifact.getArtifactId(), PLUGIN_FRAMEWORK_LOADER_ARTIFACT_ID);
    }

    public static JarFile getSourceJarFile(MavenProject mavenProject) {
        File file = mavenProject.getArtifact().getFile();
        try {
            return new JarFile(file);
        } catch (Exception e){
            log.warn("Failed to get source jar file for: {}", mavenProject.getName(), e);
            return null;
        }
    }

    public static String getDateTime() {
        return DATE_TIME_FORMATTER.format(LocalDateTime.now());
    }

    public static void deleteFile(File rootFile) throws MojoFailureException {
        try {
            if(rootFile == null){
                return;
            }
            if(!rootFile.exists()){
                return;
            }
            if(rootFile.isFile()){
                FileUtils.delete(rootFile);
            } else {
                FileUtils.deleteDirectory(rootFile);
            }
        } catch (Exception e){
            log.error("Failed to delete file: {}", rootFile.getPath(), e);
            throw new MojoFailureException("Delete file '" + rootFile.getPath() + "' failure. " + e.getMessage());
        }
    }

}
