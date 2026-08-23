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

import java.util.List;

/**
 * 集群内节点间发布记录拉取器。抽象为接口以便于单元测试桩替与替换传输实现。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
public interface PeerReleaseFetcher {

    /**
     * 从对端节点拉取发布记录。
     *
     * @param baseUrl 对端 Web 基址（如 http://host:port/context）
     * @param limit   拉取条数
     * @param token   集群内部令牌
     * @return 发布记录（拉取失败返回空列表，不抛异常）
     */
    List<ReleaseRecord> fetch(String baseUrl, int limit, String token);
}
