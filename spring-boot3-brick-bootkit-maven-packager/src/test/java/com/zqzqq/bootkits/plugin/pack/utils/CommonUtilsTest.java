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


package com.zqzqq.bootkits.plugin.pack.utils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归 {@link CommonUtils#isPluginFrameworkLoader(Artifact)}:
 * 历史 bug 是 groupId 常量写成了 com.zqzqq.bootkits, 但实际发布的 groupId 是 com.zqzqq,
 * 导致 prod 打包时 loader 永远识别不到, 启动报找不到 SpringMainProdBootstrap。
 */
class CommonUtilsTest {

    @Test
    void shouldRecognizeRealLoaderCoordinates() {
        Artifact loaderArtifact = createArtifact(
                CommonUtils.PLUGIN_FRAMEWORK_GROUP_ID,
                CommonUtils.PLUGIN_FRAMEWORK_LOADER_ARTIFACT_ID);

        assertThat(CommonUtils.isPluginFrameworkLoader(loaderArtifact)).isTrue();
    }

    @Test
    void shouldRecognizeRealFrameworkCoordinates() {
        Artifact frameworkArtifact = createArtifact(
                CommonUtils.PLUGIN_FRAMEWORK_GROUP_ID,
                CommonUtils.PLUGIN_FRAMEWORK_ARTIFACT_ID);

        assertThat(CommonUtils.isPluginFramework(frameworkArtifact)).isTrue();
    }

    @Test
    void shouldNotRecognizeUnrelatedArtifact() {
        Artifact unrelated = createArtifact("org.springframework", "spring-core");

        assertThat(CommonUtils.isPluginFrameworkLoader(unrelated)).isFalse();
        assertThat(CommonUtils.isPluginFramework(unrelated)).isFalse();
    }

    @Test
    void shouldDefaultGroupIdToComZqzqq() {
        // 防止再次回退到错误的 com.zqzqq.bootkits
        assertThat(CommonUtils.PLUGIN_FRAMEWORK_GROUP_ID).isEqualTo("com.zqzqq");
    }

    private Artifact createArtifact(String groupId, String artifactId) {
        return new DefaultArtifact(
                groupId,
                artifactId,
                VersionRange.createFromVersion("4.0.9"),
                "compile",
                "jar",
                null,
                new DefaultArtifactHandler("jar"));
    }
}
