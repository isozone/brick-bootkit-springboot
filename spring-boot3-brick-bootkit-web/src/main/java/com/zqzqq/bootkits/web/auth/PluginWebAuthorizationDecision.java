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

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Authorization decision for a single request.
 */
@Data
@AllArgsConstructor
public class PluginWebAuthorizationDecision {
    private boolean allowed;
    private String reason;

    public static PluginWebAuthorizationDecision allow() {
        return new PluginWebAuthorizationDecision(true, "allowed");
    }

    public static PluginWebAuthorizationDecision deny(String reason) {
        return new PluginWebAuthorizationDecision(false, reason == null ? "permission denied" : reason);
    }
}

