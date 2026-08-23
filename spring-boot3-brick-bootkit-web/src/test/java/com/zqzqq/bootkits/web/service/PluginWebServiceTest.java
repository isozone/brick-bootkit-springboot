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


package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import com.zqzqq.bootkits.web.dto.ApiResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PluginWebServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldStoreUploadedPluginUnderManagedTempDirectory() throws Exception {
        PluginWebService service = createService(tempDir.resolve("upload-temp"), tempDir.resolve("plugins"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "demo-plugin.jar", "application/java-archive", "demo".getBytes());

        ApiResult<String> result = service.uploadPluginTemp(file);

        Path uploadedPath = Path.of(result.getData()).toAbsolutePath().normalize();
        assertThat(result.isSuccess()).isTrue();
        assertThat(uploadedPath.startsWith(tempDir.resolve("upload-temp").toAbsolutePath().normalize())).isTrue();
        assertThat(uploadedPath.getFileName().toString()).isEqualTo("demo-plugin.jar");
        assertThat(Files.exists(uploadedPath)).isTrue();
        assertThat(uploadedPath.getParent()).isNotEqualTo(tempDir.resolve("upload-temp").toAbsolutePath().normalize());
    }

    @Test
    void shouldRejectUnsafeUploadFilename() {
        PluginWebService service = createService(tempDir.resolve("upload-temp"), tempDir.resolve("plugins"));
        MockMultipartFile file = new MockMultipartFile(
                "file", "../demo-plugin.jar", "application/java-archive", "demo".getBytes());

        assertThatThrownBy(() -> service.uploadPluginTemp(file))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("上传文件名不合法");
    }

    @Test
    void shouldRejectPathsOutsideManagedDirectories() throws Exception {
        PluginManager pluginManager = mock(PluginManager.class);
        PluginWebService service = createService(tempDir.resolve("upload-temp"), tempDir.resolve("plugins"), pluginManager);
        Path externalPath = Files.createTempFile("external-plugin", ".jar");

        assertThatThrownBy(() -> service.installPlugin(externalPath))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("仅允许访问插件上传目录或插件根目录中的文件");

        assertThat(service.verifyPlugin(externalPath)).isFalse();
        verifyNoInteractions(pluginManager);
    }

    private PluginWebService createService(Path uploadTempPath, Path pluginRootPath) {
        return createService(uploadTempPath, pluginRootPath, mock(PluginManager.class));
    }

    private PluginWebService createService(Path uploadTempPath, Path pluginRootPath, PluginManager pluginManager) {
        BrickWebProperties properties = new BrickWebProperties();
        properties.setUploadTempPath(uploadTempPath.toString());
        properties.setPluginPaths(List.of(pluginRootPath.toString()));

        @SuppressWarnings("unchecked")
        ObjectProvider<PluginManager> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(pluginManager);

        ReleaseService releaseService = mock(ReleaseService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RolloutWebService> rolloutProvider = mock(ObjectProvider.class);
        when(rolloutProvider.getIfAvailable()).thenReturn(null);

        return new PluginWebService(provider, properties, releaseService, rolloutProvider);
    }
}
