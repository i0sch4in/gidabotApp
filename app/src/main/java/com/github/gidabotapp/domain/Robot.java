package com.github.gidabotapp.domain;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class Robot {
    private final String name;
    private final int icon;
    private final Floor floor;
    private MapPosition currentPos;

    public Robot(Floor floor, String name, int iconResId){
        this.floor = floor;
        this.name = name;
        this.icon = iconResId;
        this.currentPos = floor.getStartPosition();
    }

    public String getName() {
        return name;
    }

    public Floor getFloor() {
        return floor;
    }

    public MapPosition getCurrentPos() {
        return currentPos;
    }

    public LatLng getLatLng(){
        return currentPos.toLatLng(floor);
    }

    public void setCurrentPos(MapPosition currentPos) {
        this.currentPos = currentPos;
    }

    public int getIcon() {
        return icon;
    }
}
