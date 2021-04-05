package com.github.gidabotapp;


import java.util.Locale;

import geometry_msgs.Point;
import geometry_msgs.Pose;
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
}
