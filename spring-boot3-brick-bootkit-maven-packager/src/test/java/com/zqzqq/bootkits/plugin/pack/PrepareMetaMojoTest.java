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


package com.zqzqq.bootkits.plugin.pack;

import com.zqzqq.bootkits.common.Constants;
import com.zqzqq.bootkits.common.PackageStructure;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrepareMetaMojoTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldPrepareMetaFilesForStandaloneRun() throws Exception {
        Path buildDir = Files.createDirectories(tempDir.resolve("target"));
        Path classesDir = Files.createDirectories(buildDir.resolve("classes"));
        Path fakeSourceJar = buildDir.resolve("source.jar");
        createEmptyJar(fakeSourceJar);

        MavenProject project = createProject(buildDir, classesDir, fakeSourceJar, Collections.emptySet());
        PrepareMetaMojo mojo = createMojo(project, buildDir);

        mojo.execute();

        Path metaDir = buildDir.resolve(PackageStructure.META_INF_NAME);
        Path pluginMeta = metaDir.resolve(PackageStructure.PLUGIN_META_NAME);
        Path resourcesConf = metaDir.resolve(PackageStructure.RESOURCES_DEFINE_NAME);
        Path manifest = metaDir.resolve(PackageStructure.MANIFEST);

        assertTrue(Files.exists(pluginMeta), "PLUGIN.META should exist");
        assertTrue(Files.exists(resourcesConf), "RESOURCES.CONF should exist");
        assertTrue(Files.exists(manifest), "MANIFEST.MF should exist");

        Properties properties = new Properties();
        properties.load(Files.newBufferedReader(pluginMeta, StandardCharsets.UTF_8));
        assertEquals("demo-plugin", properties.getProperty("plugin.id"));
        assertEquals("1.0.0", properties.getProperty("plugin.version"));
        assertEquals("com.example.DemoPluginBootstrap", properties.getProperty("plugin.bootstrapClass"));
        assertEquals(classesDir.toString(), properties.getProperty("plugin.system.path"));
    }

    @Test
    void shouldKeepLoadToMainMarkInResourcesConfig() throws Exception {
        Path buildDir = Files.createDirectories(tempDir.resolve("target-load-main"));
        Path classesDir = Files.createDirectories(buildDir.resolve("classes"));
        Path fakeSourceJar = buildDir.resolve("source.jar");
        createEmptyJar(fakeSourceJar);
        Path dependencyJar = buildDir.resolve("dep-a.jar");
        Files.write(dependencyJar, "dep".getBytes(StandardCharsets.UTF_8));

        Artifact dependencyArtifact = createArtifact("com.example", "dep-a", dependencyJar.toFile());
        Set<Artifact> artifacts = new HashSet<>();
        artifacts.add(dependencyArtifact);

        MavenProject project = createProject(buildDir, classesDir, fakeSourceJar, artifacts);
        PrepareMetaMojo mojo = createMojo(project, buildDir);

        LoadToMain loadToMain = new LoadToMain();
        List<Dependency> dependencies = new ArrayList<>();
        Dependency dependency = new Dependency();
        dependency.setGroupId("com.example");
        dependency.setArtifactId("dep-a");
        dependencies.add(dependency);
        loadToMain.setDependencies(dependencies);
        setField(RepackageMojo.class, mojo, "loadToMain", loadToMain);

        mojo.execute();

        Path resourcesConf = buildDir.resolve(PackageStructure.META_INF_NAME)
                .resolve(PackageStructure.RESOURCES_DEFINE_NAME);
        String content = Files.readString(resourcesConf);
        assertTrue(content.contains(dependencyJar.toString() + Constants.LOAD_TO_MAIN_SIGN));
    }

    private PrepareMetaMojo createMojo(MavenProject project, Path buildDir) {
        PrepareMetaMojo mojo = new PrepareMetaMojo();
        mojo.setProject(project);
        mojo.setOutputDirectory(buildDir.toFile());
        mojo.setMode("prod");
        mojo.setSkip(false);
        mojo.setIncludeSystemScope(Boolean.TRUE);

        PluginInfo pluginInfo = new PluginInfo();
        pluginInfo.setId("demo-plugin");
        pluginInfo.setVersion("1.0.0");
        pluginInfo.setBootstrapClass("com.example.DemoPluginBootstrap");
        mojo.setPluginInfo(pluginInfo);
        return mojo;
    }

    private MavenProject createProject(Path buildDir,
                                       Path classesDir,
                                       Path sourceArtifactPath,
                                       Set<Artifact> artifacts) {
        MavenProject project = new MavenProject();
        project.setPackaging("jar");
        Build build = new Build();
        build.setDirectory(buildDir.toString());
        build.setOutputDirectory(classesDir.toString());
        project.setBuild(build);
        project.setArtifact(createArtifact("com.example", "demo-plugin", sourceArtifactPath.toFile()));
        project.setArtifacts(artifacts);
        return project;
    }

    private Artifact createArtifact(String groupId, String artifactId, File file) {
        DefaultArtifact artifact = new DefaultArtifact(
                groupId,
                artifactId,
                VersionRange.createFromVersion("1.0.0"),
                "compile",
                "jar",
                null,
                new DefaultArtifactHandler("jar")
        );
        artifact.setFile(file);
        return artifact;
    }

    private void setField(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field: " + fieldName, e);
        }
    }

    private void createEmptyJar(Path jarPath) throws Exception {
        try (OutputStream outputStream = Files.newOutputStream(jarPath);
             JarOutputStream ignored = new JarOutputStream(outputStream)) {
            // Create an empty but valid jar file for source artifact simulation.
        }
    }
}
