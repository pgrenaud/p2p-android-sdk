package com.pgrenaud.android.p2p.peering;

import android.util.Log;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import com.pgrenaud.android.p2p.entity.PeerEntity;
import com.pgrenaud.android.p2p.repository.PeerRepository;
import com.pgrenaud.android.p2p.service.PeerService;

public class PeerHive {

    private static final int POOL_SIZE = 32;

    private final ExecutorService pool;
    private final ConcurrentHashMap<UUID, PeerWorker> workers;
    private final PeerService service;
    private final PeerRepository peers;

    public PeerHive(PeerService service, PeerRepository peers) {
        this.service = service;
        this.peers = peers;

        pool = Executors.newFixedThreadPool(POOL_SIZE);
        workers = new ConcurrentHashMap<>();
    }

    /**
     * Start worker for specified PeerEntity if not already started.
     *
     * @param peerEntity PeerEntity associated with the worker.
     */
    public void spawnWorker(PeerEntity peerEntity) {
        PeerWorker worker = workers.get(peerEntity.getUUID());

        if (worker == null || !worker.isRunning()) {
            try {
                worker = new PeerWorker(this, peerEntity);

                workers.put(peerEntity.getUUID(), worker);
                pool.submit(worker);

                Log.d("PeerHive", "Spawned worker for peer " + peerEntity);
            } catch (RejectedExecutionException e) {
                Log.e("PeerHive", "Exception occurred while submitting new peer worker to thread pool", e);
            }
        }
    }

    /**
     * Stop worker for specified PeerEntity if currently running.
     *
     * @param peerEntity PeerEntity associated with the worker.
     */
    public void killWorker(PeerEntity peerEntity) {
        PeerWorker worker = workers.get(peerEntity.getUUID());

        if (worker != null) {
            worker.stop();
            workers.remove(worker.getPeerEntity().getUUID());

            Log.d("PeerHive", "Killed worker for peer " + peerEntity);
        }
    }

    /**
     * Synchronizing peer hive by starting missing workers and stopping unneeded ones.
     */
    public void sync() {
        // Stopping workers whom peer has been removed
        for (PeerWorker worker : workers.values()) {
            PeerEntity peer = peers.get(worker.getPeerEntity().getUUID());

            if (peer == null) {
                worker.stop();
                workers.remove(worker.getPeerEntity().getUUID());

                Log.d("PeerHive", "Killed worker for peer " + worker.getPeerEntity());
            }
        }

        // Starting workers whom peer has been added
        for (PeerEntity peer : peers.getAll()) {
            spawnWorker(peer);
        }
    }

    /**
     * Internal API
     */
    public void stop() {
        Log.d("PeerHive", "Stopping PeerHive");

        for (PeerWorker worker : workers.values()) {
            worker.stop();
        }

        pool.shutdown();

        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e("PeerHive", "Exception occurred while waiting thread pool to terminate", e);
        }

        Log.d("PeerHive", "PeerHive stopped");
    }

    public PeerService getService() {
        return service;
    }
}
