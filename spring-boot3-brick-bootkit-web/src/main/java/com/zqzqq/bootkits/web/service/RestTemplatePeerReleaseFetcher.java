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

import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.dto.ReleaseRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * 基于 {@link RestTemplate} 的跨节点发布记录拉取实现。
 *
 * @author brick-bootkit
 * @since 4.2.0
 */
@Slf4j
@Component
public class RestTemplatePeerReleaseFetcher implements PeerReleaseFetcher {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public List<ReleaseRecord> fetch(String baseUrl, int limit, String token) {
        try {
            String url = baseUrl.replaceAll("/+$", "") + "/plugins-web/api/releases/peer?limit=" + limit;
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            if (token != null && !token.isEmpty()) {
                headers.set("X-Cluster-Token", token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<ApiResult<List<ReleaseRecord>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<ApiResult<List<ReleaseRecord>>>() {
                    });
            ApiResult<List<ReleaseRecord>> body = response.getBody();
            if (body != null && body.isSuccess() && body.getData() != null) {
                return body.getData();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("跨节点拉取发布记录失败: baseUrl={}", baseUrl, e);
            return Collections.emptyList();
        }
    }
}
