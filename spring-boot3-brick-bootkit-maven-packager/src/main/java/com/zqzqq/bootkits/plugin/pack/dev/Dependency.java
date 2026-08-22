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



package com.zqzqq.bootkits.plugin.pack.dev;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 开发环境下配置本地依赖的 Bean.
 *
 * @author jove
 * @since 3.0.0
 * @version 4.0.5
 */
@Data
public class Dependency {

    @Parameter(required = true)
    private String groupId;

    @Parameter(required = true)
    private String artifactId;

    @Parameter(required = true)
    private String classesPath;
}
