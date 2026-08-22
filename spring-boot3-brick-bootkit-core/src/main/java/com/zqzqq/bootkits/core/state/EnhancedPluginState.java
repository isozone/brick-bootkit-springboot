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


package com.zqzqq.bootkits.core.state;

import com.zqzqq.bootkits.core.PluginState;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Enhanced plugin lifecycle state.
 */
public enum EnhancedPluginState implements PluginState {
    PARSED("Plugin descriptor parsed"),
    LOADED("Plugin loaded"),
    STARTED("Plugin started"),
    STOPPED("Plugin stopped"),
    DISABLED("Plugin disabled"),
    UNLOADED("Plugin unloaded"),
    STOPPED_FAILURE("Plugin stop failed"),
    STARTED_FAILURE("Plugin start failed");

    private Set<EnhancedPluginState> allowedTransitions;
    private final String description;

    static {
        PARSED.allowedTransitions = EnumSet.of(LOADED, DISABLED);
        LOADED.allowedTransitions = EnumSet.of(STARTED, DISABLED, UNLOADED);
        STARTED.allowedTransitions = EnumSet.of(STOPPED, STOPPED_FAILURE, UNLOADED);
        STOPPED.allowedTransitions = EnumSet.of(STARTED, STARTED_FAILURE, UNLOADED);
        DISABLED.allowedTransitions = EnumSet.of(LOADED, UNLOADED);
        UNLOADED.allowedTransitions = EnumSet.noneOf(EnhancedPluginState.class);
        STOPPED_FAILURE.allowedTransitions = EnumSet.of(STOPPED, STARTED, UNLOADED);
        STARTED_FAILURE.allowedTransitions = EnumSet.of(STOPPED, STARTED, UNLOADED);
    }

    EnhancedPluginState(String description) {
        this.description = description;
    }

    public boolean canTransitionTo(EnhancedPluginState newState) {
        Objects.requireNonNull(newState, "newState");
        if (this == newState) {
            throw new IllegalArgumentException("Cannot transition to same state");
        }
        return allowedTransitions.contains(newState);
    }

    @Override
    public boolean canTransitionTo(PluginState targetState) {
        if (targetState instanceof EnhancedPluginState) {
            return canTransitionTo((EnhancedPluginState) targetState);
        }
        return false;
    }

    public Set<EnhancedPluginState> getAllowedTransitions() {
        return EnumSet.copyOf(allowedTransitions);
    }

    @Override
    public String getDescription() {
        return description;
    }
}
