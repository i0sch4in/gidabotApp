package com.github.gidabotapp.domain;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.github.gidabotapp.domain.Robot.*;

public enum Floor {
    ZEROTH_FLOOR    (TARTALO, -30,    37.5,  -21.6,   9.3),
    FIRST_FLOOR     (KBOT,    -60.82, 2.38,  -1.82,  28.42),
    SECOND_FLOOR    (GALTXA,  -14.61, 47.0,  -41.91,-17.34),
    THIRD_FLOOR     (MARI,    -8.6,   54.25, -14.6,  14.27)
    ;

    private final Robot robot;
    private final double[] x_bounds;
    private final double[] y_bounds;

    private static final List<String> CODES;
    static {
        CODES = new ArrayList<>();
        for(Floor f: Floor.values()){
            CODES.add(String.valueOf(f.ordinal()));
        }
    }

    Floor(Robot robot, double x_min, double x_max, double y_min, double y_max)
    {
        this.robot = robot;
        this.x_bounds = new double[]{x_min,x_max};
        this.y_bounds = new double[]{y_min,y_max};
    }

    public int getFloorCode(){
        return this.ordinal();
    }
    public String getRobotNameShort(){
        return this.robot.getShortName();
    }
    public String getRobotNameLong(){
        return this.robot.longName;
    }

    public int getRobotIconRes(){
        return this.robot.iconResId;
    }

    public double[] getXBounds(){return this.x_bounds;}

    public double[] getYBounds(){return this.y_bounds;}

    public LatLng getStartLatLng(){
        return new MapPosition(robot.start_x, robot.start_y, 0).toLatLng(this);
    }

    public static List<String> getFloorCodeList(){
        return Collections.unmodifiableList(CODES);
    }

    public static Floor getStartingFloor(){
        return ZEROTH_FLOOR;
    }

    public static Floor getFromDouble(double d){
        return values()[(int)d];
    }
}