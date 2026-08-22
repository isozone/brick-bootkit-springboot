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


package com.zqzqq.bootkits.distributed.rpc;

import com.zqzqq.bootkits.core.communication.PluginServiceRegistry;
import com.zqzqq.bootkits.distributed.rpc.proto.InvokeReply;
import com.zqzqq.bootkits.distributed.rpc.proto.InvokeRequest;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 执行节点服务端的防御性校验：空 pluginId/serviceInterface、超大参数列表应返回明确的
 * 错误回复而非空指针/穿透。
 */
class PluginInvocationServiceDefenseTest {

    @Test
    void rejectsEmptyPluginIdAndInterface() {
        PluginInvocationServiceImpl service =
                new PluginInvocationServiceImpl(mock(PluginServiceRegistry.class));
        AtomicReference<InvokeReply> ref = new AtomicReference<>();
        StreamObserver<InvokeReply> observer = new StreamObserver<>() {
            @Override public void onNext(InvokeReply value) { ref.set(value); }
            @Override public void onError(Throwable t) { }
            @Override public void onCompleted() { }
        };

        // 空 pluginId
        service.invoke(InvokeRequest.newBuilder()
                .setServiceInterface("com.example.UserService").setMethodName("getName").build(), observer);
        assertThat(ref.get()).isNotNull();
        assertThat(ref.get().getStatus()).isNotEqualTo(0);
        // 空 interface
        ref.set(null);
        service.invoke(InvokeRequest.newBuilder()
                .setPluginId("p").setMethodName("getName").build(), observer);
        assertThat(ref.get()).isNotNull();
        assertThat(ref.get().getStatus()).isNotEqualTo(0);
    }

    @Test
    void rejectsOversizedParamList() {
        PluginInvocationServiceImpl service =
                new PluginInvocationServiceImpl(mock(PluginServiceRegistry.class));
        AtomicReference<InvokeReply> ref = new AtomicReference<>();
        StreamObserver<InvokeReply> observer = new StreamObserver<>() {
            @Override public void onNext(InvokeReply value) { ref.set(value); }
            @Override public void onError(Throwable t) { }
            @Override public void onCompleted() { }
        };
        InvokeRequest.Builder rb = InvokeRequest.newBuilder()
                .setPluginId("plugin-a")
                .setServiceInterface("com.example.UserService")
                .setMethodName("getMany");
        // 注入 100 个参数名/值（超过上限64）
        for (int i = 0; i < 100; i++) {
            rb.addParamTypeNames("int").addParamValues("0");
        }
        service.invoke(rb.build(), observer);
        assertThat(ref.get()).isNotNull();
        assertThat(ref.get().getStatus()).isNotEqualTo(0);
        assertThat(ref.get().getErrorMessage()).contains("参数数量超过上限");
    }
}