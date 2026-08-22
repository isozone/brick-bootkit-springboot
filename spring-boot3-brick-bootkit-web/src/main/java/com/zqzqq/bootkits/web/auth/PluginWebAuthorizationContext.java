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


package com.zqzqq.bootkits.web.auth;

import lombok.Builder;
import lombok.Data;

/**
 * Authorization context passed to host application authorizer.
 */
@Data
@Builder
public class PluginWebAuthorizationContext {
    private PluginWebPermission permission;
    private String pluginId;
    private String principal;
    private String requestUri;
    private String httpMethod;
    private String remoteAddress;
}

