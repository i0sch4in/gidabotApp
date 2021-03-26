package com.github.gidabotapp;

public class Room {
    private final int floor;
    private final int num;
    private final String name;
    private final MapPosition position;

    public Room(int floor, int num, String name, MapPosition position){
        this.floor = floor;
        this.num = num;
        this.name = name;
        this.position = position;
    }

    public int getFloor() {
        return floor;
    }

    public int getNum() {
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
}
