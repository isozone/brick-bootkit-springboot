/**
 * Copyright [2019-Present] [starBlues]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zqzqq.bootkits.bootstrap;

import com.zqzqq.bootkits.bootstrap.processor.ProcessorContext;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.integration.AutoIntegrationConfiguration;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.loader.launcher.DevelopmentModeSetting;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 插件环境配置
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.2
 */
public class ConfigurePluginEnvironment {
    private final Logger logger = LoggerFactory.getLogger(ConfigurePluginEnvironment.class);

    private final static String PLUGIN_PROPERTY_NAME = "pluginPropertySources";

    private final static String SPRING_CONFIG_NAME = "spring.config.name";
    private final static String SPRING_CONFIG_LOCATION = "spring.config.location";

    private final static String SPRING_JMX_UNIQUE_NAMES = "spring.jmx.unique-names";
    private final static String SPRING_ADMIM_ENABLED = "spring.application.admin.enabled";
    private final static String SPRING_ADMIN_JMX_NAME = "spring.application.admin.jmx-name";
    private final static String SPRING_ADMIN_JMX_VALUE = "org.springframework.boot:type=Admin,name=";

    public static final String REGISTER_SHUTDOWN_HOOK_PROPERTY = "logging.register-shutdown-hook";

    private final ProcessorContext processorContext;
    private final InsidePluginDescriptor pluginDescriptor;

    public ConfigurePluginEnvironment(ProcessorContext processorContext) {
        this.processorContext = Assert.isNotNull(processorContext, "processorContext 不能为空");
        this.pluginDescriptor = Assert.isNotNull(processorContext.getPluginDescriptor(),
                "pluginDescriptor 不能为空");
    }

    public void configureEnvironment(ConfigurableEnvironment environment, String[] args) {
        Map<String, Object> env = new HashMap<>();
        String pluginId = pluginDescriptor.getPluginId();
        String configFileName = pluginDescriptor.getConfigFileName();
        if(!ObjectUtils.isEmpty(configFileName)){
            env.put(SPRING_CONFIG_NAME, PluginFileUtils.getFileName(configFileName));
        }
        String configFileLocation = pluginDescriptor.getConfigFileLocation();
        if(!ObjectUtils.isEmpty(configFileLocation)){
            env.put(SPRING_CONFIG_LOCATION, getConfigFileLocation(configFileLocation));
        }
        env.put(AutoIntegrationConfiguration.ENABLE_KEY, false);
        env.put(SPRING_JMX_UNIQUE_NAMES, true);
        // 直接禁用插件spring-admin mbean
        env.put(SPRING_ADMIM_ENABLED, false);
        env.put(SPRING_ADMIN_JMX_NAME, SPRING_ADMIN_JMX_VALUE + pluginId);
        env.put(REGISTER_SHUTDOWN_HOOK_PROPERTY, false);


        // LiveBeansView removed in Spring Boot 3.x
        // JMX monitoring can be configured via other means if needed
        if(DevelopmentModeSetting.coexist()){
            env.put(AutoIntegrationConfiguration.ENABLE_KEY, false);
        }
        configProfiles(environment);
        environment.getPropertySources().addFirst(new MapPropertySource(PLUGIN_PROPERTY_NAME, env));
    }

    public void logProfiles(ConfigurableEnvironment environment){
        IntegrationConfiguration configuration = processorContext.getConfiguration();
        String fromMainMsg = configuration.pluginFollowProfile() ? " from main" : " ";
        String[] activeProfiles = environment.getActiveProfiles();
        if(activeProfiles.length > 0){
            logger.info("Plugin[{}] following profiles are active{}: {}",
                    MsgUtils.getPluginUnique(pluginDescriptor),
                    fromMainMsg,
                    StringUtils.toStrByArray(activeProfiles));
        } else {
            logger.info("Plugin[{}]  No active profile set, falling back to default profiles{}: {}",
                    MsgUtils.getPluginUnique(pluginDescriptor),
                    fromMainMsg,
                    StringUtils.toStrByArray(environment.getDefaultProfiles()));
        }
    }

    private void configProfiles(ConfigurableEnvironment environment){
        IntegrationConfiguration configuration = processorContext.getConfiguration();
        if(!configuration.pluginFollowProfile()){
            return;
        }
        MainApplicationContext mainApplicationContext = processorContext.getMainApplicationContext();
        String[] mainActiveProfiles = mainApplicationContext.getActiveProfiles();
        if(mainActiveProfiles.length > 0){
            environment.setActiveProfiles(mainActiveProfiles);
        } else {
            logger.info("Plugin[{}]  No active profile set, falling back to default profiles: {}",
                    MsgUtils.getPluginUnique(pluginDescriptor),
                    StringUtils.toStrByArray(environment.getDefaultProfiles()));
        }
    }


    private String getConfigFileLocation(String configFileLocation){
        String path = FilesUtils.resolveRelativePath(new File("").getAbsolutePath(), configFileLocation);
        // 拼接路径字符
        if(path.endsWith(FilesUtils.SLASH) || path.endsWith(File.separator)){
            return path;
        } else {
            return path + File.separator;
        }
    }

}

