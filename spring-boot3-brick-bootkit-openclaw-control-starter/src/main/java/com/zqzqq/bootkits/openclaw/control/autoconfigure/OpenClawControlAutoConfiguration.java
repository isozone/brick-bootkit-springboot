package com.zqzqq.bootkits.openclaw.control.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.zqzqq.bootkits.openclaw.control.service.DefaultTaskRoutingPolicy;
import com.zqzqq.bootkits.openclaw.control.service.OpenClawControlService;
import com.zqzqq.bootkits.openclaw.control.service.OpenClawIntegrationRegistry;
import com.zqzqq.bootkits.openclaw.control.spi.ClientStateStore;
import com.zqzqq.bootkits.openclaw.control.spi.OpenClawRuntimeIntegration;
import com.zqzqq.bootkits.openclaw.control.spi.TaskLifecycleListener;
import com.zqzqq.bootkits.openclaw.control.spi.TaskRoutingPolicy;
import com.zqzqq.bootkits.openclaw.control.spi.TaskStateStore;
import com.zqzqq.bootkits.openclaw.control.storage.jdbc.JdbcClientStateStore;
import com.zqzqq.bootkits.openclaw.control.storage.jdbc.JdbcTaskStateStore;
import com.zqzqq.bootkits.openclaw.control.storage.jdbc.OpenClawJdbcSchemaInitializer;
import com.zqzqq.bootkits.openclaw.control.store.InMemoryClientStateStore;
import com.zqzqq.bootkits.openclaw.control.store.InMemoryTaskStateStore;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.List;

