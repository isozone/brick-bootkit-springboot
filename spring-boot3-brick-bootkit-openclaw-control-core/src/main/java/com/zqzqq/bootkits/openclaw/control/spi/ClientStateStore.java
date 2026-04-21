package com.zqzqq.bootkits.openclaw.control.spi;

import com.zqzqq.bootkits.openclaw.protocol.ClientSnapshot;

import java.util.List;

public interface ClientStateStore {

    ClientSnapshot findByClientId(String clientId);

    void save(ClientSnapshot snapshot);

    List<ClientSnapshot> findAll();
}
