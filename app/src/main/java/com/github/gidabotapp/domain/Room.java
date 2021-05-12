package com.github.gidabotapp.domain;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity (tableName = "rooms")
public class Room {
    @PrimaryKey @NonNull
    private final String num;

    private final double floor;
    private final String name;

    @Embedded
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
