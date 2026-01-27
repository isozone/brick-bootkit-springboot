/**
 * Copyright [2019-Present] [starBlues]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.zqzqq.bootkits.spring.web.thymeleaf;

import com.zqzqq.bootkits.core.PluginInsideInfo;
import com.zqzqq.bootkits.core.descriptor.InsidePluginDescriptor;
import com.zqzqq.bootkits.core.launcher.plugin.involved.PluginLaunchInvolved;
import com.zqzqq.bootkits.integration.IntegrationConfiguration;
import com.zqzqq.bootkits.spring.SpringPluginHook;
import com.zqzqq.bootkits.utils.ClassUtils;
import com.zqzqq.bootkits.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件 Thymeleaf 注册
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
public class PluginThymeleafInvolved implements PluginLaunchInvolved {

    private static final Logger logger = LoggerFactory.getLogger(PluginThymeleafInvolved.class);

    private Set<ITemplateResolver> templateResolvers;

    private final Map<String, ClassLoaderTemplateResolver> pluginTemplateResolver = new ConcurrentHashMap<>();

    @Override
    public void initialize(GenericApplicationContext applicationContext, IntegrationConfiguration configuration) {
        this.templateResolvers = getTemplateResolvers(getSpringTemplateEngine(applicationContext));
    }

    @Override
    public void after(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader, SpringPluginHook pluginHook) throws Exception {
        if(templateResolvers == null){
            return;
        }

        ThymeleafConfig thymeleafConfig = pluginHook.getThymeleafConfig();
        if(thymeleafConfig == null || !thymeleafConfig.isEnabled()){
            return;
        }

        String prefix = thymeleafConfig.getPrefix();
        if(ObjectUtils.isEmpty(prefix)){
            throw new IllegalArgumentException("prefix can't be empty");
        } else {
            if(!prefix.endsWith("/")){
                thymeleafConfig.setPrefix(prefix + "/");
            }
        }
        if(ObjectUtils.isEmpty(thymeleafConfig.getSuffix())){
            throw new IllegalArgumentException("suffix can't be empty");
        }

        if(thymeleafConfig.getMode() == null){
            throw new IllegalArgumentException("mode can't be null");
        }

        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver(classLoader);
        resolver.setPrefix(thymeleafConfig.getPrefix() + "/");
        resolver.setSuffix(thymeleafConfig.getSuffix());
        resolver.setTemplateMode(thymeleafConfig.getMode());

        resolver.setCacheable(thymeleafConfig.getCache());
        if(thymeleafConfig.getEncoding() != null){
            resolver.setCharacterEncoding(thymeleafConfig.getEncoding().name());
        }
        Integer order = thymeleafConfig.getTemplateResolverOrder();
        if(order != null){
            resolver.setOrder(order);
        }
        resolver.setCheckExistence(true);
        templateResolvers.add(resolver);
        InsidePluginDescriptor descriptor = pluginInsideInfo.getPluginDescriptor();
        if(!pluginTemplateResolver.containsKey(descriptor.getPluginId())){
            pluginTemplateResolver.put(descriptor.getPluginId(), resolver);
        }
    }

    @Override
    public void close(PluginInsideInfo pluginInsideInfo, ClassLoader classLoader) throws Exception {
        pluginTemplateResolver.remove(pluginInsideInfo.getPluginId());
    }

    private SpringTemplateEngine getSpringTemplateEngine(GenericApplicationContext context){
        String[] beanNamesForType = context.getBeanNamesForType(SpringTemplateEngine.class,
                false, false);
        if(beanNamesForType.length == 0){
            return null;
        }
        try {
            return context.getBean(SpringTemplateEngine.class);
        } catch (Exception e){
            return null;
        }
    }

    private Set<ITemplateResolver> getTemplateResolvers(SpringTemplateEngine springTemplateEngine) {
        String errorMsg = "当前插件不能使用Thymeleaf, 主程序未发现Thymeleaf注册接口";
        if(springTemplateEngine == null){
            logger.error(errorMsg);
        }
        try {
            Set<ITemplateResolver> templateResolvers = ClassUtils.getReflectionField(springTemplateEngine, "templateResolvers");
            if(templateResolvers == null) {
                logger.error(errorMsg);
            }
            return templateResolvers;
        } catch (Exception e){
            logger.error("褰撳墠插件不能使用Thymeleaf, 获取主程序搴忔敞鍐屽叆鍙ｅけ璐? {}", e.getMessage());
            return null;
        }
    }

}

