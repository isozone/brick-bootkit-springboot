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


package com.zqzqq.bootkits.core.lock;

import java.time.Duration;

/**
 * No-op cluster lock provider for single instance mode.
 */
public class NoOpClusterLockProvider implements ClusterLockProvider {

    private static final ClusterLock NO_OP_LOCK = () -> {
    };

    @Override
    public ClusterLock acquire(String key, Duration timeout) {
        return NO_OP_LOCK;
    }
}
