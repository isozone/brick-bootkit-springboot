package com.zqzqq.bootkits.core;

import com.zqzqq.bootkits.core.checker.PluginBasicChecker;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptorLoader;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultPluginManagerReloadTest {

    @Test
    void shouldKeepStoppedPluginStoppedAfterReload() throws Exception {
        DefaultPluginManager manager = createManager();
        InsidePluginDescriptor descriptor = createDescriptor("demo-plugin", "plugins/demo-plugin.jar");

        PluginInsideInfo pluginInsideInfo = new DefaultPluginInsideInfo(descriptor);
        pluginInsideInfo.setPluginState(EnhancedPluginState.STOPPED);
        resolvedPlugins(manager).put("demo-plugin", pluginInsideInfo);

        manager.reload("demo-plugin");

        assertThat(manager.getPlugin("demo-plugin").getPluginState()).isEqualTo(EnhancedPluginState.STOPPED);
        assertThat(startedPlugins(manager)).isEmpty();
    }

    @Test
    void shouldRestartPreviouslyStartedPluginAfterReload() throws Exception {
        DefaultPluginManager manager = createManager();
        InsidePluginDescriptor descriptor = createDescriptor("demo-plugin", "plugins/demo-plugin.jar");

        PluginInsideInfo pluginInsideInfo = new DefaultPluginInsideInfo(descriptor);
        pluginInsideInfo.setPluginState(EnhancedPluginState.STARTED);
        resolvedPlugins(manager).put("demo-plugin", pluginInsideInfo);
        startedPlugins(manager).put("demo-plugin", pluginInsideInfo);

        manager.reload("demo-plugin");

        assertThat(manager.getPlugin("demo-plugin").getPluginState()).isEqualTo(EnhancedPluginState.STARTED);
        assertThat(startedPlugins(manager)).containsKey("demo-plugin");
    }

    private DefaultPluginManager createManager() {
        RealizeProvider provider = mock(RealizeProvider.class);
        PluginBasicChecker checker = mock(PluginBasicChecker.class);
        PluginDescriptorLoader descriptorLoader = mock(PluginDescriptorLoader.class);
        IntegrationConfiguration configuration = mock(IntegrationConfiguration.class);

        when(configuration.pluginPath()).thenReturn(Collections.singletonList("plugins"));
        when(configuration.clusterEnabled()).thenReturn(false);
        when(configuration.clusterSharedPath()).thenReturn("");
        when(configuration.clusterLockTimeoutMs()).thenReturn(1000L);
        when(configuration.sortInitPluginIds()).thenReturn(Collections.emptyList());

        when(provider.getPluginBasicChecker()).thenReturn(checker);
        when(provider.getPluginDescriptorLoader()).thenReturn(descriptorLoader);

        InsidePluginDescriptor descriptor = createDescriptor("demo-plugin", "plugins/demo-plugin.jar");
        when(descriptorLoader.load(java.nio.file.Path.of("plugins/demo-plugin.jar"))).thenReturn(descriptor);

        return new DefaultPluginManager(provider, configuration);
    }

    private InsidePluginDescriptor createDescriptor(String pluginId, String pluginPath) {
        InsidePluginDescriptor descriptor = mock(InsidePluginDescriptor.class);
        when(descriptor.getPluginId()).thenReturn(pluginId);
        when(descriptor.getPluginPath()).thenReturn(pluginPath);
        return descriptor;
    }

    @SuppressWarnings("unchecked")
    private Map<String, PluginInsideInfo> resolvedPlugins(DefaultPluginManager manager) throws Exception {
        Field field = DefaultPluginManager.class.getDeclaredField("resolvedPlugins");
        field.setAccessible(true);
        return (Map<String, PluginInsideInfo>) field.get(manager);
    }

    @SuppressWarnings("unchecked")
    private Map<String, PluginInsideInfo> startedPlugins(DefaultPluginManager manager) throws Exception {
        Field field = DefaultPluginManager.class.getDeclaredField("startedPlugins");
        field.setAccessible(true);
        return (Map<String, PluginInsideInfo>) field.get(manager);
    }
}
