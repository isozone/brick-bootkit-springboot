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

import com.zqzqq.bootkits.common.PackageStructure;
import com.zqzqq.bootkits.core.DefaultPluginInsideInfo;
import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.core.descriptor.DefaultInsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.DevPluginDescriptorLoader;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptorLoader;
import com.zqzqq.bootkits.core.descriptor.PluginType;
import com.zqzqq.bootkits.core.descriptor.decrypt.EmptyPluginDescriptorDecrypt;
import com.zqzqq.bootkits.core.launcher.plugin.PluginInteractive;
import com.zqzqq.bootkits.integration.AutoIntegrationConfiguration;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.spring.MainApplicationContext;
import com.zqzqq.bootkits.spring.extract.DefaultOpExtractFactory;
import com.zqzqq.bootkits.spring.extract.OpExtractFactory;
import com.zqzqq.bootkits.spring.invoke.DefaultInvokeSupperCache;
import com.zqzqq.bootkits.spring.invoke.InvokeSupperCache;
import com.zqzqq.bootkits.utils.FilesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * PluginInteractive for standalone plugin startup.
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.1
 */
public class PluginOneselfInteractive implements PluginInteractive {

    private static final Logger log = LoggerFactory.getLogger(PluginOneselfInteractive.class);

    private static final String STANDALONE_PLUGIN_ID_KEY = "plugin.standalone.id";
    private static final String STANDALONE_PLUGIN_VERSION_KEY = "plugin.standalone.version";
    private static final String STANDALONE_PLUGIN_BOOTSTRAP_CLASS_KEY = "plugin.standalone.bootstrapClass";
    private static final String STANDALONE_PLUGIN_DESCRIPTION_KEY = "plugin.standalone.description";
    private static final String STANDALONE_PLUGIN_PROVIDER_KEY = "plugin.standalone.provider";
    private static final String STANDALONE_PLUGIN_REQUIRES_KEY = "plugin.standalone.requires";
    private static final String STANDALONE_PLUGIN_LICENSE_KEY = "plugin.standalone.license";
    private static final String STANDALONE_PLUGIN_ARGS_KEY = "plugin.standalone.args";
    private static final String STANDALONE_PLUGIN_CONFIG_FILE_NAME_KEY = "plugin.standalone.configFileName";
    private static final String STANDALONE_PLUGIN_CONFIG_FILE_LOCATION_KEY = "plugin.standalone.configFileLocation";

    private final Class<?> bootstrapClass;
    private final PluginInsideInfo pluginInsideInfo;
    private final MainApplicationContext mainApplicationContext;
    private final IntegrationConfiguration configuration;
    private final InvokeSupperCache invokeSupperCache;
    private final OpExtractFactory opExtractFactory;

    public PluginOneselfInteractive() {
        this(null);
    }

    public PluginOneselfInteractive(Class<?> bootstrapClass) {
        this.bootstrapClass = bootstrapClass;
        this.pluginInsideInfo = createPluginInsideInfo();
        this.mainApplicationContext = new EmptyMainApplicationContext();
        this.configuration = new AutoIntegrationConfiguration();
        this.invokeSupperCache = new DefaultInvokeSupperCache();
        this.opExtractFactory = new DefaultOpExtractFactory();
    }


    @Override
    public InsidePluginDescriptor getPluginDescriptor() {
        return pluginInsideInfo.getPluginDescriptor();
    }

    @Override
    public PluginInsideInfo getPluginInsideInfo() {
        return pluginInsideInfo;
    }

    @Override
    public MainApplicationContext getMainApplicationContext() {
        return mainApplicationContext;
    }

    @Override
    public IntegrationConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public InvokeSupperCache getInvokeSupperCache() {
        return invokeSupperCache;
    }

    @Override
    public OpExtractFactory getOpExtractFactory() {
        return opExtractFactory;
    }

