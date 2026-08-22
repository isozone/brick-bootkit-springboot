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


package com.zqzqq.bootkits.bootstrap;

import com.zqzqq.bootkits.common.PackageStructure;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PluginOneselfInteractive tests for standalone fallback behavior.
 */
class PluginOneselfInteractiveTest {

    private static final String[] STANDALONE_KEYS = new String[]{
            "plugin.standalone.id",
            "plugin.standalone.version",
            "plugin.standalone.bootstrapClass",
            "plugin.standalone.description",
            "plugin.standalone.provider",
            "plugin.standalone.requires",
            "plugin.standalone.license",
            "plugin.standalone.args",
            "plugin.standalone.configFileName",
            "plugin.standalone.configFileLocation"
    };

    @AfterEach
    void cleanup() throws Exception {
        for (String key : STANDALONE_KEYS) {
            System.clearProperty(key);
        }
        Path pluginMeta = resolveTestPluginMetaPath();
        Files.deleteIfExists(pluginMeta);
    }

    @Test
    void shouldFallbackWhenPluginMetaMissing() {
        PluginOneselfInteractive interactive = new PluginOneselfInteractive(StandaloneBootstrap.class);
        InsidePluginDescriptor descriptor = interactive.getPluginDescriptor();

        assertEquals("standalone-bootstrap-plugin", descriptor.getPluginId());
        assertEquals("0.0.0-standalone", descriptor.getPluginVersion());
        assertEquals(StandaloneBootstrap.class.getName(), descriptor.getPluginBootstrapClass());
        assertEquals(PluginType.DEV, descriptor.getType());
        assertEquals("classes", descriptor.getPluginClassPath());
    }

    @Test
    void shouldUseStandaloneSystemPropertiesForFallback() {
        System.setProperty("plugin.standalone.id", "demo-standalone");
        System.setProperty("plugin.standalone.version", "1.2.3");
        System.setProperty("plugin.standalone.bootstrapClass", "com.example.CustomBootstrap");

        PluginOneselfInteractive interactive = new PluginOneselfInteractive(StandaloneBootstrap.class);
        InsidePluginDescriptor descriptor = interactive.getPluginDescriptor();

        assertEquals("demo-standalone", descriptor.getPluginId());
        assertEquals("1.2.3", descriptor.getPluginVersion());
        assertEquals("com.example.CustomBootstrap", descriptor.getPluginBootstrapClass());
    }

    @Test
    void shouldPreferPluginMetaWhenExists() throws Exception {
        Path pluginMeta = resolveTestPluginMetaPath();
        Files.createDirectories(pluginMeta.getParent());

        Properties properties = new Properties();
        properties.setProperty("plugin.id", "meta-plugin");
        properties.setProperty("plugin.version", "9.9.9");
        properties.setProperty("plugin.bootstrapClass", StandaloneBootstrap.class.getName());
        properties.setProperty("plugin.system.path", "classes");
        properties.setProperty("plugin.system.resourcesConfig", "non-existent-resources.list");
        try (Writer writer = Files.newBufferedWriter(pluginMeta, StandardCharsets.UTF_8)) {
            properties.store(writer, "test");
        }

        PluginOneselfInteractive interactive = new PluginOneselfInteractive(StandaloneBootstrap.class);
        InsidePluginDescriptor descriptor = interactive.getPluginDescriptor();

        assertEquals("meta-plugin", descriptor.getPluginId());
        assertEquals("9.9.9", descriptor.getPluginVersion());
    }

    private static Path resolveTestPluginMetaPath() throws Exception {
        URL classesUrl = StandaloneBootstrap.class.getResource("/");
        assertNotNull(classesUrl, "Test classes path not found");
        Path targetPath = Paths.get(classesUrl.toURI()).getParent();
        Path metaInfPath = Paths.get(targetPath.toString(), PackageStructure.META_INF_NAME);
        return metaInfPath.resolve(PackageStructure.PLUGIN_META_NAME);
    }

    static class StandaloneBootstrap {
    }
}
