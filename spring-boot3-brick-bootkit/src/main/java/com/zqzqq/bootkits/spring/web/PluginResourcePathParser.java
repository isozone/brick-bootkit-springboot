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

package com.zqzqq.bootkits.spring.web;

import com.zqzqq.bootkits.utils.UrlUtils;

/**
 * 插件静态资源请求路径解析器 (纯函数, 可单测)。
 *
 * <p>入参 requestPath 是 Spring 解析后的路径, 已去掉 context-path,
 * 格式形如 "plugins/plugin1/index.html"。本类负责剥离 pathPrefix 前缀,
 * 并拆分成 pluginId / partialPath。</p>
 *
 * @author starBlues
 * @since 3.1.0
 */
public final class PluginResourcePathParser {

    private final String pathPrefix;
    private final String indexPageName;

    public PluginResourcePathParser(PluginStaticResourceConfig config) {
        this.pathPrefix = config.getPathPrefix();
        this.indexPageName = config.getIndexPageName();
    }

    /**
     * 解析请求路径
     * @param requestPath Spring 传入的已去掉 context-path 的请求路径
     * @return 解析结果, 永不为 null
     */
    public ParseResult parse(String requestPath) {
        // 归一化: 去掉连续分隔符与首尾分隔符
        requestPath = UrlUtils.format(requestPath);
        // 剥离 pathPrefix 前缀 (例如 "plugins")
        if (pathPrefix != null && !pathPrefix.isEmpty()) {
            int prefixIndex = requestPath.indexOf(pathPrefix);
            if (prefixIndex == 0) {
                requestPath = requestPath.substring(pathPrefix.length());
            }
        }
        requestPath = UrlUtils.format(requestPath);

        int startOffset = requestPath.indexOf("/");
        String pluginId;
        String partialPath;
        if (startOffset == -1) {
            // 仅有 pluginId, 例如 "plugins/plugin1" 去前缀后为 "plugin1"
            pluginId = requestPath;
            partialPath = indexPageName;
        } else {
            pluginId = requestPath.substring(0, startOffset);
            partialPath = requestPath.substring(startOffset + 1);
        }
        return new ParseResult(pluginId, partialPath);
    }

    /**
     * 路径解析结果
     */
    public static final class ParseResult {
        private final String pluginId;
        private final String partialPath;

        public ParseResult(String pluginId, String partialPath) {
            this.pluginId = pluginId;
            this.partialPath = partialPath;
        }

        public String getPluginId() {
            return pluginId;
        }

        public String getPartialPath() {
            return partialPath;
        }
    }
}
