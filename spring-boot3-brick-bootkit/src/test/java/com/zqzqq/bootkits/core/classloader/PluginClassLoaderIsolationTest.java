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

package com.zqzqq.bootkits.core.classloader;

import com.zqzqq.bootkits.loader.classloader.GenericClassLoader;
import com.zqzqq.bootkits.loader.classloader.resource.Resource;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoader;
import com.zqzqq.bootkits.loader.classloader.resource.loader.ResourceLoaderFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PluginClassLoaderIsolationTest {

    @Test
    void shouldBlockParentSpringAutoConfigurationMetadataResources() throws Exception {
        URL allowedUrl = new URL("file:/allowed-resource.txt");

        Map<String, URL> resourceUrlMap = new HashMap<>();
        resourceUrlMap.put("META-INF/spring.factories", new URL("file:/spring.factories"));
        resourceUrlMap.put("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
                new URL("file:/auto-configuration.imports"));
        resourceUrlMap.put("META-INF/spring/com.example.Custom.imports", new URL("file:/custom.imports"));
        resourceUrlMap.put("META-INF/spring/com.example.Custom.replacements", new URL("file:/custom.replacements"));
        resourceUrlMap.put("com/example/Allowed.txt", allowedUrl);

        Map<String, byte[]> streamMap = new HashMap<>();
        streamMap.put("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports",
                "blocked".getBytes());
        streamMap.put("com/example/Allowed.txt", "allowed".getBytes());

        ParentResourceClassLoader parentResourceClassLoader = new ParentResourceClassLoader(resourceUrlMap, streamMap);

        try (GenericClassLoader parentGenericClassLoader =
                     new GenericClassLoader("parent-test", new NoOpResourceLoaderFactory());
             PluginClassLoader pluginClassLoader =
                     new PluginClassLoader("plugin-test", parentGenericClassLoader, parentResourceClassLoader,
                             new NoOpResourceLoaderFactory(), resourceUrl -> Boolean.TRUE)) {

            assertThat(pluginClassLoader.findResourceFromParent("META-INF/spring.factories")).isNull();
            assertThat(pluginClassLoader.findResourceFromParent(
                    "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")).isNull();
            assertThat(pluginClassLoader.findResourceFromParent(
                    "/META-INF/spring/com.example.Custom.replacements")).isNull();

            assertThat(pluginClassLoader.findInputStreamFromParent(
                    "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")).isNull();

            assertThat(pluginClassLoader.findResourcesFromParent("META-INF/spring/com.example.Custom.imports")).isNull();

            assertThat(pluginClassLoader.findResourceFromParent("com/example/Allowed.txt")).isEqualTo(allowedUrl);
            assertThat(pluginClassLoader.findInputStreamFromParent("com/example/Allowed.txt")).isNotNull();
            Enumeration<URL> allowedResources = pluginClassLoader.findResourcesFromParent("com/example/Allowed.txt");
            assertThat(allowedResources).isNotNull();
            assertThat(allowedResources.hasMoreElements()).isTrue();
            assertThat(allowedResources.nextElement()).isEqualTo(allowedUrl);
        }
    }

    private static class ParentResourceClassLoader extends ClassLoader {

        private final Map<String, URL> resourceUrlMap;
        private final Map<String, byte[]> streamMap;

        ParentResourceClassLoader(Map<String, URL> resourceUrlMap, Map<String, byte[]> streamMap) {
            super(null);
            this.resourceUrlMap = resourceUrlMap;
            this.streamMap = streamMap;
        }

        @Override
        public URL getResource(String name) {
            return resourceUrlMap.get(name);
        }

        @Override
        public Enumeration<URL> getResources(String name) {
            URL url = resourceUrlMap.get(name);
            if (url == null) {
                return Collections.emptyEnumeration();
            }
            return Collections.enumeration(Collections.singletonList(url));
        }

        @Override
        public InputStream getResourceAsStream(String name) {
            byte[] bytes = streamMap.get(name);
            if (bytes == null) {
                return null;
            }
            return new ByteArrayInputStream(bytes);
        }
    }

    private static class NoOpResourceLoaderFactory implements ResourceLoaderFactory {

        @Override
        public void addResource(String path) {
        }

        @Override
        public void addResource(java.io.File file) {
        }

        @Override
        public void addResource(Path path) {
        }

        @Override
        public void addResource(URL url) {
        }

        @Override
        public void addResource(Resource resource) {
        }

        @Override
        public void addResource(ResourceLoader resourceLoader) {
        }

        @Override
        public Resource findFirstResource(String name) {
            return null;
        }

        @Override
        public Enumeration<Resource> findAllResource(String name) {
            return Collections.emptyEnumeration();
        }

        @Override
        public InputStream getInputStream(String name) {
            return null;
        }

        @Override
        public List<URL> getUrls() {
            return Collections.emptyList();
        }

        @Override
        public void close() {
        }

        @Override
        public void release() {
        }
    }
}
