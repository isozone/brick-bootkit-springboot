package com.zqzqq.bootkits.openclaw.control.client.autoconfigure;

import com.zqzqq.bootkits.openclaw.control.client.OpenClawControlMessageListener;
import com.zqzqq.bootkits.openclaw.protocol.ControlMessageEnvelope;

import java.util.List;

public class CompositeOpenClawControlMessageListener implements OpenClawControlMessageListener {

    private final List<OpenClawControlMessageListener> listeners;

    public CompositeOpenClawControlMessageListener(List<OpenClawControlMessageListener> listeners) {
        this.listeners = listeners == null ? List.of() : List.copyOf(listeners);
    }

    @Override
    public void onConnected() {
        listeners.forEach(OpenClawControlMessageListener::onConnected);
    }

    @Override
    public void onMessage(ControlMessageEnvelope envelope) {
        listeners.forEach(listener -> listener.onMessage(envelope));
    }

    @Override
    public void onDisconnected() {
        listeners.forEach(OpenClawControlMessageListener::onDisconnected);
    }

    @Override
    public void onError(Throwable throwable) {
        listeners.forEach(listener -> listener.onError(throwable));
    }
}
