package com.github.gidabotapp.domain;

import com.google.android.gms.maps.model.Marker;

public class Robot {
    private final String ICON_URL_FORMAT = "robotIcons/ic_%s";
    private final String name;
    private final double floor;
    private Marker marker;

    public Robot(double floor, String name){
        this.floor = floor;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public double getFloor() {
        return floor;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }
}
