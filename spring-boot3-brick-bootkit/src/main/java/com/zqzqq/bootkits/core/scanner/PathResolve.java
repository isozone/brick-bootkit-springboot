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



package com.zqzqq.bootkits.core.scanner;

import java.nio.file.Path;

/**
 * 从路径中解析适合的插件
 * @author starBlues
 * @since 3.0.0
 * @version 3.0.0
 */
public interface PathResolve {

    /**
     * 解析并返回合适的路径
     * @param path 待过滤路径
     * @return  处理后的路径, 返回null 表示不可用
     */
    Path resolve(Path path);


}