@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnProperty(prefix = "openclaw.control", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OpenClawControlProperties.class)
public class OpenClawControlAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TaskRoutingPolicy openClawTaskRoutingPolicy() {
        return new DefaultTaskRoutingPolicy();
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenClawControlService openClawControlService(ClientStateStore clientStateStore,
                                                         TaskStateStore taskStateStore,
                                                         TaskRoutingPolicy taskRoutingPolicy,
                                                         ObjectProvider<TaskLifecycleListener> taskLifecycleListeners,
                                                         OpenClawControlProperties properties) {
        return new OpenClawControlService(
                clientStateStore,
                taskStateStore,
                taskRoutingPolicy,
                taskLifecycleListeners.orderedStream().toList(),
                Duration.ofSeconds(properties.getStaleAfterSeconds()),
                Duration.ofSeconds(properties.getOfflineAfterSeconds()),
                properties.getHeartbeatIntervalSeconds(),
                properties.getDefaultTaskLeaseSeconds()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenClawIntegrationRegistry openClawIntegrationRegistry(ObjectProvider<OpenClawRuntimeIntegration> integrations) {
        List<OpenClawRuntimeIntegration> items = integrations.orderedStream().toList();
        return new OpenClawIntegrationRegistry(items);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "openclaw.control", name = "expose-http-endpoints", havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean
    public OpenClawControlController openClawControlController(OpenClawControlService controlService,
                                                               OpenClawIntegrationRegistry integrationRegistry,
                                                               OpenClawClientAuthVerifier clientAuthVerifier) {
        return new OpenClawControlController(controlService, integrationRegistry, clientAuthVerifier);
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenClawClientAuthVerifier openClawClientAuthVerifier(ObjectProvider<ObjectMapper> objectMapperProvider,
                                                                 OpenClawControlProperties properties) {
        if (!properties.getAuth().isEnabled()) {
            return new NoopOpenClawClientAuthVerifier();
        }
        return new ConfigurableOpenClawClientAuthVerifier(properties.getAuth(), buildObjectMapper(objectMapperProvider));
    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(OpenClawMemoryStoreCondition.class)
    static class MemoryStoreConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ClientStateStore openClawClientStateStore() {
            return new InMemoryClientStateStore();
        }

        @Bean
        @ConditionalOnMissingBean
        public TaskStateStore openClawTaskStateStore() {
            return new InMemoryTaskStateStore();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Conditional(OpenClawJdbcStoreCondition.class)
    static class JdbcStoreConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public JdbcTemplate openClawJdbcTemplate(DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Bean
        @ConditionalOnProperty(prefix = "openclaw.control.jdbc", name = "initialize-schema", havingValue = "true", matchIfMissing = true)
        @ConditionalOnMissingBean
        public OpenClawJdbcSchemaInitializer openClawJdbcSchemaInitializer(JdbcTemplate jdbcTemplate,
                                                                           OpenClawControlProperties properties) {
            return new OpenClawJdbcSchemaInitializer(
                    jdbcTemplate,
                    properties.getJdbc().getClientsTableName(),
                    properties.getJdbc().getTasksTableName()
            );
        }

        @Bean
        @ConditionalOnMissingBean
        public ClientStateStore openClawClientStateStore(JdbcTemplate jdbcTemplate,
                                                         ObjectProvider<ObjectMapper> objectMapperProvider,
                                                         OpenClawControlProperties properties) {
            return new JdbcClientStateStore(
                    jdbcTemplate,
                    buildObjectMapper(objectMapperProvider),
                    properties.getJdbc().getClientsTableName()
            );
        }

        @Bean
        @ConditionalOnMissingBean
        public TaskStateStore openClawTaskStateStore(JdbcTemplate jdbcTemplate,
                                                     ObjectProvider<ObjectMapper> objectMapperProvider,
                                                     OpenClawControlProperties properties) {
            return new JdbcTaskStateStore(
                    jdbcTemplate,
                    buildObjectMapper(objectMapperProvider),
                    properties.getJdbc().getTasksTableName()
            );
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "openclaw.control.websocket", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class WebSocketConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public OpenClawControlWebSocketSessionRegistry openClawControlWebSocketSessionRegistry() {
            return new OpenClawControlWebSocketSessionRegistry();
        }

        @Bean
        @ConditionalOnMissingBean
        public OpenClawControlWebSocketHandler openClawControlWebSocketHandler(OpenClawControlService controlService,
                                                                               OpenClawIntegrationRegistry integrationRegistry,
                                                                               OpenClawControlWebSocketSessionRegistry sessionRegistry,
                                                                               ObjectProvider<ObjectMapper> objectMapperProvider,
                                                                               OpenClawClientAuthVerifier clientAuthVerifier) {
            return new OpenClawControlWebSocketHandler(
                    controlService,
                    integrationRegistry,
                    sessionRegistry,
                    buildObjectMapper(objectMapperProvider),
                    clientAuthVerifier
            );
        }

        @Bean
        @ConditionalOnMissingBean
        public TaskLifecycleListener openClawControlTaskPushListener(OpenClawControlWebSocketSessionRegistry sessionRegistry,
                                                                     ObjectProvider<ObjectMapper> objectMapperProvider,
                                                                     OpenClawControlProperties properties) {
            return new OpenClawControlTaskPushBridge(
                    sessionRegistry,
                    buildObjectMapper(objectMapperProvider),
                    properties
            );
        }

        @Bean
        @ConditionalOnMissingBean(name = "openClawControlWebSocketConfigurer")
        public WebSocketConfigurer openClawControlWebSocketConfigurer(OpenClawControlWebSocketHandler handler,
                                                                     OpenClawControlProperties properties) {
            return registry -> registry.addHandler(handler, properties.getWebSocket().getEndpointPath())
                    .setAllowedOriginPatterns(properties.getWebSocket().getAllowedOriginPatterns().toArray(String[]::new));
        }
    }

    private static ObjectMapper buildObjectMapper(ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        if (objectMapper != null) {
            ObjectMapper copy = objectMapper.copy();
            copy.findAndRegisterModules();
            return copy;
        }
        return JsonMapper.builder().findAndAddModules().build();
    }

    static class OpenClawJdbcStoreCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String storeType = context.getEnvironment().getProperty("openclaw.control.store-type", "auto");
            if ("memory".equalsIgnoreCase(storeType)) {
                return false;
            }
            if ("jdbc".equalsIgnoreCase(storeType)) {
                return true;
            }
            return hasDataSourceBean(context);
        }
    }

    static class OpenClawMemoryStoreCondition implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String storeType = context.getEnvironment().getProperty("openclaw.control.store-type", "auto");
            if ("jdbc".equalsIgnoreCase(storeType)) {
                return false;
            }
            if ("memory".equalsIgnoreCase(storeType)) {
                return true;
            }
            return !hasDataSourceBean(context);
        }
    }

    private static boolean hasDataSourceBean(ConditionContext context) {
        if (!ClassUtils.isPresent("javax.sql.DataSource", context.getClassLoader())) {
            return false;
        }
        if (!(context.getBeanFactory() instanceof org.springframework.beans.factory.ListableBeanFactory)) {
            return false;
        }
        org.springframework.beans.factory.ListableBeanFactory beanFactory =
                (org.springframework.beans.factory.ListableBeanFactory) context.getBeanFactory();
        return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, DataSource.class, true, false).length > 0;
    }
}
