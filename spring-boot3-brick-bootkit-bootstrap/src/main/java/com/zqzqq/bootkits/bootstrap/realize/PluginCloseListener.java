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



package com.zqzqq.bootkits.bootstrap.realize;

import com.zqzqq.bootkits.core.PluginCloseType;
import com.zqzqq.bootkits.core.PluginInfo;
import com.zqzqq.bootkits.core.descriptor.PluginDescriptor;
import org.springframework.context.support.GenericApplicationContext;

/**
 * 插件琚仠姝㈢洃的呫傜敤浜庤嚜瀹氫箟关闭资源
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.1.0
 */
public interface PluginCloseListener {

    /**
     * 关闭时调用
     * @param descriptor 当前插件描述者
     * @deprecated 在 3.1.1 版本会被删除
     * @since 3.0.0
     */
    default void close(PluginDescriptor descriptor){}

    /**
     * 关闭时调用
     * @param applicationContext 当前插件的ApplicationContext
     * @param pluginInfo 当前插件信息
     * @param closeType 停止类型
     * @since 3.1.0
     */
    default void close(GenericApplicationContext applicationContext,
                       PluginInfo pluginInfo, PluginCloseType closeType){
        close(pluginInfo != null ? pluginInfo.getPluginDescriptor() : null);
    }

}

