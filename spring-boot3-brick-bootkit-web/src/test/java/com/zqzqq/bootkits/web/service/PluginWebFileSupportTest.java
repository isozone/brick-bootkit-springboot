package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginWebFileSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateBackupUnderPluginsBackupDirectory() throws Exception {
        Path pluginRoot = tempDir.resolve("plugins");
        Files.createDirectories(pluginRoot);
        Path pluginFile = pluginRoot.resolve("demo-plugin.jar");
        Files.writeString(pluginFile, "demo");

        PluginWebFileSupport fileSupport = createFileSupport();

        Path backupPath = fileSupport.backupPluginFile(pluginRoot, pluginFile);

        assertThat(backupPath).exists();
        assertThat(backupPath.getParent().getFileName().toString()).isEqualTo("demo-plugin");
        assertThat(backupPath.toString()).contains("plugins-backup");
    }

    @Test
    void shouldRejectAccessOutsideManagedPluginDirectories() throws Exception {
        Path pluginRoot = tempDir.resolve("plugins");
        Files.createDirectories(pluginRoot);
        Path externalFile = Files.createTempFile("outside-plugin", ".jar");

        PluginWebFileSupport fileSupport = createFileSupport();

        assertThatThrownBy(() -> fileSupport.resolveManagedPluginPath(externalFile))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("仅允许访问插件上传目录或插件根目录中的文件");
    }

    private PluginWebFileSupport createFileSupport() {
        BrickWebProperties properties = new BrickWebProperties();
        properties.setUploadTempPath(tempDir.resolve("upload-temp").toString());
        properties.setPluginPaths(List.of(tempDir.resolve("plugins").toString()));
        return new PluginWebFileSupport(properties);
    }
}
