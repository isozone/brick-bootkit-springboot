package com.zqzqq.bootkits.distributed.rpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * gRPC 客户端连接池：按「host:port」复用 ManagedChannel。
 * <p>
 * 宿主到多个执行节点之间会复用一个 channel，避免反复建连。
 */
public class GrpcClientProvider implements AutoCloseable {

    private final Map<String, ManagedChannel> channels = new ConcurrentHashMap<>();
    private final int maxInboundMessageSize;
    private final long callTimeoutMillis;

    public GrpcClientProvider(int maxInboundMessageSize, long callTimeoutMillis) {
        this.maxInboundMessageSize = maxInboundMessageSize;
        this.callTimeoutMillis = callTimeoutMillis;
    }

    /**
     * 获取（或创建）到指定节点的 channel。
     */
    public ManagedChannel channel(String host, int port) {
        String key = host + ":" + port;
        return channels.computeIfAbsent(key, k -> ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .maxInboundMessageSize(maxInboundMessageSize)
                .build());
    }

    public long getCallTimeoutMillis() {
        return callTimeoutMillis;
    }

    public void evict(String host, int port) {
        String key = host + ":" + port;
        ManagedChannel channel = channels.remove(key);
        if (channel != null) {
            channel.shutdown();
        }
    }

    @Override
    public void close() {
        for (ManagedChannel channel : channels.values()) {
            channel.shutdown();
        }
        channels.clear();
    }

    public void shutdownNow() {
        for (ManagedChannel channel : channels.values()) {
            channel.shutdownNow();
        }
        channels.clear();
    }
}