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
