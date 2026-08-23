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

import com.zqzqq.bootkits.integration.cluster.ClusterNodeInfo;
import lombok.Data;

import java.util.List;

/**
 * 集群发布聚合视图。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Data
public class ClusterReleases {

    /** 集群功能是否启用 */
    private boolean clusterEnabled;

    /** 当前节点 ID */
    private String currentNodeId;

    /** 在线节点清单 */
    private List<ClusterNodeInfo> nodes;

    /** 本节点发布记录（已按所属节点标记） */
    private List<ReleaseRecord> releases;
}
