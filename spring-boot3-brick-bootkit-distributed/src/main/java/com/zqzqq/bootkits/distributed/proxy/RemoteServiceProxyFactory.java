package com.zqzqq.bootkits.distributed.proxy;

import com.zqzqq.bootkits.distributed.registry.ServiceDirectory;
import com.zqzqq.bootkits.distributed.rpc.GrpcClientProvider;

import java.lang.reflect.Proxy;

/**
 * 远程服务代理工厂。
 * <p>
 * 宿主侧替代 {@code ServiceProxyFactory}：为本地接口 Class 创建远程代理，
 * 方法调用通过 gRPC 透明转发到远端执行节点。对调用方而言与本地调用无异。
 */
public final class RemoteServiceProxyFactory {

    private final ServiceDirectory directory;
    private final GrpcClientProvider clients;
    private final com.zqzqq.bootkits.distributed.metrics.DistributedMetrics metrics;
    private final java.util.Map<String, Long> methodTimeouts;
    private final int maxRetries;

    public RemoteServiceProxyFactory(ServiceDirectory directory, GrpcClientProvider clients) {
        this(directory, clients, null, java.util.Collections.emptyMap(), 0);
    }

    public RemoteServiceProxyFactory(ServiceDirectory directory,
                                     GrpcClientProvider clients,
                                     com.zqzqq.bootkits.distributed.metrics.DistributedMetrics metrics) {
        this(directory, clients, metrics, java.util.Collections.emptyMap(), 0);
    }

    public RemoteServiceProxyFactory(ServiceDirectory directory,
                                     GrpcClientProvider clients,
                                     com.zqzqq.bootkits.distributed.metrics.DistributedMetrics metrics,
                                     java.util.Map<String, Long> methodTimeouts,
                                     int maxRetries) {
        this.directory = directory;
        this.clients = clients;
        this.metrics = metrics != null ? metrics : (clients != null ? clients.metrics() : null);
        this.methodTimeouts = methodTimeouts != null ? methodTimeouts : java.util.Collections.emptyMap();
        this.maxRetries = Math.max(0, maxRetries);
    }

    /**
     * 创建远程服务代理。
     *
     * @param pluginId         目标插件 ID
     * @param serviceInterface 服务接口
     * @param <T>              服务类型
     * @return 远程代理实例
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(String pluginId, Class<T> serviceInterface) {
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                new RemoteInvocationHandler(pluginId, serviceInterface, directory, clients, metrics,
                        methodTimeouts, maxRetries)
        );
    }
}