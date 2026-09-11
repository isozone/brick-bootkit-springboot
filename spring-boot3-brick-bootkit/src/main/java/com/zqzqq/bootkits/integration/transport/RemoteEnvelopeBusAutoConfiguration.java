package com.zqzqq.bootkits.integration.transport;

import com.zqzqq.bootkits.protocol.bus.EnvelopeBus;
import com.zqzqq.bootkits.protocol.transport.RemoteEnvelopeBus;
import com.zqzqq.bootkits.transport.mqtt.MqttEnvelopeTransport;
import com.zqzqq.bootkits.transport.mqtt.MqttTransportConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 跨进程传输自动配置：当 classpath 存在 transport-mqtt 时，将
 * {@link EnvelopeBus} 与 MQTT 传输桥接为 {@link RemoteEnvelopeBus}。
 * <p>
 * 通过 {@code brick.transport.mqtt.*} 配置 broker 地址、clientId、topic 前缀等。
 *
 * @author brick-bootkit
 * @since 4.1.0
 */
@Configuration
@ConditionalOnClass({EnvelopeBus.class, MqttEnvelopeTransport.class})
@EnableConfigurationProperties(RemoteEnvelopeBusAutoConfiguration.MqttProperties.class)
public class RemoteEnvelopeBusAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MqttEnvelopeTransport mqttEnvelopeTransport(MqttProperties properties) {
        MqttTransportConfig config = new MqttTransportConfig();
        if (properties.getBrokerUri() != null) {
            config.setBrokerUri(properties.getBrokerUri());
        }
        if (properties.getClientId() != null) {
            config.setClientId(properties.getClientId());
        }
        if (properties.getTopicPrefix() != null) {
            config.setTopicPrefix(properties.getTopicPrefix());
        }
        return new MqttEnvelopeTransport(config);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({EnvelopeBus.class, MqttEnvelopeTransport.class})
    public RemoteEnvelopeBus remoteEnvelopeBus(EnvelopeBus envelopeBus, MqttEnvelopeTransport transport) {
        RemoteEnvelopeBus remoteBus = new RemoteEnvelopeBus(envelopeBus, transport);
        remoteBus.start();
        return remoteBus;
    }

    /**
     * MQTT 传输配置属性，前缀 {@code brick.transport.mqtt}。
     */
    @ConfigurationProperties(prefix = "brick.transport.mqtt")
    public static class MqttProperties {

        /** broker URI，例如 tcp://localhost:1883 */
        private String brokerUri;

        /** 客户端 ID */
        private String clientId;

        /** topic 前缀，默认 brick */
        private String topicPrefix;

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
    }
}