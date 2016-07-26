package com.pgrenaud.android.p2p.entity;

import android.location.Location;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;

public class LocationEntity {

    private static final String TO_STRING_FORMAT = "[%f, %f]";

    @SerializedName("lat")
    private Double latitude;
    @SerializedName("lng")
    private Double longitude;

    public LocationEntity() {
        this(0.0, 0.0);
    }

    public LocationEntity(Location location) {
        this(location.getLatitude(), location.getLongitude());
    }

    public LocationEntity(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public float distanceTo(LocationEntity dest) {
        return distanceBetween(latitude, longitude, dest.getLatitude(), dest.getLongitude());
    }

    public float distanceTo(Location dest) {
        return distanceBetween(latitude, longitude, dest.getLatitude(), dest.getLongitude());
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setLocation(LocationEntity locationEntity) {
        latitude = locationEntity.getLatitude();
        longitude = locationEntity.getLongitude();
    }

    public boolean hasLocation() {
        return latitude != 0.0 || longitude != 0.0;
    }

    public static float distanceBetween(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        float[] results = new float[1];

        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);

        return results[0];
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), TO_STRING_FORMAT, latitude, longitude);
    }
}
