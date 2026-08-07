package com.zqzqq.bootkits.web.service;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.PluginManager;
import com.zqzqq.bootkits.core.exception.PluginException;
import com.zqzqq.bootkits.core.state.EnhancedPluginState;
import com.zqzqq.bootkits.web.config.BrickWebProperties;
import com.zqzqq.bootkits.web.dto.MarketplacePluginDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 插件市场 Web 服务单元测试。
 * 使用 web 模块内置的 classpath 索引（marketplace/index.json）验证。
 */
@DisplayName("PluginMarketplaceService Test")
class PluginMarketplaceServiceTest {

    private PluginManager pluginManager;
    private PluginWebService pluginWebService;
    private BrickWebProperties properties;
    private PluginMarketplaceService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        pluginManager = mock(PluginManager.class);
        pluginWebService = mock(PluginWebService.class);
        properties = new BrickWebProperties();

        ObjectProvider<PluginManager> pmProvider = mock(ObjectProvider.class);
        when(pmProvider.getIfAvailable()).thenReturn(pluginManager);
        ObjectProvider<PluginWebService> pwProvider = mock(ObjectProvider.class);
        when(pwProvider.getIfAvailable()).thenReturn(pluginWebService);

        service = new PluginMarketplaceService(pmProvider, pwProvider, properties);
    }

    @Test
    @DisplayName("未配置索引时从内置 classpath 索引加载清单")
    void listMarketplaceShouldLoadBuiltinIndex() {
        // 未配置 marketplaceIndexUrl → 回退 classpath marketplace/index.json
        List<MarketplacePluginDTO> plugins = service.listMarketplace();

        assertThat(plugins).isNotEmpty();
        assertThat(plugins.get(0).getPluginId()).isNotBlank();
    }

    @Test
    @DisplayName("已安装插件标记为 installed")
    void listMarketplaceShouldMarkInstalled() {
        PluginInfo pluginInfo = mock(PluginInfo.class);
        when(pluginInfo.getPluginId()).thenReturn("demoTestUploadPlus");
        when(pluginInfo.getPluginState()).thenReturn(EnhancedPluginState.STARTED);
        when(pluginManager.getPlugins()).thenReturn(Collections.singletonList(pluginInfo));

        List<MarketplacePluginDTO> plugins = service.listMarketplace();

        MarketplacePluginDTO demo = plugins.stream()
                .filter(p -> "demoTestUploadPlus".equals(p.getPluginId()))
                .findFirst().orElse(null);
        assertThat(demo).isNotNull();
        assertThat(demo.isInstalled()).isTrue();
        assertThat(demo.getState()).isEqualTo("STARTED");
    }

    @Test
    @DisplayName("市场不存在该插件时抛出异常")
    void installFromMarketplaceShouldFailWhenPluginMissing() {
        assertThatThrownBy(() -> service.installFromMarketplace("missing", true))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("不存在该插件");
    }
}
