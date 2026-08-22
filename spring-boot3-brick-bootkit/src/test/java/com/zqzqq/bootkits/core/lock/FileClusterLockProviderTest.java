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


package com.zqzqq.bootkits.core.lock;

import com.zqzqq.bootkits.core.exception.PluginException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileClusterLockProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldTimeoutWhenLockIsHeld() {
        FileClusterLockProvider provider = new FileClusterLockProvider(tempDir);

        try (ClusterLock ignored = provider.acquire("plugin:lifecycle:test", Duration.ofSeconds(2))) {
            assertThatThrownBy(() -> provider.acquire("plugin:lifecycle:test", Duration.ofMillis(200)))
                    .isInstanceOf(PluginException.class)
                    .hasMessageContaining("timeout");
        }
    }
}
