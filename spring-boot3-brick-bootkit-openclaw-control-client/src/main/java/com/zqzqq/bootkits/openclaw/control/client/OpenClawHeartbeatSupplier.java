package com.zqzqq.bootkits.openclaw.control.client;

import com.zqzqq.bootkits.openclaw.protocol.ClientHeartbeatRequest;

@FunctionalInterface
public interface OpenClawHeartbeatSupplier {

    ClientHeartbeatRequest get();
}
