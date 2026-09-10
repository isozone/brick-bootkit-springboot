package com.zqzqq.bootkits.transport.mqtt;

import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.QosLevel;
import com.zqzqq.bootkits.protocol.serialization.EnvelopeSerializer;
import com.zqzqq.bootkits.protocol.serialization.JacksonEnvelopeSerializer;
import com.zqzqq.bootkits.protocol.transport.EnvelopeTransport;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT 跨进程传输参考实现。
 * <p>
 * 职责：将 {@link BrickEnvelope} 序列化为 JSON 字节，通过 MQTT broker 在进程间转发。
 * 不感知信封语义（协议宪法），只负责搬运字节。
 * <p>
 * Topic 规则：
 * <ul>
 *   <li>发布：{@code brick/{deviceId}/{capabilityId}}</li>
 *   <li>订阅：{@code brick/#}（全局）或 {@code brick/{deviceId}/+}（单设备）</li>
 * </ul>
 * <p>
 * QoS 映射：Brick AT_MOST_ONCE → MQTT QoS 0，Brick AT_LEAST_ONCE → MQTT QoS 1。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
public class MqttEnvelopeTransport implements EnvelopeTransport, MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttEnvelopeTransport.class);

    private final MqttTransportConfig config;
    private final EnvelopeSerializer serializer;
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private volatile MqttClient mqttClient;
    private volatile TransportListener listener;

    public MqttEnvelopeTransport(MqttTransportConfig config) {
        this.config = config;
        this.serializer = new JacksonEnvelopeSerializer();
    }

    public MqttEnvelopeTransport(MqttTransportConfig config, EnvelopeSerializer serializer) {
        this.config = config;
        this.serializer = serializer;
    }

    @Override
    public void start(TransportListener listener) {
        this.listener = listener;
        try {
            mqttClient = new MqttClient(config.getBrokerUri(), config.getClientId());
            mqttClient.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(config.isCleanSession());
            options.setConnectionTimeout(config.getConnectionTimeout());
            options.setKeepAliveInterval(config.getKeepAliveInterval());
            options.setAutomaticReconnect(config.isAutomaticReconnect());

            mqttClient.connect(options);
            connected.set(true);

            String subscription = config.buildGlobalSubscription();
            mqttClient.subscribe(subscription, config.getDefaultQos().ordinal());
            log.info("MQTT 传输已启动: broker={}, clientId={}, subscription={}",
                    config.getBrokerUri(), config.getClientId(), subscription);
        } catch (Exception e) {
            log.error("MQTT 连接失败: {}", config.getBrokerUri(), e);
            connected.set(false);
        }
    }

    @Override
    public void stop() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            if (mqttClient != null) {
                mqttClient.close();
            }
        } catch (Exception e) {
            log.warn("MQTT 断开异常", e);
        } finally {
            connected.set(false);
            log.info("MQTT 传输已停止: clientId={}", config.getClientId());
        }
    }

    @Override
    public boolean isConnected() {
        return connected.get() && mqttClient != null && mqttClient.isConnected();
    }

    @Override
    public void publish(BrickEnvelope envelope) {
        if (!isConnected()) {
            log.warn("MQTT 未连接，丢弃消息: messageId={}", envelope.getMessageId());
            return;
        }
        try {
            String topic = config.buildTopic(envelope.getDeviceId(), envelope.getCapabilityId());
            byte[] payload = serializer.serialize(envelope);
            int mqttQos = mapQos(envelope.getQos());

            MqttMessage message = new MqttMessage(payload);
            message.setQos(mqttQos);
            message.setRetained(false);

            mqttClient.publish(topic, message);
            log.debug("MQTT publish: topic={}, qos={}, messageId={}", topic, mqttQos, envelope.getMessageId());
        } catch (Exception e) {
            log.error("MQTT publish 失败: messageId={}", envelope.getMessageId(), e);
        }
    }

    // ==================== MqttCallback ====================

    @Override
    public void connectionLost(Throwable cause) {
        connected.set(false);
        log.warn("MQTT 连接断开: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) {
        try {
            BrickEnvelope envelope = serializer.deserialize(mqttMessage.getPayload());
            if (listener != null) {
                listener.onMessage(envelope);
            }
            log.debug("MQTT 消息到达: topic={}, messageId={}", topic, envelope.getMessageId());
        } catch (Exception e) {
            log.error("MQTT 消息解析失败: topic={}", topic, e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // delivery confirmed
    }

    // ==================== 内部方法 ====================

    private static int mapQos(QosLevel qos) {
        return qos == null ? 0 : qos.getCode();
    }
}