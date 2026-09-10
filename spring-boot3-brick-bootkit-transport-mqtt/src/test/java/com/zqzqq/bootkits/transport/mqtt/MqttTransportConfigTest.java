package com.zqzqq.bootkits.transport.mqtt;

import com.zqzqq.bootkits.protocol.message.BrickEnvelope;
import com.zqzqq.bootkits.protocol.message.BrickMessages;
import com.zqzqq.bootkits.protocol.message.QosLevel;
import com.zqzqq.bootkits.protocol.serialization.EnvelopeSerializer;
import com.zqzqq.bootkits.protocol.serialization.JacksonEnvelopeSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MqttTransportConfigTest {

    private MqttTransportConfig config;

    @BeforeEach
    void setUp() {
        config = new MqttTransportConfig();
    }

    @Test
    void shouldBuildDefaultTopic() {
        String topic = config.buildTopic("sensor-01", "temp");
        assertEquals("brick/sensor-01/temp", topic);
    }

    @Test
    void shouldBuildDeviceSubscription() {
        String topic = config.buildDeviceSubscription("sensor-01");
        assertEquals("brick/sensor-01/+", topic);
    }

    @Test
    void shouldBuildGlobalSubscription() {
        assertEquals("brick/#", config.buildGlobalSubscription());
    }

    @Test
    void shouldParseDeviceId() {
        assertEquals("sensor-01", config.parseDeviceId("brick/sensor-01/temp"));
    }

    @Test
    void shouldParseCapabilityId() {
        assertEquals("temp", config.parseCapabilityId("brick/sensor-01/temp"));
    }

    @Test
    void shouldReturnNullForInvalidTopic() {
        assertNull(config.parseDeviceId("invalid"));
        assertNull(config.parseCapabilityId("invalid"));
    }

    @Test
    void shouldUseCustomPrefix() {
        config.setTopicPrefix("iot");
        assertEquals("iot/sensor-01/temp", config.buildTopic("sensor-01", "temp"));
        assertEquals("iot/#", config.buildGlobalSubscription());
    }

    @Test
    void shouldConstructWithBrokerAndClientId() {
        MqttTransportConfig cfg = new MqttTransportConfig("tcp://10.0.0.1:1883", "my-client");
        assertEquals("tcp://10.0.0.1:1883", cfg.getBrokerUri());
        assertEquals("my-client", cfg.getClientId());
    }

    @Test
    void shouldHaveDefaults() {
        assertTrue(config.isCleanSession());
        assertTrue(config.isAutomaticReconnect());
        assertEquals(10, config.getConnectionTimeout());
        assertEquals(30, config.getKeepAliveInterval());
        assertEquals(QosLevel.FIRE_FORGET, config.getDefaultQos());
    }
}