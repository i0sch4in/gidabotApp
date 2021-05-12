package com.github.gidabotapp.repository;

import androidx.room.TypeConverter;

import com.github.gidabotapp.domain.Floor;

public class FloorConverters {
    @TypeConverter
    public static Floor toFloor(int value){
        return Floor.values()[value];
    }

    @TypeConverter
    public static int fromFloor(Floor floor){
        return floor.ordinal();
    }
}
