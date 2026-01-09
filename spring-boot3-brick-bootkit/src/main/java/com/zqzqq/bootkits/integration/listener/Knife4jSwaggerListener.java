package com.zqzqq.bootkits.integration.listener;

import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import com.zqzqq.bootkits.utils.MsgUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Knife4j Swagger 监听事件
 * @version 1.0.0
 */
public class Knife4jSwaggerListener implements PluginListener {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private final ApplicationContext mainApplicationContext;

    public Knife4jSwaggerListener(ApplicationContext mainApplicationContext) {
        this.mainApplicationContext = mainApplicationContext;
    }

    @Override
    public void startSuccess(PluginInfo pluginInfo) {
        PluginDescriptor descriptor = pluginInfo.getPluginDescriptor();
        try {
            OpenAPI openAPI = mainApplicationContext.getBean(OpenAPI.class);
            if(openAPI != null) {
                addPluginApiInfo(openAPI, descriptor);
                log.debug("插件[{}]注册锟?Knife4j 鎴愬姛", MsgUtils.getPluginUnique(descriptor));
            }
        } catch (Exception e) {
            log.error("插件[{}]注册到Knife4j失败: {}", MsgUtils.getPluginUnique(descriptor), e);
        }
    }

    @Override
    public void stopSuccess(PluginInfo pluginInfo) {
        // Knife4j浼氳嚜鍔ㄥ鐞咥PI鏂囨。鏇存柊锛屾棤闇€鎵嬪姩操作
        log.debug("插件[{}]锟?Knife4j 移除鎴愬姛", MsgUtils.getPluginUnique(pluginInfo.getPluginDescriptor()));
    }

    private void addPluginApiInfo(OpenAPI openAPI, PluginDescriptor descriptor) {
        String description = descriptor.getDescription();
        if (description == null || description.isEmpty()) {
            description = descriptor.getPluginId();
        }

        String provider = descriptor.getProvider();
        Contact contact = new Contact()
                .name(provider)
                .url("");

        Info info = new Info()
                .title(getGroupName(descriptor))
                .description(description)
                .contact(contact)
                .version(descriptor.getPluginVersion());

        openAPI.addTagsItem(new io.swagger.v3.oas.models.tags.Tag()
                .name(getGroupName(descriptor))
                .description(description));
    }

    private String getGroupName(PluginDescriptor descriptor) {
        return descriptor.getPluginId() + "@" + descriptor.getPluginVersion();
    }
}
