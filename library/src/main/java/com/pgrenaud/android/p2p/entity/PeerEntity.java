package com.pgrenaud.android.p2p.entity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.annotations.Until;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class PeerEntity implements Comparable<PeerEntity> {

    private static final String NAME_UUID_FORMAT = "%s:%s";
    private static final String ADDRESS_FORMAT = "%s:%s";
    private static final String ACCESSED_AT_FORMAT = "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS";
    private static final String TO_STRING_FORMAT = "%S (%s)";

    // Do not serialize location or online
    private transient LocationEntity location;
    private transient boolean online;

    private UUID uuid;
    @SerializedName("name")
    private String displayName;
    @SerializedName("ip")
    private String ipAddress;
    private Integer port;
    @Until(1.3)
    @SerializedName("accessed_at")
    private Date accessedAt;

    // Ensure that location is properly initialize when Gson is decoding
    private PeerEntity() {
        this(null, null, null);
    }

    public PeerEntity(String displayName, String ipAddress, Integer port) {
        this.displayName = displayName;
        this.ipAddress = ipAddress;
        this.port = port;

        uuid = UUID.nameUUIDFromBytes(generateNameForUUID());
        location = new LocationEntity();
        online = false;
    }

    private byte[] generateNameForUUID() {
        return String.format(Locale.getDefault(), NAME_UUID_FORMAT, ipAddress, port).getBytes();
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public String getHost() {
        return String.format(Locale.getDefault(), ADDRESS_FORMAT, ipAddress, port);
    }

    public LocationEntity getLocation() {
        return location;
    }

    @Nullable
    public Date getAccessedAt() {
        return accessedAt;
    }

    @Nullable
    public String getFormatedAccessedAt() {
        if (accessedAt != null) {
            return String.format(Locale.getDefault(), ACCESSED_AT_FORMAT, accessedAt);
        } else {
            return null;
        }
    }

    public void updateAccessedAt() {
        this.accessedAt = new Date();
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), TO_STRING_FORMAT, uuid, displayName);
    }

    @Override
    public int compareTo(@NonNull PeerEntity peerEntity) {
        return getDisplayName().compareTo(peerEntity.getDisplayName());
    }

    public String persistEncode() {
        Gson gson = new Gson();

        return gson.toJson(this);
    }

    public String encode() {
        Gson gson = new GsonBuilder().setVersion(1.4).create();

        return gson.toJson(this);
    }

    public static PeerEntity decode(String json) throws JsonSyntaxException {
        Gson gson = new Gson();

        return gson.fromJson(json, PeerEntity.class);
    }
}
