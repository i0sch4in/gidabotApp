package com.github.gidabotapp.domain;


import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

import geometry_msgs.Point;
import geometry_msgs.PoseWithCovarianceStamped;

public class MapPosition {
    private double x,y,z;

    public MapPosition(double x, double y, double z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public MapPosition(PoseWithCovarianceStamped poseWithCovariance){
         Point position = poseWithCovariance.getPose().getPose().getPosition();
         this.x = position.getX();
         this.y = position.getY();
         this.y = position.getY();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    // Beste puntu batekiko distantzia euklidearra (karratua) kalkulatu
    // ez da erroa erabiltzen -> distance square
    public double dSquare(MapPosition otherPos){
        double dx = otherPos.x - this.x;
        double dy = otherPos.y - this.y;
        return dx * dx + dy * dy;
    }

    public String toString(){
        return String.format(Locale.getDefault(), "X:%.2f , Y %.2f: , Z:%.2f ", this.x, this.y, this.z);
    }

    public LatLng toLatLng(){
        final double[] LAT_BOUNDS = new double[]{-65 , +65};
        final double[] LNG_BOUNDS = new double[]{-180, +180};

        // Floor = 0 tartalo
        final double[] X_BOUNDS = new double[]{-30, 37.5};
        final double[] Y_BOUNDS = new double[]{-21.6, 9.3};

        // Floor = 1 kbot
//        final double[] X_BOUNDS = new double[]{-60.82, 2.38};
//        final double[] Y_BOUNDS = new double[]{-28.61, 28.42}; // y[0] inaccurate

        // Floor = 2 galtxagorri
//        final double[] X_BOUNDS = new double[]{-14.61, 47.0};
//        final double[] Y_BOUNDS = new double[]{-39.91, -17.34}; // y[0] inaccurate

        // Floor = 3 marisorgin
//        final double[] X_BOUNDS = new double[]{-8.6, 54.25}; // 54.24 fix
//        final double[] Y_BOUNDS = new double[]{-14.6, 14.27};

        double lng = rangeConversion(X_BOUNDS, LNG_BOUNDS, x);
        double lat = rangeConversion(Y_BOUNDS, LAT_BOUNDS, y);

        return new LatLng(lat,lng);
    }

    public double rangeConversion(double[] old_bounds, double[] new_bounds, double old_value) {
        double old_range = (old_bounds[1] - old_bounds[0]);
        double new_range = (new_bounds[1] - new_bounds[0]);
        return (((old_value - old_bounds[0]) * new_range) / old_range) + new_bounds[0];
    }

}
