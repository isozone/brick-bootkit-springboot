package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.protocol.ControlMessageEnvelope;

public interface OpenClawControlMessageListener {

    default void onConnected() {
    }

    default void onMessage(ControlMessageEnvelope envelope) {
    }

    default void onDisconnected() {
    }

    default void onError(Throwable throwable) {
    }
}
