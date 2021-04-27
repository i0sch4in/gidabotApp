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
        final int[] LAT_RANGE = new int[]{-65 , +65};
        final int[] LNG_RANGE = new int[]{-180, +180};

        double lng = rangeConversion(-30, 37.5, LNG_RANGE[0], LNG_RANGE[1], x);
        double lat = rangeConversion(-21.6, 9.3, LAT_RANGE[0], LAT_RANGE[1], y);

        return new LatLng(lat,lng);
    }

    public double rangeConversion(double first_old, double last_old, double first_new, double last_new, double old_value) {
        double old_range = (last_old - first_old);
        double new_range = (last_new - first_new);
        return (((old_value - first_old) * new_range) / old_range) + first_new;
    }

}
