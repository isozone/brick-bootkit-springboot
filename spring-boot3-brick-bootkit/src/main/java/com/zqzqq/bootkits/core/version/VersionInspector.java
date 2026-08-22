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



package com.zqzqq.bootkits.core.version;

import com.zqzqq.bootkits.core.exception.PluginException;

/**
 * 版本检查器
 *
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.1
 */
public interface VersionInspector {

    /**
     * 检查插件版本号是否合法
     * @param version 版本号
     * @throws PluginException 版本号不合法则抛出异常
     */
    void check(String version) throws PluginException;

    /**
     * 比较 v1 和 v2版本.
     * @param v1 版本号1
     * @param v2 版本号2
     * @return 如果 v1大于等于v2, 则返回大于等于0的数值。否则返回小于0的数值
     * @throws PluginException 版本号不合法则抛出异常
     */
    int compareTo(String v1, String v2) throws PluginException;;

}

