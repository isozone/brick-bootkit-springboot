package com.zqzqq.bootkits.core;

/**
 * Plugin lifecycle state contract.
 */
public interface PluginState {

    String name();

    boolean canTransitionTo(PluginState targetState);

    String getDescription();
}
