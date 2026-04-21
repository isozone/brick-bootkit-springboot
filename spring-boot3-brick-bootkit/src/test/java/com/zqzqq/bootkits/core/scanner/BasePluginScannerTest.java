package com.zqzqq.bootkits.core.scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BasePluginScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldScanAllPluginArchivesInProdDirectory() throws IOException {
        Path pluginsDir = Files.createDirectory(tempDir.resolve("plugins"));
        Path firstJar = Files.createFile(pluginsDir.resolve("eqLicensePlugins-0.0.1-repackage.jar"));
        Path secondJar = Files.createFile(pluginsDir.resolve("wxminiOps-0.0.1-repackage.jar"));

        BasePluginScanner scanner = new BasePluginScanner(new ProdPathResolve(), null);

        List<Path> pluginPaths = scanner.scan(List.of(pluginsDir.toString()));

        assertThat(pluginPaths)
                .containsExactlyInAnyOrder(firstJar, secondJar);
    }
}
