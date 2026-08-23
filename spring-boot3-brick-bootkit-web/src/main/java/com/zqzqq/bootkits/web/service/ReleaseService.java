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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqzqq.bootkits.web.dto.ReleaseRecord;

import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * 发布记录服务（发布治理审计存储）。
 *
 * <p>采用与 {@code UploadHistoryService} 一致的「内存 + JSON 文件」存储策略，
 * 进程内保留最近 {@link #MAX_RECORDS} 条发布记录，并落地到
 * {@code ./release-history/release-history.json} 以便重启后可追溯。</p>
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Service
public class ReleaseService {

    private static final int MAX_RECORDS = 1000;

    private final List<ReleaseRecord> records = new ArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final File storeFile = new File("./release-history/release-history.json");

    public ReleaseService() {
        load();
    }

    private synchronized void load() {
        if (!storeFile.exists()) {
            return;
        }
        try {
            List<ReleaseRecord> list = objectMapper.readValue(storeFile,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, ReleaseRecord.class));
            if (list != null) {
                records.addAll(list);
            }
        } catch (Exception e) {
            // 历史文件损坏不影响启动，忽略并以空记录启动
        }
    }

    private synchronized void persist() {
        try {
            if (storeFile.getParentFile() != null) {
                storeFile.getParentFile().mkdirs();
            }
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(storeFile, new ArrayList<>(records));
        } catch (Exception e) {
            // 持久化失败仅告警，不阻断发布主流程
        }
    }

    /**
     * 创建一条发布记录
     */
    public ReleaseRecord create(ReleaseRecord record) {
        synchronized (records) {
            records.add(record);
            if (records.size() > MAX_RECORDS) {
                records.remove(0);
            }
        }
        persist();
        return record;
    }

    /**
     * 按 releaseId 更新发布记录（如状态流转、补充错误信息）
     */
    public void update(String releaseId, Consumer<ReleaseRecord> updater) {
        ReleaseRecord target = null;
        synchronized (records) {
            for (ReleaseRecord r : records) {
                if (r.getReleaseId() != null && r.getReleaseId().equals(releaseId)) {
                    target = r;
                    break;
                }
            }
        }
        if (target != null) {
            updater.accept(target);
            persist();
        }
    }

    public ReleaseRecord get(String releaseId) {
        synchronized (records) {
            for (ReleaseRecord r : records) {
                if (r.getReleaseId() != null && r.getReleaseId().equals(releaseId)) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * 返回最近的发布记录（按开始时间倒序），最多 limit 条
     */
    public List<ReleaseRecord> list(int limit) {
        List<ReleaseRecord> snapshot;
        synchronized (records) {
            snapshot = new ArrayList<>(records);
        }
        snapshot.sort(Comparator.comparingLong(ReleaseRecord::getStartTime).reversed());
        if (limit <= 0 || limit >= snapshot.size()) {
            return snapshot;
        }
        return new ArrayList<>(snapshot.subList(0, limit));
    }

    /**
     * 按插件 ID 过滤最近的发布记录
     */
    public List<ReleaseRecord> listByPlugin(String pluginId, int limit) {
        List<ReleaseRecord> result = new ArrayList<>();
        synchronized (records) {
            for (ReleaseRecord r : records) {
                if (r.getPluginId() != null && r.getPluginId().equals(pluginId)) {
                    result.add(r);
                }
            }
        }
        result.sort(Comparator.comparingLong(ReleaseRecord::getStartTime).reversed());
        if (limit <= 0 || limit >= result.size()) {
            return result;
        }
        return new ArrayList<>(result.subList(0, limit));
    }

    public void remove(String releaseId) {
        synchronized (records) {
            records.removeIf(r -> r.getReleaseId() != null && r.getReleaseId().equals(releaseId));
        }
        persist();
    }
}
