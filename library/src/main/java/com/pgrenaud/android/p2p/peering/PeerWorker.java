package com.pgrenaud.android.p2p.peering;

import android.util.Log;

import com.google.gson.JsonSyntaxException;

import java.io.IOException;

import com.pgrenaud.android.p2p.entity.EventEntity;
import com.pgrenaud.android.p2p.entity.PeerEntity;
import com.pgrenaud.android.p2p.helper.ApiEndpoints;
import com.pgrenaud.android.p2p.helper.HttpClientWrapper;
import com.pgrenaud.android.p2p.helper.HttpClientWrapper.HttpResponseCallback;

import cz.msebera.android.httpclient.conn.HttpHostConnectException;

public class PeerWorker implements Runnable {

    private final PeerHive hive;
    private final PeerEntity peer;
    private final HttpClientWrapper client;

    private final String pingUrl;
    private final String pollingUrl;

    private volatile boolean running;
    private boolean available = false;

    public PeerWorker(PeerHive hive, PeerEntity peer) {
        this.hive = hive;
        this.peer = peer;

        client = new HttpClientWrapper();

        pingUrl = ApiEndpoints.getPingUri(peer);
        pollingUrl = ApiEndpoints.getPollingUri(peer, hive.getService().getSelfPeerEntity());

        running = false;
        available = false;
    }

    @Override
    public void run() {
        running = true;

        Log.d("PeerWorker", "Starting worker " + peer);

        try {
            client.performHttpGet(pingUrl, new HttpResponseCallback() {
                @Override
                public void onHttpResponse(int status, String content) {
                    if (status == 200) {
                        Log.d("PeerWorker", "Peer " + peer  + " is online");

                        available = true;
                        peer.setOnline(true);
                        notifyConnectionListener();
                    } else {
                        Log.e("PeerWorker", "ping: unknown status: " + status);
                    }
                }
                @Override
                public void onException(Exception exception) {
                    if (exception instanceof HttpHostConnectException) {
                        Log.d("PeerWorker", exception.getMessage());
                    } else {
                        Log.e("PeerWorker", "ping: onException", exception);
                    }
                }
            });

            while (running && available) {
                client.performHttpGet(pollingUrl, new HttpResponseCallback() {
                    @Override
                    public void onHttpResponse(int status, String content) {
                        if (status == 408) {
                            // Polling timeout
                        } else if (status == 200) {
                            try {
                                EventEntity event = EventEntity.decode(content);

                                // Handle event
                                if (event.getEvent() == EventEntity.Type.DISPLAY_NAME_UPDATE) {
                                    peer.setDisplayName(event.getParams().getDisplayName());
                                    notifyDisplayNameListener();
                                } else if (event.getEvent() == EventEntity.Type.LOCATION_UPDATE) {
                                    peer.getLocation().setLocation(event.getParams().getLocation());
                                    notifyLocationListener();
                                } else if (event.getEvent() == EventEntity.Type.DIRECTORY_CHANGE) {
                                    notifyDirectoryListener();
                                } else {
                                    Log.e("PeerWorker", "polling: unknown event type");

                                    available = false;
                                }
                            } catch (JsonSyntaxException e) {
                                Log.e("PeerWorker", "polling: unknown event", e);

                                available = false;
                            }
                        } else {
                            Log.e("PeerWorker", "polling: unknown status: " + status);

                            available = false;
                        }
                    }
                    @Override
                    public void onException(Exception exception) {
                        if (exception instanceof HttpHostConnectException) {
                            Log.d("PeerWorker", exception.getMessage());
                        } else {
                            Log.e("PeerWorker", "polling: onException", exception);
                        }

                        available = false;
                    }
                });
            }

            Log.d("PeerWorker", "Peer " + peer  + " is offline");

            available = false;
            peer.setOnline(false);
            notifyConnectionListener();
        } finally {
            stop();
        }
    }

    public void stop() {
        if (!running) {
            return; // Already stopped, nothing to do.
        }

        Log.d("PeerWorker", "Stopping worker " + peer);

        running = false;

        try {
            client.close();
        } catch (IOException e) {
            Log.e("PeerWorker", "Exception occurred while closing http client", e);
        }
    }

    public PeerEntity getPeerEntity() {
        return peer;
    }

    public boolean isRunning() {
        return running;
    }

    private void notifyConnectionListener() {
        if (hive.getService().getListener() != null) {
            Log.d("PeerWorker", "Invoking listener onPeerConnection()");

            hive.getService().getListener().onPeerConnection(peer);
        }
    }

    private void notifyDisplayNameListener() {
        if (hive.getService().getListener() != null) {
            Log.d("PeerWorker", "Invoking listener onPeerDisplayNameUpdate()");

            hive.getService().getListener().onPeerDisplayNameUpdate(peer);
        }
    }

    private void notifyLocationListener() {
        if (hive.getService().getListener() != null) {
            Log.d("PeerWorker", "Invoking listener onPeerLocationUpdate()");

            hive.getService().getListener().onPeerLocationUpdate(peer);
        }
    }

    private void notifyDirectoryListener() {
        if (hive.getService().getListener() != null) {
            Log.d("PeerWorker", "Invoking listener notifyDirectoryListener()");

            hive.getService().getListener().onPeerDirectoryChange(peer);
        }
    }
}
