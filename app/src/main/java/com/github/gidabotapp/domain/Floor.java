package com.github.gidabotapp.domain;

public enum Floor {
    ZEROTH (0),
    MEZZANINE (0.5),
    FIRST (1),
    SECOND (2),
    THIRD (3)
    ;

    private final double floorCode;

    Floor(double floorCode){
        this.floorCode = floorCode;
    }

    public double getFloorCode(){
        return this.floorCode;
    }
}
