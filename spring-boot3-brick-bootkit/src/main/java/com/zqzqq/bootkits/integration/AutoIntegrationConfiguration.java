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



package com.zqzqq.bootkits.integration;

import com.zqzqq.bootkits.core.RuntimeMode;
import com.zqzqq.bootkits.core.admission.PluginAdmissionMode;
import com.zqzqq.bootkits.integration.decrypt.DecryptConfiguration;
import com.zqzqq.bootkits.integration.rollout.PluginRolloutMode;
import com.zqzqq.bootkits.utils.ResourceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Set;

/**
 * Auto integration configuration.
 */
@Configuration
@ConditionalOnMissingBean(IntegrationConfiguration.class)
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "plugin")
@Data
public class AutoIntegrationConfiguration extends DefaultIntegrationConfiguration {

    public static final String ENABLE_KEY = "plugin.enable";

    private Boolean enable = true;
    private String runMode = RuntimeMode.DEV.getMode();
    private String mainPackage = "";
    private List<String> pluginPath;
    private String uploadTempPath = "";
    private String backupPath = "backupPlugin";
    private String pluginRestPathPrefix = "/plugins";
    private Boolean enablePluginIdRestPathPrefix = true;
    private Set<String> enablePluginIds;
    private Set<String> disablePluginIds;
    private List<String> sortInitPluginIds;
    private String version = "0.0.0";
    private Boolean exactVersion = false;
    private Boolean pluginSwaggerScan = true;
    private Boolean pluginFollowProfile = false;
    private Boolean pluginFollowLog = false;
    private DecryptConfiguration decrypt;

    private Boolean clusterEnabled = false;
    private String clusterSharedPath = "";
    private String clusterWebBaseUrl = "";
    private Long clusterLockTimeoutMs = 30000L;
    private String clusterLockProviderBeanName = "";

    private String admissionMode = "warn";
    private Boolean lifecycleExtensionsEnabled = true;

    private Boolean migrationValidateChecksum = true;
    private Boolean migrationContinueOnError = false;

    private String rolloutMode = "direct";
    private Boolean rolloutAutoStart = true;
    private Boolean rolloutRollbackOnFailure = true;

    @Override
    public Boolean enable() {
        if (enable == null) {
            return true;
        }
        return enable;
    }

    @Override
    public RuntimeMode environment() {
        return RuntimeMode.byName(runMode);
    }

    @Override
    public String mainPackage() {
        return ResourceUtils.replacePackage(mainPackage);
    }

    @Override
    public List<String> pluginPath() {
        if (ObjectUtils.isEmpty(pluginPath)) {
            return super.pluginPath();
        }
        return pluginPath;
    }

    @Override
    public String uploadTempPath() {
        if (ObjectUtils.isEmpty(uploadTempPath)) {
            return super.uploadTempPath();
        }
        return uploadTempPath;
    }

    @Override
    public String backupPath() {
        if (ObjectUtils.isEmpty(backupPath)) {
            return super.backupPath();
        }
        return backupPath;
    }

    @Override
    public String pluginRestPathPrefix() {
        if (pluginRestPathPrefix == null) {
            return super.pluginRestPathPrefix();
        }
        return pluginRestPathPrefix;
    }

    @Override
    public Boolean enablePluginIdRestPathPrefix() {
        if (enablePluginIdRestPathPrefix == null) {
            return super.enablePluginIdRestPathPrefix();
        }
        return enablePluginIdRestPathPrefix;
    }

    @Override
    public Set<String> enablePluginIds() {
        return enablePluginIds;
    }

    @Override
    public Set<String> disablePluginIds() {
        return disablePluginIds;
    }

    @Override
    public List<String> sortInitPluginIds() {
        return sortInitPluginIds;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public Boolean exactVersion() {
        return exactVersion;
    }

    @Override
    public Boolean pluginSwaggerScan() {
        if (pluginSwaggerScan == null) {
            return super.pluginSwaggerScan();
        }
        return pluginSwaggerScan;
    }

    @Override
    public Boolean pluginFollowProfile() {
        if (pluginFollowProfile == null) {
            return super.pluginFollowProfile();
        }
        return pluginFollowProfile;
    }

    @Override
    public Boolean pluginFollowLog() {
        if (pluginFollowLog == null) {
            return super.pluginFollowLog();
        }
        return pluginFollowLog;
    }

    @Override
    public DecryptConfiguration decrypt() {
        if (decrypt == null) {
            return super.decrypt();
        }
        return decrypt;
    }

    @Override
    public Boolean clusterEnabled() {
        if (clusterEnabled == null) {
            return super.clusterEnabled();
        }
        return clusterEnabled;
    }

    @Override
    public String clusterSharedPath() {
        if (ObjectUtils.isEmpty(clusterSharedPath)) {
            return super.clusterSharedPath();
        }
        return clusterSharedPath;
    }

    @Override
    public String clusterWebBaseUrl() {
        if (ObjectUtils.isEmpty(clusterWebBaseUrl)) {
            return super.clusterWebBaseUrl();
        }
        return clusterWebBaseUrl;
    }

    @Override
    public Long clusterLockTimeoutMs() {
        if (clusterLockTimeoutMs == null || clusterLockTimeoutMs <= 0L) {
            return super.clusterLockTimeoutMs();
        }
        return clusterLockTimeoutMs;
    }

    @Override
    public String clusterLockProviderBeanName() {
        if (ObjectUtils.isEmpty(clusterLockProviderBeanName)) {
            return super.clusterLockProviderBeanName();
        }
        return clusterLockProviderBeanName;
    }

    @Override
    public PluginAdmissionMode pluginAdmissionMode() {
        return PluginAdmissionMode.fromText(admissionMode, super.pluginAdmissionMode());
    }

    @Override
    public Boolean pluginLifecycleExtensionsEnabled() {
        if (lifecycleExtensionsEnabled == null) {
            return super.pluginLifecycleExtensionsEnabled();
        }
        return lifecycleExtensionsEnabled;
    }

    @Override
    public boolean migrationValidateChecksum() {
        if (migrationValidateChecksum == null) {
            return super.migrationValidateChecksum();
        }
        return migrationValidateChecksum;
    }

    @Override
    public boolean migrationContinueOnError() {
        if (migrationContinueOnError == null) {
            return super.migrationContinueOnError();
        }
        return migrationContinueOnError;
    }

    @Override
    public PluginRolloutMode pluginRolloutMode() {
        return PluginRolloutMode.fromText(rolloutMode, super.pluginRolloutMode());
    }

    @Override
    public boolean pluginRolloutAutoStart() {
        if (rolloutAutoStart == null) {
            return super.pluginRolloutAutoStart();
        }
        return rolloutAutoStart;
    }

    @Override
    public boolean pluginRolloutRollbackOnFailure() {
        if (rolloutRollbackOnFailure == null) {
            return super.pluginRolloutRollbackOnFailure();
        }
        return rolloutRollbackOnFailure;
    }
}
