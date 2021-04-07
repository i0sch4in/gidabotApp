package com.github.gidabotapp;

import androidx.annotation.NonNull;

// TODO: floor -> double egin da, ez dakit arazoak eman ditzakeen
// TODO: num -> String egin da, laborategiek letrak dituztelako
public class Room {
    private final double floor;
    private final String num;
    private final String name;
    private final MapPosition position;

    public Room(double floor, String num, String name, MapPosition position){
        this.floor = floor;
        this.num = num;
        this.name = name;
        this.position = position;
    }

    public double getFloor() {
        return floor;
    }

    public String getNum() {
        return num;
    }

    public String getName() {
        return name;
    }

    public MapPosition getPosition() {
        return position;
    }

    public double getX(){
        return position.getX();
    }

    public double getY(){
        return position.getY();
    }

    public double getZ(){
        return position.getZ();
    }

    @NonNull
    @Override
    public String toString(){
        return num + " - " + name;
    }
}
