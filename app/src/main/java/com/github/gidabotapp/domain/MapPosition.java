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

    public LatLng toLatLng(Floor floor){
        final double[] LAT_BOUNDS = new double[]{-65 , +65};
        final double[] LNG_BOUNDS = new double[]{-180, +180};

        double lng = rangeConversion(floor.getXBounds(), LNG_BOUNDS, x);
        double lat = rangeConversion(floor.getYBounds(), LAT_BOUNDS, y);

        return new LatLng(lat,lng);
    }

    public double rangeConversion(double[] old_bounds, double[] new_bounds, double old_value) {
        double old_range = (old_bounds[1] - old_bounds[0]);
        double new_range = (new_bounds[1] - new_bounds[0]);
        return (((old_value - old_bounds[0]) * new_range) / old_range) + new_bounds[0];
    }

    @Override
    public String toString(){
        return String.format(Locale.getDefault(), "X:%.2f , Y %.2f: , Z:%.2f ", this.x, this.y, this.z);
    }

}
