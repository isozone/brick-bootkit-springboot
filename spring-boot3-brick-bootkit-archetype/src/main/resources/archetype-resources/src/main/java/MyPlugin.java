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


package ;

import com.zqzqq.bootkits.sdk.annotation.BrickPlugin;
import com.zqzqq.bootkits.sdk.annotation.BrickService;
import com.zqzqq.bootkits.sdk.annotation.BrickEventListener;
import com.zqzqq.bootkits.core.eventbus.PluginEvent;
import com.zqzqq.bootkits.core.plugin.AbstractPlugin;

/**
 * 插件示例
 * 
 * 使用方法:
 * 1. 继承 AbstractPlugin
 * 2. 使用 @BrickPlugin 注解标记
 * 3. 使用 @BrickService 暴露服务
 * 4. 使用 @BrickEventListener 监听事件
 * 
 * @author 你的名字
 * @since 1.0.0
 */
@BrickPlugin(
    id = ".my-plugin",
    name = "My Plugin",
    description = "Brick BootKit 插件示例",
    author = "你的名字",
    version = "1.0.0"
)
public class MyPlugin extends AbstractPlugin {

    @Override
    public void start() throws Exception {
        System.out.println("插件启动: " + getId());
    }

    @Override
    public void stop() throws Exception {
        System.out.println("插件停止: " + getId());
    }

    @Override
    public void uninstall() throws Exception {
        System.out.println("插件卸载: " + getId());
    }

    /**
     * 示例服务
     */
    @BrickService(value = MyService.class, version = "1.0.0")
    public static class MyService {
        public String sayHello(String name) {
            return "Hello, " + name + "!";
        }
    }

    /**
     * 示例事件监听器
     */
    @BrickEventListener(value = {
        PluginEvent.EventType.PLUGIN_STARTED,
        PluginEvent.EventType.PLUGIN_INSTALLED
    }, priority = 1, async = true)
    public void onPluginEvent(PluginEvent event) {
        System.out.println("收到事件: " + event.getType() 
                + " from: " + event.getSourcePluginId());
    }
}
