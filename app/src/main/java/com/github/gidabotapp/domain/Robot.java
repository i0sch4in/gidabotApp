package com.github.gidabotapp.domain;

import com.google.android.gms.maps.model.Marker;

public class Robot {
    private final String name;
    public final int iconResId;
    private Marker marker;
    private final Floor floor;
    private MapPosition position;

    public Robot(Floor floor, String name, int iconResId){
        this.name = name;
        this.iconResId = iconResId;
        this.floor = floor;
    }

    public String getName() {
        return name;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public void updateMarker(MapPosition position){
        this.marker.setPosition(position.toLatLng(floor));
        this.position = position;
    }

    public MapPosition getPosition(){
        return this.position;
    }

    public void show(){
        this.marker.setVisible(true);
    }

    public void hide(){
        this.marker.setVisible(false);
    }
}
