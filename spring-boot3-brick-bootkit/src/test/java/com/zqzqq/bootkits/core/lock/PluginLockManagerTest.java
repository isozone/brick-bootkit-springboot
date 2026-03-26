package com.zqzqq.bootkits.core.lock;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PluginLockManagerTest {

    @Test
    void shouldReleaseAllPluginLocksAfterSortedMultiLockExecution() throws Exception {
        PluginLockManager manager = new PluginLockManager();

        manager.executeWithMultiPluginLock(Arrays.asList("plugin-b", "plugin-a"), () -> {
            // no-op
        });

        assertThat(getPluginLocks(manager)).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPluginLocks(PluginLockManager manager) throws Exception {
        Field field = PluginLockManager.class.getDeclaredField("pluginLocks");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(manager);
    }
}
