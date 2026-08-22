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

package com.zqzqq.bootkits.springboot.starter.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrickBootkitPropertiesTest {

    @Test
    void defaults() {
        BrickBootkitProperties p = new BrickBootkitProperties();
        assertTrue(p.isEnabled());
        assertEquals("./plugins", p.getPluginPath());
        assertTrue(p.isAutoDiscover());
        assertTrue(p.isEnableEventBus());
        assertTrue(p.isEnableHealthCheck());
        assertTrue(p.isEnableAutoRecovery());
        assertEquals(10, p.getEventBusThreadPoolSize());
        assertEquals(60, p.getHealthCheckIntervalSeconds());
        assertEquals(3, p.getMaxRestartCount());
    }

    @Test
    void setters() {
        BrickBootkitProperties p = new BrickBootkitProperties();
        p.setEnabled(false);
        p.setPluginPath("/x");
        p.setEventBusThreadPoolSize(5);
        p.setHealthCheckIntervalSeconds(30);
        p.setMaxRestartCount(7);
        assertFalse(p.isEnabled());
        assertEquals("/x", p.getPluginPath());
        assertEquals(5, p.getEventBusThreadPoolSize());
        assertEquals(30, p.getHealthCheckIntervalSeconds());
        assertEquals(7, p.getMaxRestartCount());
    }
}
