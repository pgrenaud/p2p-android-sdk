package com.pgrenaud.android.p2p.helper;

import com.pgrenaud.android.p2p.entity.FileEntity;
import com.pgrenaud.android.p2p.entity.PeerEntity;

import cz.msebera.android.httpclient.client.utils.URIBuilder;

public class ApiEndpoints {

    public static String getPingUri(PeerEntity peerEntity) {
        return new URIBuilder()
                .setScheme("http")
                .setHost(peerEntity.getIpAddress())
                .setPort(peerEntity.getPort())
                .setPath("/api/v1/ping")
                .toString();
    }

    public static String getPollingUri(PeerEntity peerEntity, PeerEntity selfPeerEntity) {
        return new URIBuilder()
                .setScheme("http")
                .setHost(peerEntity.getIpAddress())
                .setPort(peerEntity.getPort())
                .setPath("/api/v1/polling/" + selfPeerEntity.getUUID())
                .toString();
    }

    public static String getFileListUri(PeerEntity peerEntity) {
        return new URIBuilder()
                .setScheme("http")
                .setHost(peerEntity.getIpAddress())
                .setPort(peerEntity.getPort())
                .setPath("/api/v1/files")
                .toString();
    }

    public static String getFileListUri(String host) {
        return new URIBuilder()
                .setScheme("http")
                .setHost(host)
                .setPath("/api/v1/files")
                .toString();
    }

    public static String getFileDownloadUri(PeerEntity peerEntity, FileEntity fileEntity) {
        return new URIBuilder()
                .setScheme("http")
                .setHost(peerEntity.getIpAddress())
                .setPort(peerEntity.getPort())
                .setPath("/api/v1/file/" + fileEntity.getUuid())
                .toString();
    }

    public static String getFileDownloadUri(String host, String uuid) {
        return new URIBuilder()
                .setScheme("http")
                .setHost(host)
                .setPath("/api/v1/file/" + uuid)
                .toString();
    }
}
