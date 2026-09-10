package com.zqzqq.bootkits.transport.mqtt;

import com.zqzqq.bootkits.protocol.message.QosLevel;

/**
 * MQTT 传输层配置。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class MqttTransportConfig {

    /** MQTT broker URI，例如 tcp://localhost:1883 */
    private String brokerUri = "tcp://localhost:1883";

    /** 客户端 ID，同一 broker 下必须唯一 */
    private String clientId = "brick-transport-" + System.currentTimeMillis();

    /** topic 前缀，例如 brick/sensor-01/temp → brick/sensor-01/temp */
    private String topicPrefix = "brick";

    /** 默认 QoS 等级 */
    private QosLevel defaultQos = QosLevel.FIRE_FORGET;

    /** 是否持久会话 */
    private boolean cleanSession = true;

    /** 连接超时（秒） */
    private int connectionTimeout = 10;

    /** 心跳间隔（秒） */
    private int keepAliveInterval = 30;

    /** 是否自动重连 */
    private boolean automaticReconnect = true;

    public MqttTransportConfig() {
    }

    public MqttTransportConfig(String brokerUri, String clientId) {
        this.brokerUri = brokerUri;
        this.clientId = clientId;
    }

    public String getBrokerUri() {
        return brokerUri;
    }

    public void setBrokerUri(String brokerUri) {
        this.brokerUri = brokerUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public void setTopicPrefix(String topicPrefix) {
        this.topicPrefix = topicPrefix;
    }

    public QosLevel getDefaultQos() {
        return defaultQos;
    }

    public void setDefaultQos(QosLevel defaultQos) {
        this.defaultQos = defaultQos;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getKeepAliveInterval() {
        return keepAliveInterval;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public boolean isAutomaticReconnect() {
        return automaticReconnect;
    }

    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    /**
     * 构造 MQTT topic。
     *
     * @param deviceId      设备 ID
     * @param capabilityId  能力 ID
     * @return MQTT topic，例如 brick/sensor-01/temp
     */
    public String buildTopic(String deviceId, String capabilityId) {
        return topicPrefix + "/" + deviceId + "/" + capabilityId;
    }

    /**
     * 构造设备级订阅 topic（单设备所有能力）。
     *
     * @param deviceId 设备 ID
     * @return 例如 brick/sensor-01/+
     */
    public String buildDeviceSubscription(String deviceId) {
        return topicPrefix + "/" + deviceId + "/+";
    }

    /**
     * 构造全局订阅 topic。
     *
     * @return 例如 brick/#
     */
    public String buildGlobalSubscription() {
        return topicPrefix + "/#";
    }

    /**
     * 从 MQTT topic 中解析 deviceId。
     *
     * @param topic MQTT topic，格式 prefix/deviceId/capabilityId
     * @return deviceId，解析失败返回 null
     */
    public String parseDeviceId(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 3) {
            return parts[1];
        }
        return null;
    }

    /**
     * 从 MQTT topic 中解析 capabilityId。
     *
     * @param topic MQTT topic
     * @return capabilityId，解析失败返回 null
     */
    public String parseCapabilityId(String topic) {
        String[] parts = topic.split("/");
        if (parts.length >= 3) {
            return parts[parts.length - 1];
        }
        return null;
    }
}