package com.github.gidabotapp.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Floor {
    ZEROTH (0),
    MEZZANINE (0.5),
    FIRST (1),
    SECOND (2),
    THIRD (3)
    ;

    private final double floorCode;
    private static final List<String> CODES;
    static {
        CODES = new ArrayList<>();
        for(Floor f: Floor.values()){
            CODES.add(String.valueOf(f.floorCode));
        }
    }

    Floor(double floorCode){
        this.floorCode = floorCode;
    }

    public double getFloorCode(){
        return this.floorCode;
    }

    public static List<String> getFloorList(){
        return Collections.unmodifiableList(CODES);
    }
}
