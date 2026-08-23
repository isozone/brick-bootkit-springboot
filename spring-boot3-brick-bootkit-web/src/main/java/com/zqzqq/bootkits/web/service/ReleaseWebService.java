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
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 发布治理 Web 服务：对外提供发布记录的查询与删除能力。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Service
public class ReleaseWebService {

    private final ReleaseService releaseService;

    public ReleaseWebService(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    public List<ReleaseRecord> listReleases(int limit) {
        return releaseService.list(limit <= 0 ? 50 : limit);
    }

    public List<ReleaseRecord> listReleasesByPlugin(String pluginId, int limit) {
        return releaseService.listByPlugin(pluginId, limit);
    }

    public ReleaseRecord getRelease(String releaseId) {
        return releaseService.get(releaseId);
    }

    public void removeRelease(String releaseId) {
        releaseService.remove(releaseId);
    }
}
