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


package com.zqzqq.bootkits.core.descriptor;

import com.zqzqq.bootkits.common.PackageStructure;
import com.zqzqq.bootkits.core.descriptor.decrypt.EmptyPluginDescriptorDecrypt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归 {@link DevPluginDescriptorLoader}:
 *
 * <p>历史 bug: create() 强制 setPluginClassPath("classes"), 在主服务配置
 * plugin.pluginPath 指向插件根目录时, PluginResourceLoaderFactoryProxy.addDirPluginClasspath
 * 会把 insidePluginPath 与 "classes" 拼成不存在的路径, 导致 "插件xxx未发现Classpath" 启动失败。</p>
 *
 * <p>该测试模拟 pluginPath=./plugins 的布局, 验证 dev 模式下能从 target/classes
 * 正确加载 PLUGIN.META, 且 pluginClassPath 不被强制覆盖。</p>
 */
class DevPluginDescriptorLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadFromMavenTargetClassesLayout() throws Exception {
        // 模拟 plugins/plugin2/target/classes/META-INF/PLUGIN.META
        Path pluginRoot = tempDir.resolve("plugins/plugin2");
        Path metaDir = pluginRoot.resolve("target/classes/" + PackageStructure.META_INF_NAME);
        Files.createDirectories(metaDir);
        writePluginMeta(metaDir.resolve(PackageStructure.PLUGIN_META_NAME), "target/classes");

        DevPluginDescriptorLoader loader = new DevPluginDescriptorLoader(new EmptyPluginDescriptorDecrypt());

        InsidePluginDescriptor descriptor = loader.load(pluginRoot);

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.getPluginId()).isEqualTo("plugin2");
        // 关键断言: pluginClassPath 应沿用 plugin.system.path 读出的 target/classes, 而非被强制设为 "classes"
        assertThat(descriptor.getPluginClassPath()).isEqualTo("target/classes");
    }

    @Test
    void shouldLoadFromGradleBuildClassesLayout() throws Exception {
        // 模拟 plugins/plugin2/build/classes/java/main/META-INF/PLUGIN.META
        Path pluginRoot = tempDir.resolve("plugins/plugin2");
        Path metaDir = pluginRoot.resolve("build/classes/java/main/" + PackageStructure.META_INF_NAME);
        Files.createDirectories(metaDir);
        writePluginMeta(metaDir.resolve(PackageStructure.PLUGIN_META_NAME), "build/classes/java/main");

        DevPluginDescriptorLoader loader = new DevPluginDescriptorLoader(new EmptyPluginDescriptorDecrypt());

        InsidePluginDescriptor descriptor = loader.load(pluginRoot);

        assertThat(descriptor).isNotNull();
        assertThat(descriptor.getPluginId()).isEqualTo("plugin2");
        assertThat(descriptor.getPluginClassPath()).isEqualTo("build/classes/java/main");
    }

    @Test
    void shouldReturnNullWhenNoMetaFound() throws Exception {
        Path pluginRoot = tempDir.resolve("plugins/emptyPlugin");
        Files.createDirectories(pluginRoot);

        DevPluginDescriptorLoader loader = new DevPluginDescriptorLoader(new EmptyPluginDescriptorDecrypt());

        InsidePluginDescriptor descriptor = loader.load(pluginRoot);

        assertThat(descriptor).isNull();
    }

    @Test
    void shouldFallbackToClassesWhenPluginSystemPathAbsent() throws Exception {
        // plugin.system.path 为空字符串时, super.create 读出空, 触发 CLASSES_NAME 回退
        Path pluginRoot = tempDir.resolve("plugins/plugin3");
        Path metaDir = pluginRoot.resolve("target/classes/" + PackageStructure.META_INF_NAME);
        Files.createDirectories(metaDir);
        Properties props = baseProps("plugin3");
        props.setProperty("plugin.system.resourcesConfig", "dependencies.index");
        // 显式置空 plugin.system.path
        props.setProperty("plugin.system.path", "");
        try (OutputStream os = Files.newOutputStream(metaDir.resolve(PackageStructure.PLUGIN_META_NAME))) {
            props.store(os, null);
        }

        DevPluginDescriptorLoader loader = new DevPluginDescriptorLoader(new EmptyPluginDescriptorDecrypt());

        InsidePluginDescriptor descriptor = loader.load(pluginRoot);

        assertThat(descriptor).isNotNull();
        // 空值时回退到 PackageStructure.CLASSES_NAME
        assertThat(descriptor.getPluginClassPath()).isEqualTo(PackageStructure.CLASSES_NAME);
    }

    private void writePluginMeta(Path metaFile, String pluginSystemPath) throws Exception {
        Properties props = baseProps("plugin2");
        props.setProperty("plugin.system.path", pluginSystemPath);
        props.setProperty("plugin.system.resourcesConfig", "dependencies.index");
        try (OutputStream os = Files.newOutputStream(metaFile)) {
            props.store(os, null);
        }
    }

    private Properties baseProps(String pluginId) {
        Properties props = new Properties();
        props.setProperty("plugin.id", pluginId);
        props.setProperty("plugin.version", "1.0.0");
        props.setProperty("plugin.bootstrapClass", "com.example.Bootstrap");
        return props;
    }
}
