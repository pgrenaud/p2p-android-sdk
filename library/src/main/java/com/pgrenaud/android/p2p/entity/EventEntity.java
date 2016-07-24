package com.pgrenaud.android.p2p.entity;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

public class EventEntity {

    private final Type event;
    private final Params params;

    public EventEntity(Type event) {
        this.event = event;

        params = new Params();
    }

    public Params getParams() {
        return params;
    }

    public Type getEvent() {
        return event;
    }

    public String encode() {
        Gson gson = new Gson();

        return gson.toJson(this);
    }

    public static EventEntity decode(String json) throws JsonSyntaxException {
        Gson gson = new Gson();

        return gson.fromJson(json, EventEntity.class);
    }

    public enum Type {
        @SerializedName("name")
        DISPLAY_NAME_UPDATE,
        @SerializedName("loc")
        LOCATION_UPDATE,
        ;
    }

    public class Params {
        @SerializedName("name")
        private String displayName;
        @SerializedName("loc")
        private LocationEntity location;

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public LocationEntity getLocation() {
            return location;
        }

        public void setLocation(LocationEntity location) {
            this.location = location;
        }
    }
}
