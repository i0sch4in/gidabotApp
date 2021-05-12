package com.github.gidabotapp.domain;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.github.gidabotapp.repository.FloorConverters;

@Entity (tableName = "rooms")
public class Room {
    @PrimaryKey @NonNull
    private final String num;

    @TypeConverters(FloorConverters.class)
    private final Floor floor;
    private final String name;

    @Embedded
    private final MapPosition position;

    public Room(Floor floor, String num, String name, MapPosition position){
        this.floor = floor;
        this.num = num;
        this.name = name;
        this.position = position;
    }

    public Floor getFloor() {
        return floor;
    }

    public float getFloorCode(){
        return floor.ordinal();
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

    public boolean equals(Room room2){
        return this.num.compareTo(room2.num) == 0;
    }

    @NonNull
    @Override
    public String toString(){
        return num + " - " + name;
    }
}
