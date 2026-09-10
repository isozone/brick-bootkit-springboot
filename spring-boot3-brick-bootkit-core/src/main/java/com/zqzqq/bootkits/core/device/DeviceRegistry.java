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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备注册表：设备接入的运行时管理中心。
 * <p>
 * 业务层通过 {@code registry.lookup("sensor-01")} 获取能力树，通过 bus 发送信封。
 * 注册表负责 adapter 生命周期编排 + 设备目录维护。
 * <p>
 * 职责边界：
 * <ul>
 *   <li>注册表管理"哪些设备在线"（设备目录）</li>
 *   <li>adapter 负责"设备怎么通信"（传输翻译）</li>
 *   <li>信封总线负责"消息怎么路由"（语义管道）</li>
 * </ul>
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class DeviceRegistry {

    private static final Logger log = LoggerFactory.getLogger(DeviceRegistry.class);

    /** 设备目录：deviceId → DeviceEntry */
    private final Map<String, DeviceEntry> devices = new ConcurrentHashMap<>();

    /** 信封总线：业务与 adapter 的语义管道 */
    private final EnvelopeBus bus;

    /** 注册/注销监听器 */
    private final List<DeviceRegistrationListener> listeners = new ArrayList<>();

    public DeviceRegistry(EnvelopeBus bus) {
        this.bus = bus;
    }

    /**
     * 注册一个设备适配器。
     * <p>
     * 流程：
     * 1. adapter.connect() 建立传输连接
     * 2. adapter.getDevice() 获取能力树
     * 3. 加入设备目录
     * 4. 通知监听器
     *
     * @param adapter 设备适配器
     * @param context 传输上下文
     */
    public void register(DeviceAdapter adapter, TransportContext context) {
        adapter.connect(bus, context);
        Device device = adapter.getDevice();
        if (device == null) {
            adapter.disconnect();
            throw new IllegalStateException("adapter 连接后未提供设备能力树: " + adapter.getDeviceType());
        }
        DeviceEntry entry = new DeviceEntry(device, adapter);
        DeviceEntry prev = devices.put(device.getDeviceId(), entry);
        if (prev != null) {
            log.warn("设备 {} 已存在，旧适配器将被断开", device.getDeviceId());
            prev.adapter().disconnect();
        }
        log.info("设备注册: {} (type={}, transport={})", device.getDeviceId(),
                device.getDeviceType(), adapter.getTransport());
        notifyRegistered(device, adapter);
    }

    /**
     * 注销一个设备。
     *
     * @param deviceId 设备标识
     */
    public void unregister(String deviceId) {
        DeviceEntry entry = devices.remove(deviceId);
        if (entry == null) {
            return;
        }
        log.info("设备注销: {}", deviceId);
        notifyUnregistered(entry.device(), entry.adapter());
        entry.adapter().disconnect();
    }

    /**
     * 按设备ID查找能力树。
     *
     * @return 设备能力树，未找到返回 null
     */
    public Device lookup(String deviceId) {
        DeviceEntry entry = devices.get(deviceId);
        return entry != null ? entry.device() : null;
    }

    /**
     * 按设备ID查找适配器。
     *
     * @return 适配器，未找到返回 null
     */
    public DeviceAdapter getAdapter(String deviceId) {
        DeviceEntry entry = devices.get(deviceId);
        return entry != null ? entry.adapter() : null;
    }

    /**
     * 获取所有已注册设备（只读视图）。
     */
    public Collection<Device> getAllDevices() {
        List<Device> result = new ArrayList<>();
        for (DeviceEntry entry : devices.values()) {
            result.add(entry.device());
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 获取所有已注册设备ID。
     */
    public Collection<String> getAllDeviceIds() {
        return Collections.unmodifiableSet(devices.keySet());
    }

    /**
     * 已注册设备数量。
     */
    public int getDeviceCount() {
        return devices.size();
    }

    /**
     * 设备是否已注册。
     */
    public boolean isRegistered(String deviceId) {
        return devices.containsKey(deviceId);
    }

    /**
     * 注销所有设备（shutdown 用）。
     */
    public void shutdown() {
        log.info("设备注册表关闭，注销全部 {} 个设备", devices.size());
        for (Map.Entry<String, DeviceEntry> entry : devices.entrySet()) {
            try {
                notifyUnregistered(entry.getValue().device(), entry.getValue().adapter());
                entry.getValue().adapter().disconnect();
            } catch (Exception e) {
                log.error("设备注销失败: {}", entry.getKey(), e);
            }
        }
        devices.clear();
    }

    /**
     * 获取信封总线。
     */
    public EnvelopeBus getBus() {
        return bus;
    }

    /**
     * 添加注册/注销监听器。
     */
    public void addListener(DeviceRegistrationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 移除监听器。
     */
    public void removeListener(DeviceRegistrationListener listener) {
        listeners.remove(listener);
    }

    /**
     * 发送一条消息到指定设备。
     *
     * @param deviceId     设备标识
     * @param capabilityId 能力标识
     * @param payload      业务载荷
     * @return reply 信封（同步请求），或 null（超时/fire-and-forget）
     */
    public BrickEnvelope send(String deviceId, String capabilityId, Object payload) {
        BrickEnvelope request = BrickMessages.request(deviceId, capabilityId, payload);
        return bus.request(request, 5, java.util.concurrent.TimeUnit.SECONDS);
    }

    /**
     * 发送一条命令到指定设备。
     *
     * @param deviceId     设备标识
     * @param capabilityId 能力标识
     * @param payload      命令载荷
     * @return reply 信封（同步命令），或 null（超时/fire-and-forget）
     */
    public BrickEnvelope command(String deviceId, String capabilityId, Object payload) {
        BrickEnvelope cmd = BrickMessages.command(deviceId, capabilityId, payload);
        return bus.command(cmd, 5, java.util.concurrent.TimeUnit.SECONDS);
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

        DeviceEntry(Device device, DeviceAdapter adapter) {
            this.device = device;
            this.adapter = adapter;
        }

        Device device() {
            return device;
        }

        DeviceAdapter adapter() {
            return adapter;
        }
    }
}