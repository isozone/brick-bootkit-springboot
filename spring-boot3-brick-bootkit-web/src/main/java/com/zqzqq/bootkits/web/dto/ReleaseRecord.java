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

package com.zqzqq.bootkits.web.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 发布记录（发布治理审计中枢）。
 *
 * <p>一次插件升级对应一条 {@link ReleaseRecord}：记录从哪个版本到哪个版本、
 * 采用的发布模式（直接/灰度）、运行状态以及运行过的灰度探针，用于发布审计与可视化。</p>
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Data
public class ReleaseRecord {

    /** 发布唯一标识 */
    private String releaseId;

    /** 插件 ID */
    private String pluginId;

    /** 插件名称 */
    private String pluginName;

    /** 升级前版本 */
    private String fromVersion;

    /** 升级后版本 */
    private String toVersion;

    /** 发布模式：DIRECT / GRAY */
    private String mode;

    /**
     * 发布状态：
     * INIT（已创建）/ UPGRADING（升级中）/ SUCCESS（成功）/
     * FAILED（失败）/ ROLLED_BACK（已回滚）
     */
    private String status;

    /** 发布开始时间（毫秒） */
    private long startTime;

    /** 发布结束时间（毫秒，进行中为 null） */
    private Long endTime;

    /** 触发者 */
    private String operator;

    /** 本次发布运行过的灰度探针名称 */
    private List<String> probes = new ArrayList<>();

    /** 升级前备份路径（用于回滚） */
    private String backupPath;

    /** 失败原因（成功为 null） */
    private String errorMessage;

    /** 附加说明 */
    private String detail;
}
