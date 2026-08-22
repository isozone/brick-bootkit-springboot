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



package com.zqzqq.bootkits.integration.decrypt;

import com.zqzqq.bootkits.common.cipher.AesPluginCipher;
import com.zqzqq.bootkits.common.cipher.RsaPluginCipher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对插件启动时进行解密校验配置。默认不启用。
 *
 * @author starBlues
 * @version 3.0.1
 */
public class DecryptConfiguration {

    /**
     * 是否启动. 默认启用
     */
    private Boolean enable = true;

    /**
     * 加解密实现类名称.
     * 通过类加载器加载, 然后从Spring容器中获取, 获取不到, 对其直接实例
     * 默认: {@link AesPluginCipher}
     * 可以是:
     * @see AesPluginCipher
     * @see RsaPluginCipher
     */
    private String className = AesPluginCipher.class.getName();

    /**
     * 总配置
     */
    private Map<String, Object> props = new HashMap<>();

    /**
     * 指定可生效的插件. 如果不配置, 则默认对全部插件生效
     */
    private List<DecryptPluginConfiguration> plugins;

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) {
        this.props = props;
    }

    public List<DecryptPluginConfiguration> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<DecryptPluginConfiguration> plugins) {
        this.plugins = plugins;
    }
}