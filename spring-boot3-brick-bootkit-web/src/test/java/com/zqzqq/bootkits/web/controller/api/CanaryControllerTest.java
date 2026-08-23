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

package com.zqzqq.bootkits.web.controller.api;

import com.zqzqq.bootkits.core.communication.ServiceRoutingGroup;
import com.zqzqq.bootkits.web.auth.PluginWebAuthorizationService;
import com.zqzqq.bootkits.web.auth.PluginWebPermission;
import com.zqzqq.bootkits.web.dto.ApiResult;
import com.zqzqq.bootkits.web.service.CanaryWebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CanaryController Test")
class CanaryControllerTest {

    private CanaryWebService canaryWebService;
    private PluginWebAuthorizationService authorizationService;
    private CanaryController controller;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        canaryWebService = mock(CanaryWebService.class);
        authorizationService = mock(PluginWebAuthorizationService.class);
        ObjectProvider<CanaryWebService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(canaryWebService);
        controller = new CanaryController(provider, authorizationService);
    }

    @Test
    @DisplayName("路由分组查询返回权重视图")
    void routingShouldReturnGroups() {
        ServiceRoutingGroup group = new ServiceRoutingGroup();
        group.setInterfaceName("Sample");
        when(canaryWebService.describeRouting()).thenReturn(Collections.singletonList(group));

        ApiResult<List<ServiceRoutingGroup>> result = controller.routing();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        verify(authorizationService).check(PluginWebPermission.PLUGIN_HISTORY_READ, null);
    }

    @Test
    @DisplayName("权重更新调用服务")
    void updateWeightShouldInvokeService() {
        CanaryController.WeightUpdateRequest request = new CanaryController.WeightUpdateRequest();
        request.setPluginId("plugin-b");
        request.setInterfaceName("com.example.Sample");
        request.setWeight(10);

        ApiResult<Void> result = controller.updateWeight(request);

        assertThat(result.isSuccess()).isTrue();
        verify(canaryWebService).updateWeight("plugin-b", "com.example.Sample", 10);
        verify(authorizationService).check(PluginWebPermission.PLUGIN_HISTORY_WRITE, null);
    }
}
