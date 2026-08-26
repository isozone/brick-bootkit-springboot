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

package com.zqzqq.bootkits.distributed.registry;

import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NacosServiceDirectoryTest {

    private NamingService namingService;
    private NacosServiceDirectory directory;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String PREFIX = "brick:distributed:services";
    private static final String GROUP = "BRICK_BOOTKIT_DISTRIBUTED";

    @BeforeEach
    void setUp() {
        namingService = mock(NamingService.class);
        directory = new NacosServiceDirectory(namingService, PREFIX, GROUP, 30L, null, mapper);
    }

    private RemoteServiceRegistration sampleReg() {
        return new RemoteServiceRegistration("user-plugin", "com.example.UserService",
                "1.0.0", "node-1", "10.0.0.5", 9090, System.currentTimeMillis());
    }

    @Test
    void registerBuildsNacosInstanceWithMetadata() throws Exception {
        RemoteServiceRegistration reg = sampleReg();
        directory.register(reg);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> groupCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Instance> instCaptor = ArgumentCaptor.forClass(Instance.class);
        verify(namingService).registerInstance(nameCaptor.capture(), groupCaptor.capture(), instCaptor.capture());

        assertEquals(PREFIX + ":svc:com.example.UserService", nameCaptor.getValue());
        assertEquals(GROUP, groupCaptor.getValue());
        Instance inst = instCaptor.getValue();
        assertEquals("10.0.0.5", inst.getIp());
        assertEquals(9090, inst.getPort());
        assertEquals(Boolean.TRUE, inst.isEphemeral());
        Map<String, String> md = inst.getMetadata();
        assertEquals("user-plugin", md.get("pluginId"));
        assertEquals("node-1", md.get("nodeId"));
        assertNotNull(md.get("reg"));
    }

    @Test
    void lookupParsesMetadataIntoRegistration() throws Exception {
        RemoteServiceRegistration reg = sampleReg();
        Instance inst = new Instance();
        inst.setIp(reg.getHost());
        inst.setPort(reg.getPort());
        inst.setMetadata(Collections.singletonMap("reg", mapper.writeValueAsString(reg)));
        when(namingService.getAllInstances(anyString(), anyString())).thenReturn(List.of(inst));

        List<RemoteServiceRegistration> result = directory.lookup("com.example.UserService");
        assertEquals(1, result.size());
        assertEquals("user-plugin", result.get(0).getPluginId());
        assertEquals(9090, result.get(0).getPort());

        RemoteServiceRegistration byPlugin = directory.lookup("com.example.UserService", "user-plugin");
        assertNotNull(byPlugin);
        assertNull(directory.lookup("com.example.UserService", "other-plugin"));
    }

    @Test
    void unregisterTargetsInstanceByNode() throws Exception {
        RemoteServiceRegistration reg = sampleReg();
        Instance inst = new Instance();
        inst.setIp("10.0.0.5");
        inst.setPort(9090);
        inst.setMetadata(Map.of("pluginId", "user-plugin", "nodeId", "node-1"));
        when(namingService.getAllInstances(anyString(), anyString())).thenReturn(List.of(inst));

        directory.unregister("com.example.UserService", "user-plugin", "node-1");

        verify(namingService).deregisterInstance(
                eq(PREFIX + ":svc:com.example.UserService"), eq(GROUP), eq("10.0.0.5"), eq(9090));
    }

    @Test
    void unregisterAllByNodeScansServices() throws Exception {
        ListView<String> view = new ListView<>();
        view.setData(List.of(PREFIX + ":svc:com.example.UserService"));
        when(namingService.getServicesOfServer(anyInt(), anyInt(), anyString())).thenReturn(view);

        Instance inst = new Instance();
        inst.setIp("10.0.0.5");
        inst.setPort(9090);
        inst.setMetadata(Map.of("pluginId", "user-plugin", "nodeId", "node-1"));
        when(namingService.getAllInstances(anyString(), anyString())).thenReturn(List.of(inst));

        directory.unregisterAllByNode("node-1");

        verify(namingService).deregisterInstance(
                eq(PREFIX + ":svc:com.example.UserService"), eq(GROUP), eq("10.0.0.5"), eq(9090));
    }

    @Test
    void allServiceInterfacesStripsPrefix() throws Exception {
        ListView<String> view = new ListView<>();
        view.setData(List.of(PREFIX + ":svc:com.example.UserService", "unrelated-service"));
        when(namingService.getServicesOfServer(anyInt(), anyInt(), anyString())).thenReturn(view);

        assertEquals(Collections.singleton("com.example.UserService"), directory.allServiceInterfaces());
    }
}
