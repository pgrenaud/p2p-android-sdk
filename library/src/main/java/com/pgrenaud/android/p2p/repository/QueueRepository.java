package com.pgrenaud.android.p2p.repository;

import android.util.Log;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.pgrenaud.android.p2p.entity.EventEntity;

public class QueueRepository {

    private final Map<UUID, BlockingQueue<EventEntity>> queues;

    public QueueRepository() {
        queues = new ConcurrentHashMap<>();
    }

    public BlockingQueue<EventEntity> get(UUID uuid) {
        return queues.get(uuid);
    }

    public BlockingQueue<EventEntity> getOrCreate(UUID uuid) {
        BlockingQueue<EventEntity> queue = get(uuid);

        if (queue == null) {
            queue = new LinkedBlockingQueue<>();

            add(uuid, queue);
        }

        return queue;
    }

    public Collection<BlockingQueue<EventEntity>> getAll() {
        return queues.values();
    }

    public void add(UUID uuid, BlockingQueue<EventEntity> queue) {
        queues.put(uuid, queue);
    }

    public void putAll(EventEntity event) {
        for (BlockingQueue<EventEntity> queue : getAll()) {
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                Log.e("QueueRepository", "Exception occurred while appending event to queue", e);
            }
        }
    }

    public void remove(UUID uuid) {
        queues.remove(uuid);
    }

    public void removeAll() {
        queues.clear();
    }
}
