package com.zqzqq.bootkits.openclaw.gateway.protocol;

import java.util.ArrayList;
import java.util.List;

public class GatewayFeatureSet {

    private List<String> methods = new ArrayList<>();
    private List<String> events = new ArrayList<>();

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods == null ? new ArrayList<>() : new ArrayList<>(methods);
    }

    public List<String> getEvents() {
        return events;
    }

    public void setEvents(List<String> events) {
        this.events = events == null ? new ArrayList<>() : new ArrayList<>(events);
    }
}