    private PluginInsideInfo createPluginInsideInfo() {
        EmptyPluginDescriptorDecrypt descriptorDecrypt = new EmptyPluginDescriptorDecrypt();
        try (PluginDescriptorLoader pluginDescriptorLoader = new DevPluginDescriptorLoader(descriptorDecrypt)) {
            Class<?> runnerClass = getRunnerClass();
            Path classesPath = resolveClassesPath(runnerClass);
            Path metaInfPath = Paths.get(FilesUtils.joiningFilePath(classesPath.toString(), PackageStructure.META_INF_NAME));

            InsidePluginDescriptor pluginDescriptor = pluginDescriptorLoader.load(metaInfPath);
            if (pluginDescriptor != null) {
                return new DefaultPluginInsideInfo(pluginDescriptor);
            }

            log.warn("No PLUGIN.META found under [{}], fallback to standalone descriptor.", metaInfPath);
            return new DefaultPluginInsideInfo(createFallbackDescriptor(runnerClass, metaInfPath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> getRunnerClass() {
        if (bootstrapClass != null) {
            return bootstrapClass;
        }
        return this.getClass();
    }

    private Path resolveClassesPath(Class<?> runnerClass) throws Exception {
        URL classesUrl = runnerClass.getResource("/");
        if (classesUrl == null) {
            throw new RuntimeException("Can not resolve classes path from " + runnerClass.getName());
        }
        // .../target/classes -> .../target
        return Paths.get(classesUrl.toURI()).getParent();
    }

    private InsidePluginDescriptor createFallbackDescriptor(Class<?> runnerClass, Path metaInfPath) {
        String pluginId = read(STANDALONE_PLUGIN_ID_KEY);
        if (pluginId == null) {
            pluginId = toPluginId(runnerClass.getSimpleName());
        }
        String pluginVersion = read(STANDALONE_PLUGIN_VERSION_KEY);
        if (pluginVersion == null) {
            pluginVersion = "0.0.0-standalone";
        }
        String bootstrapClassName = read(STANDALONE_PLUGIN_BOOTSTRAP_CLASS_KEY);
        if (bootstrapClassName == null) {
            bootstrapClassName = runnerClass.getName();
        }

        DefaultInsidePluginDescriptor descriptor = new DefaultInsidePluginDescriptor(
                pluginId,
                pluginVersion,
                bootstrapClassName,
                metaInfPath
        );
        applyDescriptorSetter(descriptor, "setType", PluginType.class, PluginType.DEV);
        descriptor.setPluginClassPath("classes");
        applyDescriptorSetter(descriptor, "setDescription", String.class, read(STANDALONE_PLUGIN_DESCRIPTION_KEY));
        applyDescriptorSetter(descriptor, "setProvider", String.class, read(STANDALONE_PLUGIN_PROVIDER_KEY));
        applyDescriptorSetter(descriptor, "setRequires", String.class, read(STANDALONE_PLUGIN_REQUIRES_KEY));
        applyDescriptorSetter(descriptor, "setLicense", String.class, read(STANDALONE_PLUGIN_LICENSE_KEY));
        descriptor.setArgs(read(STANDALONE_PLUGIN_ARGS_KEY));
        descriptor.setConfigFileName(read(STANDALONE_PLUGIN_CONFIG_FILE_NAME_KEY));
        descriptor.setConfigFileLocation(read(STANDALONE_PLUGIN_CONFIG_FILE_LOCATION_KEY));

        Properties properties = new Properties();
        properties.setProperty("plugin.id", pluginId);
        properties.setProperty("plugin.version", pluginVersion);
        properties.setProperty("plugin.bootstrapClass", bootstrapClassName);
        properties.setProperty("plugin.system.path", metaInfPath.toAbsolutePath().toString());
        descriptor.setProperties(properties);

        return descriptor;
    }

    private String read(String key) {
        String value = System.getProperty(key);
        if (value == null || "".equals(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private String toPluginId(String simpleClassName) {
        if (simpleClassName == null || "".equals(simpleClassName.trim())) {
            return "standalone-plugin";
        }
        String normalized = simpleClassName.replaceAll("Plugin$", "");
        if ("".equals(normalized)) {
            normalized = simpleClassName;
        }
        String kebab = normalized.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase();
        if ("".equals(kebab)) {
            return "standalone-plugin";
        }
        return kebab + "-plugin";
    }

    private void applyDescriptorSetter(DefaultInsidePluginDescriptor descriptor,
                                       String methodName,
                                       Class<?> type,
                                       Object value) {
        if (value == null) {
            return;
        }
        try {
            Method method = descriptor.getClass().getSuperclass().getDeclaredMethod(methodName, type);
            method.setAccessible(true);
            method.invoke(descriptor, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to apply descriptor field by " + methodName, e);
        }
    }
}
