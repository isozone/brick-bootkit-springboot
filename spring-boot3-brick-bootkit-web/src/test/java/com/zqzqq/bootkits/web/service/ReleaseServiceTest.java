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

import com.zqzqq.bootkits.web.dto.ReleaseRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseServiceTest {

    private final ReleaseService service = new ReleaseService();

    @Test
    void createThenGet() {
        ReleaseRecord r = new ReleaseRecord();
        r.setReleaseId("rs-" + UUID.randomUUID());
        r.setPluginId("rs-plugin-" + UUID.randomUUID());
        r.setStatus("UPGRADING");
        service.create(r);

        assertThat(service.get(r.getReleaseId())).isNotNull();
        assertThat(service.get(r.getReleaseId()).getStatus()).isEqualTo("UPGRADING");
    }

    @Test
    void updateChangesStatus() {
        String id = "rs-" + UUID.randomUUID();
        ReleaseRecord r = new ReleaseRecord();
        r.setReleaseId(id);
        r.setPluginId("rs-plugin-" + UUID.randomUUID());
        service.create(r);

        service.update(id, x -> x.setStatus("SUCCESS"));
        assertThat(service.get(id).getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void removeMakesGetNull() {
        String id = "rs-" + UUID.randomUUID();
        ReleaseRecord r = new ReleaseRecord();
        r.setReleaseId(id);
        r.setPluginId("rs-plugin-" + UUID.randomUUID());
        service.create(r);

        service.remove(id);
        assertThat(service.get(id)).isNull();
    }

    @Test
    void listByPluginContainsCreated() {
        String pluginId = "rs-plugin-" + UUID.randomUUID();
        ReleaseRecord r = new ReleaseRecord();
        r.setReleaseId("rs-" + UUID.randomUUID());
        r.setPluginId(pluginId);
        service.create(r);

        List<ReleaseRecord> list = service.listByPlugin(pluginId, 10);
        assertThat(list).anyMatch(x -> pluginId.equals(x.getPluginId()));
    }
}
