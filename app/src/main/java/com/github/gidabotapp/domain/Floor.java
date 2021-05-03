package com.github.gidabotapp.domain;

import com.github.gidabotapp.R;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Floor {
    // StartPoint => 000 - Fakultateko sarrera nagusia
    ZEROTH_FLOOR(0,"tartalo", R.drawable.tartalo_small, 3.5503,  -18.4937,-30,  37.5, -21.6,  9.3),

    // StartPoint => 122 - Dekanotza
    FIRST_FLOOR(1, "kbot", R.drawable.kbot_small,-16.2700, 4.42812,-60.82, 2.38, -1.82, 28.42),

    // StartPoint => Ezkerreko eskailerak (2nd floor)
    SECOND_FLOOR(2,"galtxa", R.drawable.galtxa_small,5.2744, -35.9580,-14.61, 47.0,  -41.91, -17.34),

    // StartPoint => 302 - DCI, Egokituz
    THIRD_FLOOR(3, "mari",R.drawable.mari_small,-0.3069, -8.7494, -8.6,  54.25, -14.6,  14.27)
    ;

    private final int floorCode;
    private final String robotName;
    private final int robotIconRes;
    private final double start_x;
    private final double start_y;
    private final double[] X_BOUNDS;
    private final double[] Y_BOUNDS;

    private static final List<String> CODES;
    static {
        CODES = new ArrayList<>();
        for(Floor f: Floor.values()){
            CODES.add(String.valueOf(f.floorCode));
        }
    }

    Floor( int floorCode, String robotName, int robotIconRes, double start_x, double start_y, double x_min, double x_max, double y_min, double y_max)
    {
        this.floorCode = floorCode;
        this.robotName = robotName;
        this.robotIconRes = robotIconRes;
        this.start_x = start_x;
        this.start_y = start_y;
        this.X_BOUNDS = new double[]{x_min,x_max};
        this.Y_BOUNDS = new double[]{y_min,y_max};
    }

    public int getFloorCode(){
        return this.floorCode;
    }
    public String getRobotName(){
        return this.robotName;
    }

    public int getRobotIconRes(){
        return this.robotIconRes;
    }

    public double[] getXBounds(){return this.X_BOUNDS;}

    public double[] getYBounds(){return this.Y_BOUNDS;}

    public MapPosition getStartPosition(){
        return new MapPosition(start_x, start_y,0);
    }

    public LatLng getStartLatLng(){
        return new MapPosition(start_x, start_y, 0).toLatLng(this);
    }

    public static List<String> getFloorList(){
        return Collections.unmodifiableList(CODES);
    }

    public static Floor getStartingFloor(){
        return ZEROTH_FLOOR;
    }
}