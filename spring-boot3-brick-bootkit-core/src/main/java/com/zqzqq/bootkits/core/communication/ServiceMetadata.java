/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.core.communication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service metadata.
 * <p>
 * Contains non-functional attributes of a service such as version, priority,
 * health check configuration, circuit breaker configuration, etc.
 *
 * @author brick-bootkit
 * @version 1.0.0
 * @since 2024/01/01
 */
public class ServiceMetadata {

    /**
     * Service name (human-readable).
     */
    private String name = "";

    /**
     * Service description.
     */
    private String description = "";

    /**
     * Semantic version.
     */
    private String version = "1.0.0";

    /**
     * Priority (lower number = higher priority).
     */
    private int priority = 0;

    /**
     * Whether this is a singleton service.
     */
    private boolean singleton = true;

    /**
     * Whether this service is enabled.
     */
    private boolean enabled = true;

    /**
     * Tags for grouping and filtering.
     */
    private Set<String> tags = new HashSet<>();

    /**
     * Additional configuration properties.
     */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Health check configuration.
     */
    private HealthCheckConfig healthCheck;

    /**
     * Circuit breaker configuration.
     */
    private CircuitBreakerConfig circuitBreaker;

    /**
     * Load balancing strategy.
     */
    private LoadBalancingStrategy loadBalancing = LoadBalancingStrategy.ROUND_ROBIN;

    /**
     * Default constructor.
     */
    public ServiceMetadata() {
    }

    /**
     * Constructor with basic fields.
     */
    public ServiceMetadata(String name, String version) {
        this.name = name;
        this.version = version;
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(boolean singleton) {
        this.singleton = singleton;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public HealthCheckConfig getHealthCheck() {
        return healthCheck;
    }

    public void setHealthCheck(HealthCheckConfig healthCheck) {
        this.healthCheck = healthCheck;
    }

    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreakerConfig circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public LoadBalancingStrategy getLoadBalancing() {
        return loadBalancing;
    }

    public void setLoadBalancing(LoadBalancingStrategy loadBalancing) {
        this.loadBalancing = loadBalancing;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void addProperty(String key, Object value) {
        this.properties.put(key, value);
    }

    /**
     * Health check configuration.
     */
    public static class HealthCheckConfig {
        private boolean enabled = true;
        private int intervalSeconds = 30;
        private int timeoutMillis = 5000;
        private int failureThreshold = 3;
        private int successThreshold = 2;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public void setIntervalSeconds(int intervalSeconds) {
            this.intervalSeconds = intervalSeconds;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public int getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(int failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public int getSuccessThreshold() {
            return successThreshold;
        }

        public void setSuccessThreshold(int successThreshold) {
            this.successThreshold = successThreshold;
        }
    }

    /**
     * Circuit breaker configuration.
     */
    public static class CircuitBreakerConfig {
        private double failureRateThreshold = 50.0;
        private int minimumNumberOfCalls = 10;
        private int slidingWindowSize = 60;
        private int waitDurationInOpenState = 30;
        private int permittedNumberOfCallsInHalfOpenState = 3;
        private boolean automaticTransitionFromOpenToHalfOpenEnabled = true;

        public double getFailureRateThreshold() {
            return failureRateThreshold;
        }

        public void setFailureRateThreshold(double failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
        }

        public int getMinimumNumberOfCalls() {
            return minimumNumberOfCalls;
        }

        public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
        }

        public int getSlidingWindowSize() {
            return slidingWindowSize;
        }

        public void setSlidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
        }

        public int getWaitDurationInOpenState() {
            return waitDurationInOpenState;
        }

        public void setWaitDurationInOpenState(int waitDurationInOpenState) {
            this.waitDurationInOpenState = waitDurationInOpenState;
        }

        public int getPermittedNumberOfCallsInHalfOpenState() {
            return permittedNumberOfCallsInHalfOpenState;
        }

        public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) {
            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
        }

        public boolean isAutomaticTransitionFromOpenToHalfOpenEnabled() {
            return automaticTransitionFromOpenToHalfOpenEnabled;
        }

        public void setAutomaticTransitionFromOpenToHalfOpenEnabled(boolean automaticTransitionFromOpenToHalfOpenEnabled) {
            this.automaticTransitionFromOpenToHalfOpenEnabled = automaticTransitionFromOpenToHalfOpenEnabled;
        }
    }

    /**
     * Load balancing strategy.
     */
    public enum LoadBalancingStrategy {
        ROUND_ROBIN,
        RANDOM,
        WEIGHTED,
        LEAST_CONNECTIONS
    }

    /**
     * Builder for ServiceMetadata.
     */
    public static class Builder {
        private final ServiceMetadata metadata = new ServiceMetadata();

        public Builder name(String name) {
            metadata.setName(name);
            return this;
        }

        public Builder version(String version) {
            metadata.setVersion(version);
            return this;
        }

        public Builder description(String description) {
            metadata.setDescription(description);
            return this;
        }

        public Builder priority(int priority) {
            metadata.setPriority(priority);
            return this;
        }

        public Builder singleton(boolean singleton) {
            metadata.setSingleton(singleton);
            return this;
        }

        public Builder enabled(boolean enabled) {
            metadata.setEnabled(enabled);
            return this;
        }

        public Builder healthCheck(HealthCheckConfig healthCheck) {
            metadata.setHealthCheck(healthCheck);
            return this;
        }

        public Builder circuitBreaker(CircuitBreakerConfig circuitBreaker) {
            metadata.setCircuitBreaker(circuitBreaker);
            return this;
        }

        public Builder loadBalancing(LoadBalancingStrategy loadBalancing) {
            metadata.setLoadBalancing(loadBalancing);
            return this;
        }

        public ServiceMetadata build() {
            return metadata;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
