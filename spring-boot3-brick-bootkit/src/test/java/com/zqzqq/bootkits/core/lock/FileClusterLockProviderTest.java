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
