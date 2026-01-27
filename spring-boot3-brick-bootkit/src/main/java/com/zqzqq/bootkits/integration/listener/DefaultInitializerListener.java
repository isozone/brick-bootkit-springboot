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

package com.zqzqq.bootkits.integration.listener;

import com.zqzqq.bootkits.utils.SpringBeanUtils;
import org.springframework.context.ApplicationContext;

/**
 * 初始化监听器的默认实现。内部注册监听?
 *
 * @author starBlues
 * @version 3.0.0
 */
public class DefaultInitializerListener implements PluginInitializerListener{

    private final Knife4jSwaggerListener swaggerListener;

    public DefaultInitializerListener(ApplicationContext applicationContext) {
        this.swaggerListener = SpringBeanUtils.getExistBean(applicationContext, Knife4jSwaggerListener.class);
    }


    @Override
    public void before() {

    }

    @Override
    public void complete() {
        refresh();
    }

    @Override
    public void failure(Throwable throwable) {
        refresh();
    }

    private void refresh(){
        // Knife4j会自动处理API文档更新，无需手动操作
        if(swaggerListener != null){
            // 未实现，Knife4j会自动处理文档更新?
        }
    }

}