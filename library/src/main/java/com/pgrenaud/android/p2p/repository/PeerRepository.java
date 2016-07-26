package com.pgrenaud.android.p2p.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.pgrenaud.android.p2p.entity.PeerEntity;
import com.pgrenaud.android.p2p.service.PeerService;

public class PeerRepository {

    private final Map<UUID, PeerEntity> peers;
    private final PeerService service;

    public PeerRepository(PeerService service) {
        this.service = service;

        peers = new ConcurrentHashMap<>();
    }

    public PeerEntity get(UUID uuid) {
        return peers.get(uuid);
    }

    public Collection<PeerEntity> getAll() {
        return peers.values();
    }

    public void add(PeerEntity peerEntity) {
        if (service.getSelfPeerEntity() != null &&
            service.getSelfPeerEntity().getUUID().equals(peerEntity.getUUID())) {
            // Do not add peerEntity if it is the selfPeerEntity
        } else {
            peers.put(peerEntity.getUUID(), peerEntity);
        }
    }

    /**
     * Add specified PeerEntity to the repository, or update its display name if it already exists.
     *
     * @param peerEntity PeerEntity instance.
     * @return Returns true if the PeerEntity was added to the repository or false if it was updated.
     */
    public boolean addOrUpdate(PeerEntity peerEntity) {
        PeerEntity peer = get(peerEntity.getUUID());

        if (peer == null) {
            add(peerEntity);

            return true;
        } else {
            peer.setDisplayName(peerEntity.getDisplayName());

            return false;
        }
    }

    public void addAll(Collection<PeerEntity> peerEntities) {
        for (PeerEntity peer : peerEntities) {
            add(peer);
        }
    }

    public void mergeAll(Collection<PeerEntity> peerEntities) {
        for (PeerEntity peerEntity : peerEntities) {
            PeerEntity peer = get(peerEntity.getUUID());

            if (peer == null) {
                add(peerEntity); // FIXME: Prevent adding the selfPeerEntity
            }
        }
    }

    public void remove(PeerEntity peerEntity) {
        peers.remove(peerEntity.getUUID());
    }

    public void removeAll() {
        peers.clear();
    }

    public String persistEncode() {
        Gson gson = new Gson();

        return gson.toJson(getAll());
    }

    public String encode() {
        Gson gson = new GsonBuilder().setVersion(1.4).create();

        return gson.toJson(getAll());
    }

    public static Collection<PeerEntity> decode(String json) throws JsonSyntaxException {
        Gson gson = new Gson();
        Type type = new TypeToken<Collection<PeerEntity>>(){}.getType();

        return gson.fromJson(json, type);
    }
}
