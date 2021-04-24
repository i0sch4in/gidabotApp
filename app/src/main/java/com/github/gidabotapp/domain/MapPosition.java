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

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double[] getPositions(){
        return new double[] {this.x,this.y,this.z};
    }

    // Beste puntu batekiko distantzia euklidearra (karratua) kalkulatu
    // ez da erro erabiltzen (horregatik izena du distance square)
    public double dSquare(MapPosition other){
        double dx = other.x - this.x;
        double dy = other.y - this.y;
        return dx * dx + dy * dy;
    }

    public String toString(){
        return String.format(Locale.getDefault(), "X:%.2f , Y %.2f: , Z:%.2f ", this.x, this.y, this.z);
    }

    public LatLng toLatLng(){
//        double FACTOR_X = 5.333;
//        double FACTOR_Y = 3.3;
//
//        double lng = FACTOR_X * (x+30) - 180;
//        double lat = FACTOR_Y * (y+22.6) - 65;

        double lng = rangeConversion(-30, 37.5, -180, 180, x);
        double lat = rangeConversion(-21.6, 9.3, -65, 65, y);

        return new LatLng(lat,lng);
    }

    public double rangeConversion(double first_old, double last_old, double first_new, double last_new, double old_value) {
        double old_range = (last_old - first_old);
        double new_range = (last_new - first_new);
        return (((old_value - first_old) * new_range) / old_range) + first_new;
    }

}
