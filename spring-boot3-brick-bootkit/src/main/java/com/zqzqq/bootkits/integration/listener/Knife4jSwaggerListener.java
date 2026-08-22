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
                log.debug("插件[{}]注册到Knife4j 成功", MsgUtils.getPluginUnique(descriptor));
            }
        } catch (Exception e) {
            log.error("插件[{}]注册到Knife4j失败: {}", MsgUtils.getPluginUnique(descriptor), e);
        }
    }

    @Override
    public void stopSuccess(PluginInfo pluginInfo) {
        // Knife4j会自动处理API文档更新，无需手动操作
        log.debug("插件[{}]从Knife4j 移除成功", MsgUtils.getPluginUnique(pluginInfo.getPluginDescriptor()));
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
