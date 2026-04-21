package com.zqzqq.bootkits.openclaw.control.store;

import com.zqzqq.bootkits.openclaw.control.spi.ClientStateStore;
import com.zqzqq.bootkits.openclaw.protocol.ClientSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryClientStateStore implements ClientStateStore {

    private final ConcurrentMap<String, ClientSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public ClientSnapshot findByClientId(String clientId) {
        ClientSnapshot snapshot = store.get(clientId);
        return snapshot == null ? null : new ClientSnapshot(snapshot);
    }

    @Override
    public void save(ClientSnapshot snapshot) {
        if (snapshot == null || snapshot.getClientId() == null || snapshot.getClientId().isBlank()) {
            return;
        }
        store.put(snapshot.getClientId(), new ClientSnapshot(snapshot));
    }

    @Override
    public List<ClientSnapshot> findAll() {
        List<ClientSnapshot> result = new ArrayList<>();
        for (ClientSnapshot snapshot : store.values()) {
            result.add(new ClientSnapshot(snapshot));
        }
        return result;
    }
}
