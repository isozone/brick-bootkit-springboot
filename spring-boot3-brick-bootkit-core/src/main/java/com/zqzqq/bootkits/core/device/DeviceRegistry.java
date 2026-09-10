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


package com.zqzqq.bootkits.core.device;

import com.zqzqq.bootkits.protocol.adapter.DeviceAdapter;
import com.zqzqq.bootkits.protocol.adapter.TransportContext;
import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.capability.Device;
import com.zqzqq.bootkits.protocol.lifecycle.DeviceLifecycle;
import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 设备注册表：设备接入的运行时管理中心（支持热插拔 + 并发安全）。
 * <p>
 * 职责边界：
 * <ul>
 *   <li>注册表管理"哪些设备在线"（设备目录）</li>
 *   <li>adapter 负责"设备怎么通信"（传输翻译）</li>
 *   <li>信封总线负责"消息怎么路由"（语义管道）</li>
 * </ul>
 * <p>
 * 热插拔特性：
 * <ul>
 *   <li>{@link #register} / {@link #unregister} 线程安全（ReentrantLock 保护）</li>
 *   <li>{@link #reconnect} 支持断线重连</li>
 *   <li>{@link #startHealthCheck} 定期检查 adapter 连接状态</li>
 *   <li>支持运行时动态注册/注销（不限于启动阶段）</li>
 * </ul>
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class DeviceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistry.class);

    /** 设备目录：deviceId → DeviceEntry（ConcurrentHashMap 保证读并发安全） */
    private final Map<String, DeviceEntry> devices = new ConcurrentHashMap<>();

    /** 注册/注销操作锁（保证 register/unregister 原子性） */
    private final ReentrantLock registrationLock = new ReentrantLock();

    /** 信封总线：业务与 adapter 的语义管道 */
    private final EnvelopeBus bus;

    /** 注册/注销监听器（CopyOnWriteArrayList 保证迭代安全） */
    private final List<DeviceRegistrationListener> listeners = new CopyOnWriteArrayList<>();

    /** 健康检查调度器（可选） */
    private ScheduledExecutorService healthCheckScheduler;
    private ScheduledFuture<?> healthCheckFuture;

    public DeviceRegistry(EnvelopeBus bus) {
        this.bus = bus;
    }

    // ==================== 热插拔：注册/注销 ====================

    /**
     * 注册一个设备适配器（线程安全，支持热插拔）。
     *
     * @param adapter 设备适配器
     * @param context 传输上下文
     */
    public void register(DeviceAdapter adapter, TransportContext context) {
        registrationLock.lock();
        try {
            adapter.connect(bus, context);
            Device device = adapter.getDevice();
            if (device == null) {
                adapter.disconnect();
                throw new IllegalStateException("adapter 连接后未提供设备能力树: " + adapter.getDeviceType());
            }
            DeviceEntry entry = new DeviceEntry(device, adapter, context);
            DeviceEntry prev = devices.put(device.getDeviceId(), entry);
            if (prev != null) {
                log.warn("设备 {} 已存在，旧适配器将被断开", device.getDeviceId());
                prev.adapter().disconnect();
            }
            log.info("设备注册: {} (type={}, transport={})", device.getDeviceId(),
                    device.getDeviceType(), adapter.getTransport());
            notifyRegistered(device, adapter);
        } finally {
            registrationLock.unlock();
        }
    }

    /**
     * 注销一个设备（线程安全）。
     *
     * @param deviceId 设备标识
     */
    public void unregister(String deviceId) {
        registrationLock.lock();
        try {
            DeviceEntry entry = devices.remove(deviceId);
            if (entry == null) {
                return;
            }
            log.info("设备注销: {}", deviceId);
            notifyUnregistered(entry.device(), entry.adapter());
            entry.adapter().disconnect();
        } finally {
            registrationLock.unlock();
        }
    }

    /**
     * 断线重连：断开当前连接并重新建立。
     *
     * @param deviceId 设备标识
     * @return true 如果重连成功
     */
    public boolean reconnect(String deviceId) {
        registrationLock.lock();
        try {
            DeviceEntry entry = devices.get(deviceId);
            if (entry == null) {
                log.warn("重连失败: 设备 {} 未注册", deviceId);
                return false;
            }
            try {
                entry.adapter().disconnect();
                entry.adapter().connect(bus, entry.context());
                log.info("设备重连成功: {}", deviceId);
                return true;
            } catch (Exception e) {
                log.error("设备重连失败: {}", deviceId, e);
                return false;
            }
        } finally {
            registrationLock.unlock();
        }
    }

    // ==================== 健康检查 ====================

    /**
     * 启动定期健康检查（检测 adapter 连接状态）。
     *
     * @param intervalMs 检查间隔（毫秒）
     */
    public void startHealthCheck(long intervalMs) {
        if (healthCheckScheduler != null) {
            return;
        }
        healthCheckScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "DeviceRegistry-HealthCheck");
            t.setDaemon(true);
            return t;
        });
        healthCheckFuture = healthCheckScheduler.scheduleWithFixedDelay(
                this::checkHealth, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
        log.info("设备健康检查已启动: 间隔={}ms", intervalMs);
    }

    /**
     * 停止健康检查。
     */
    public void stopHealthCheck() {
        if (healthCheckFuture != null) {
            healthCheckFuture.cancel(false);
            healthCheckFuture = null;
        }
        if (healthCheckScheduler != null) {
            healthCheckScheduler.shutdown();
            healthCheckScheduler = null;
        }
    }

    /**
     * 执行一次健康检查：发现断连的 adapter 尝试重连。
     */
    void checkHealth() {
        for (Map.Entry<String, DeviceEntry> entry : devices.entrySet()) {
            DeviceAdapter adapter = entry.getValue().adapter();
            if (!adapter.isConnected()) {
                log.warn("设备 {} 断连，尝试重连", entry.getKey());
                try {
                    adapter.connect(bus, entry.getValue().context());
                    log.info("设备 {} 重连成功", entry.getKey());
                } catch (Exception e) {
                    log.error("设备 {} 重连失败: {}", entry.getKey(), e.getMessage());
                }
            }
        }
    }

    // ==================== 查询 ====================

    public Device lookup(String deviceId) {
        DeviceEntry entry = devices.get(deviceId);
        return entry != null ? entry.device() : null;
    }

    public DeviceAdapter getAdapter(String deviceId) {
        DeviceEntry entry = devices.get(deviceId);
        return entry != null ? entry.adapter() : null;
    }

    public Collection<Device> getAllDevices() {
        List<Device> result = new ArrayList<>();
        for (DeviceEntry entry : devices.values()) {
            result.add(entry.device());
        }
        return Collections.unmodifiableList(result);
    }

    public Collection<String> getAllDeviceIds() {
        return Collections.unmodifiableSet(devices.keySet());
    }

    public int getDeviceCount() {
        return devices.size();
    }

    public boolean isRegistered(String deviceId) {
        return devices.containsKey(deviceId);
    }

    // ==================== 消息发送 ====================

    public BrickEnvelope send(String deviceId, String capabilityId, Object payload) {
        BrickEnvelope request = BrickMessages.request(deviceId, capabilityId, payload);
        return bus.request(request, 5, TimeUnit.SECONDS);
    }

    public BrickEnvelope command(String deviceId, String capabilityId, Object payload) {
        BrickEnvelope cmd = BrickMessages.command(deviceId, capabilityId, payload);
        return bus.command(cmd, 5, TimeUnit.SECONDS);
    }

    // ==================== 生命周期 ====================

    public void shutdown() {
        stopHealthCheck();
        log.info("设备注册表关闭，注销全部 {} 个设备", devices.size());
        registrationLock.lock();
        try {
            Iterator<Map.Entry<String, DeviceEntry>> it = devices.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, DeviceEntry> entry = it.next();
                try {
                    notifyUnregistered(entry.getValue().device(), entry.getValue().adapter());
                    entry.getValue().adapter().disconnect();
                } catch (Exception e) {
                    log.error("设备注销失败: {}", entry.getKey(), e);
                }
                it.remove();
            }
        } finally {
            registrationLock.unlock();
        }
    }

    public EnvelopeBus getBus() {
        return bus;
    }

    public void addListener(DeviceRegistrationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(DeviceRegistrationListener listener) {
        listeners.remove(listener);
    }

    private void notifyRegistered(Device device, DeviceAdapter adapter) {
        for (DeviceRegistrationListener listener : listeners) {
            try {
                listener.onDeviceRegistered(device, adapter);
            } catch (Exception e) {
                log.error("设备注册监听器异常: {}", device.getDeviceId(), e);
            }
        }
    }

    private void notifyUnregistered(Device device, DeviceAdapter adapter) {
        for (DeviceRegistrationListener listener : listeners) {
            try {
                listener.onDeviceUnregistered(device, adapter);
            } catch (Exception e) {
                log.error("设备注销监听器异常: {}", device.getDeviceId(), e);
            }
        }
    }

    private static final class DeviceEntry {
        private final Device device;
        private final DeviceAdapter adapter;
        private final TransportContext context;

        DeviceEntry(Device device, DeviceAdapter adapter, TransportContext context) {
            this.device = device;
            this.adapter = adapter;
            this.context = context;
        }

        Device device() {
            return device;
        }

        DeviceAdapter adapter() {
            return adapter;
        }

        TransportContext context() {
            return context;
        }
    }
}