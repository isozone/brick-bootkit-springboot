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