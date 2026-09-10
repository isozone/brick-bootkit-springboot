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


package com.zqzqq.bootkits.protocol.bus;

import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.MessageType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * in-process 信封总线：将信封语义落地为进程内 pub/sub + request/reply。
 * <p>
 * 这是协议在本地传输上的最小实现，也是跨进程传输的对照基准：
 * 同一个信封，换一个 {@code EnvelopeTransport} 就能走 MQTT/gRPC，语义不变。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class EnvelopeBus {

    /** 订阅者注册表：deviceId:capabilityId → listeners（"*" 通配任意） */
    private final Map<String, List<EnvelopeListener>> subscriptions = new ConcurrentHashMap<>();

    /** 待回包表：messageId → reply future */
    private final Map<String, CompletableFuture<BrickEnvelope>> pendingReplies = new ConcurrentHashMap<>();

    /**
     * 订阅某设备某能力的消息。
     *
     * @param deviceId     设备标识；"*" 表示任意设备
     * @param capabilityId 能力标识；"*" 表示任意能力
     * @param listener     监听器
     * @return 订阅句柄
     */
    public Subscription subscribe(String deviceId, String capabilityId, EnvelopeListener listener) {
        Objects.requireNonNull(deviceId, "deviceId 不能为空");
        Objects.requireNonNull(capabilityId, "capabilityId 不能为空");
        Objects.requireNonNull(listener, "listener 不能为空");
        String key = key(deviceId, capabilityId);
        List<EnvelopeListener> listeners = subscriptions.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        listeners.add(listener);
        return new Subscription(key, listener);
    }

    /**
     * 取消订阅。
     */
    public void unsubscribe(Subscription subscription) {
        if (subscription == null) {
            return;
        }
        List<EnvelopeListener> listeners = subscriptions.get(subscription.key);
        if (listeners != null) {
            listeners.remove(subscription.listener);
        }
    }

    /**
     * 发布一条消息（event / 单向 command）。
     * 若该消息期待 reply（request/command），请用 {@link #request} 或 {@link #command}。
     */
    public void publish(BrickEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope 不能为空");
        dispatch(envelope);
    }

    /**
     * 发送 request 并同步等待 reply。
     *
     * @param timeout 超时
     * @param unit    时间单位
     * @return reply 信封；超时或回复为空时返回 null
     */
    public BrickEnvelope request(BrickEnvelope envelope, long timeout, TimeUnit unit) {
        Objects.requireNonNull(envelope, "envelope 不能为空");
        if (envelope.getType() != MessageType.REQUEST) {
            throw new IllegalArgumentException("request 方法只接受 type=REQUEST 的消息");
        }
        CompletableFuture<BrickEnvelope> future = new CompletableFuture<>();
        pendingReplies.put(envelope.getMessageId(), future);
        try {
            dispatch(envelope);
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("request 等待 reply 失败: " + envelope.getMessageId(), e);
        } finally {
            pendingReplies.remove(envelope.getMessageId());
        }
    }

    /**
     * 发送 command 并同步等待 reply。
     */
    public BrickEnvelope command(BrickEnvelope envelope, long timeout, TimeUnit unit) {
        Objects.requireNonNull(envelope, "envelope 不能为空");
        if (envelope.getType() != MessageType.COMMAND) {
            throw new IllegalArgumentException("command 方法只接受 type=COMMAND 的消息");
        }
        CompletableFuture<BrickEnvelope> future = new CompletableFuture<>();
        pendingReplies.put(envelope.getMessageId(), future);
        try {
            dispatch(envelope);
            return future.get(timeout, unit);
        } catch (TimeoutException e) {
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("command 等待 reply 失败: " + envelope.getMessageId(), e);
        } finally {
            pendingReplies.remove(envelope.getMessageId());
        }
    }

    /**
     * 直接回复：将 reply 信封闭合到原请求的 pending future。
     */
    public void reply(BrickEnvelope reply) {
        Objects.requireNonNull(reply, "reply 不能为空");
        if (reply.getType() != MessageType.REPLY) {
            throw new IllegalArgumentException("reply 方法只接受 type=REPLY 的消息");
        }
        String correlationId = reply.getCorrelationId();
        if (correlationId == null) {
            throw new IllegalArgumentException("reply 必须携带 correlationId");
        }
        CompletableFuture<BrickEnvelope> future = pendingReplies.get(correlationId);
        if (future != null) {
            future.complete(reply);
        }
    }

    /**
     * 发送 request / command（带超时，以 CompletableFuture 返回）。
     * 适用于异步场景。
     */
    public CompletableFuture<BrickEnvelope> sendAsync(BrickEnvelope envelope, long timeout, TimeUnit unit) {
        Objects.requireNonNull(envelope, "envelope 不能为空");
        if (!envelope.getType().expectsReply()) {
            throw new IllegalArgumentException("sendAsync 只接受 request/command 消息");
        }
        CompletableFuture<BrickEnvelope> future = new CompletableFuture<>();
        pendingReplies.put(envelope.getMessageId(), future);
        dispatch(envelope);
        return future.completeOnTimeout(null, timeout, unit);
    }

    /**
     * 保留请求方待回包表：供传输层调用（跨进程时由远端回复触发）。
     */
    void registerPendingReply(String messageId, CompletableFuture<BrickEnvelope> future) {
        pendingReplies.put(messageId, future);
    }

    private void dispatch(BrickEnvelope envelope) {
        // 先尝试精确匹配，再尝试通配
        deliver(key(envelope.getDeviceId(), envelope.getCapabilityId()), envelope);
        deliver(key("*", envelope.getCapabilityId()), envelope);
        deliver(key(envelope.getDeviceId(), "*"), envelope);
        deliver(key("*", "*"), envelope);
    }

    private void deliver(String key, BrickEnvelope envelope) {
        List<EnvelopeListener> listeners = subscriptions.get(key);
        if (listeners == null || listeners.isEmpty()) {
            return;
        }
        for (EnvelopeListener listener : listeners) {
            listener.onMessage(envelope);
        }
    }

    private static String key(String deviceId, String capabilityId) {
        return deviceId + ":" + capabilityId;
    }

    /**
     * 订阅句柄。
     */
    public static final class Subscription {
        private final String key;
        private final EnvelopeListener listener;

        Subscription(String key, EnvelopeListener listener) {
            this.key = key;
            this.listener = listener;
        }

        public String getKey() {
            return key;
        }
    }
}