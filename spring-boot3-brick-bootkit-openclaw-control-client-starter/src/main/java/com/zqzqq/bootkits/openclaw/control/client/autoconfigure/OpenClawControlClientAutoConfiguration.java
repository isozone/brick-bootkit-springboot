package com.zqzqq.bootkits.openclaw.control.client.autoconfigure;

import com.zqzqq.bootkits.openclaw.control.client.DefaultOpenClawControlClient;
import com.zqzqq.bootkits.openclaw.control.client.ManagedOpenClawControlClient;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawAgentRuntime;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawAgentRuntimeProperties;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlClient;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlClientProperties;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlMessageListener;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawHeartbeatSupplier;
import com.zqzqq.bootkits.openclaw.control.client.OpenClawTaskHandler;
import com.zqzqq.bootkits.openclaw.protocol.ClientCapabilityDescriptor;
import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;
import com.zqzqq.bootkits.openclaw.protocol.ClientRegistrationRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@ConditionalOnProperty(prefix = "openclaw.agent", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OpenClawAgentProperties.class)
public class OpenClawControlClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenClawControlClientProperties openClawControlClientProperties(OpenClawAgentProperties properties) {
        OpenClawControlClientProperties target = new OpenClawControlClientProperties();
        target.setBaseUrl(properties.getBaseUrl());
        target.setApiBasePath(properties.getApiBasePath());
        target.setWebSocketUrl(properties.getWebSocketUrl());
        target.setWebSocketPath(properties.getWebSocketPath());
        target.setClientId(properties.getClientId());
        target.setAuthToken(properties.getAuthToken());
        target.setAuthSecret(properties.getAuthSecret());
        target.setAuthMode(properties.getAuthMode());
        target.setPreferredTransport(properties.getTransport());
        target.setRequestTimeout(properties.getRequestTimeout());
        target.setWebSocketRequestTimeout(properties.getWebSocketRequestTimeout());
        target.setHeartbeatInterval(properties.getHeartbeatInterval());
        return target;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenClawControlClient openClawControlClient(OpenClawControlClientProperties properties) {
        return new DefaultOpenClawControlClient(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ManagedOpenClawControlClient managedOpenClawControlClient(OpenClawControlClient client,
                                                                     OpenClawControlClientProperties properties) {
        return new ManagedOpenClawControlClient(client, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenClawAgentRuntimeProperties openClawAgentRuntimeProperties(OpenClawAgentProperties properties) {
        OpenClawAgentRuntimeProperties target = new OpenClawAgentRuntimeProperties();
        target.setAutoClaimOnAssigned(properties.getRuntime().isAutoClaimOnAssigned());
        target.setPollEnabled(properties.getRuntime().isPollEnabled());
        target.setClaimPollInterval(properties.getRuntime().getClaimPollInterval());
        target.setLeaseRenewAhead(properties.getRuntime().getLeaseRenewAhead());
        target.setExecutorThreads(properties.getRuntime().getExecutorThreads());
        return target;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenClawAgentRuntime openClawAgentRuntime(ManagedOpenClawControlClient managedClient,
                                                     ObjectProvider<OpenClawTaskHandler> taskHandlers,
                                                     OpenClawAgentRuntimeProperties properties) {
        return new OpenClawAgentRuntime(managedClient, taskHandlers.orderedStream().toList(), properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientRegistrationRequest openClawClientRegistrationRequest(OpenClawAgentProperties properties,
                                                                       ObjectProvider<OpenClawRegistrationCustomizer> customizers) {
        ClientRegistrationRequest request = new ClientRegistrationRequest();
        request.setClientId(properties.getClientId());
        request.setDisplayName(properties.getRegistration().getDisplayName());
        request.setMachineId(properties.getRegistration().getMachineId());
        request.setVersion(properties.getRegistration().getVersion());
        request.setSdkVersion(properties.getRegistration().getSdkVersion());
        request.setHostName(fallback(properties.getRegistration().getHostName(), detectHostName()));
        request.setOsName(fallback(properties.getRegistration().getOsName(), System.getProperty("os.name")));
        request.setOsVersion(fallback(properties.getRegistration().getOsVersion(), System.getProperty("os.version")));
        request.setTags(properties.getRegistration().getTags());
        request.setAttributes(properties.getRegistration().getAttributes());
        request.setCapabilities(toCapabilities(properties.getRegistration().getCapabilities()));
        customizers.orderedStream().forEach(customizer -> customizer.customize(request));
        return request;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenClawHeartbeatSupplier openClawHeartbeatSupplier() {
        return ClientHeartbeatRequest::new;
    }

    @Bean(name = "openClawAgentCompositeMessageListener")
    @ConditionalOnMissingBean(name = "openClawAgentCompositeMessageListener")
    public OpenClawControlMessageListener openClawAgentCompositeMessageListener(OpenClawAgentRuntime runtime,
                                                                                ObjectProvider<OpenClawControlMessageListener> listeners) {
        List<OpenClawControlMessageListener> all = new ArrayList<>();
        all.add(runtime);
        listeners.orderedStream()
                .filter(listener -> listener != runtime)
                .forEach(all::add);
        return new CompositeOpenClawControlMessageListener(all);
    }

    @Bean
    @ConditionalOnProperty(prefix = "openclaw.agent", name = "auto-startup", havingValue = "true", matchIfMissing = true)
    public OpenClawAgentLifecycle openClawAgentLifecycle(ManagedOpenClawControlClient managedClient,
                                                         OpenClawAgentRuntime runtime,
                                                         ClientRegistrationRequest registrationRequest,
                                                         OpenClawHeartbeatSupplier heartbeatSupplier,
                                                         OpenClawControlMessageListener messageListener,
                                                         OpenClawAgentProperties properties) {
        return new OpenClawAgentLifecycle(
                managedClient,
                runtime,
                registrationRequest,
                heartbeatSupplier,
                messageListener,
                properties.isAutoStartup()
        );
    }

    private List<ClientCapabilityDescriptor> toCapabilities(List<OpenClawAgentProperties.Capability> items) {
        List<ClientCapabilityDescriptor> result = new ArrayList<>();
        for (OpenClawAgentProperties.Capability item : items) {
            ClientCapabilityDescriptor descriptor = new ClientCapabilityDescriptor();
            descriptor.setCapabilityId(item.getCapabilityId());
            descriptor.setCategory(item.getCategory());
            descriptor.setDisplayName(item.getName());
            descriptor.setEnabled(item.getEnabled());
            descriptor.setAttributes(item.getMetadata());
            result.add(descriptor);
        }
        return result;
    }

    private String detectHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ex) {
            return "localhost";
        }
    }

    private String fallback(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }
}
