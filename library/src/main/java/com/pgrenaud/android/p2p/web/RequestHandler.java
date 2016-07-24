package com.pgrenaud.android.p2p.web;

import static com.pgrenaud.android.p2p.web.RoutableWebServer.sendError;
import static com.pgrenaud.android.p2p.web.RoutableWebServer.sendEvent;
import static com.pgrenaud.android.p2p.web.RoutableWebServer.sendJSON;
import static com.pgrenaud.android.p2p.web.RoutableWebServer.sendServerError;
import static com.pgrenaud.android.p2p.web.RoutableWebServer.sendStream;
import static com.pgrenaud.android.p2p.web.RoutableWebServer.sendTimeout;

import android.util.Log;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.pgrenaud.android.p2p.entity.EventEntity;
import com.pgrenaud.android.p2p.entity.FileEntity;
import com.pgrenaud.android.p2p.entity.PeerEntity;
import com.pgrenaud.android.p2p.peering.PeerHive;
import com.pgrenaud.android.p2p.repository.FileRepository;
import com.pgrenaud.android.p2p.repository.PeerRepository;
import com.pgrenaud.android.p2p.repository.QueueRepository;

import fi.iki.elonen.NanoHTTPD.Response;

public class RequestHandler {

    private static final long REQUEST_TIMEOUT = 60;

    private final QueueRepository queueRepository;
    private final FileRepository fileRepository;
    private final PeerRepository peerRepository;
    private final PeerHive peerHive;

    public RequestHandler(QueueRepository queueRepository, FileRepository fileRepository, PeerRepository peerRepository, PeerHive peerHive) {
        this.queueRepository = queueRepository;
        this.fileRepository = fileRepository;
        this.peerRepository = peerRepository;
        this.peerHive = peerHive;
    }

    public Response handlePolling(UUID uuid) {
        PeerEntity peer = peerRepository.get(uuid);

        if (peer != null) {
            // Try to start worker if not already running
            peerHive.spawnWorker(peer);
        }

        BlockingQueue<EventEntity> queue = queueRepository.getOrCreate(uuid);

        try {
            EventEntity event = queue.poll(REQUEST_TIMEOUT, TimeUnit.SECONDS);

            if (event != null) {
                Log.d("RequestHandler", "Handling event " + event.getEvent());

                return sendEvent(event);
            } else {
                return sendTimeout();
            }
        } catch (InterruptedException e) {
            return sendServerError("SERVER INTERNAL ERROR: InterruptedException: " + e.getMessage());
        }
    }

    public Response handleFileList() {
        return sendJSON(fileRepository.encode());
    }

    public Response handleFileRequest(UUID uuid) {
        FileEntity file = fileRepository.get(uuid);

        if (file != null) {
            return sendStream(file.getFile());
        } else {
            return sendError("Could not find file with UUID '" + uuid + "'.");
        }
    }
}
