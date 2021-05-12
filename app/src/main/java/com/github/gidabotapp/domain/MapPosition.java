package com.github.gidabotapp.domain;


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

    // Returns square distance to given other Position
    // Real distance is not meaningful, hence root is not computed
    public double dSquare(MapPosition otherPos){
        double dx = otherPos.x - this.x;
        double dy = otherPos.y - this.y;
        return dx * dx + dy * dy;
    }

    // Converts MapPoint to Latitude and Longitude
    // Conversion depends on floor, so floor is given as an argument
    public LatLng toLatLng(Floor floor){
        // Map's Latitude and Longitude Bounds
        final double[] LAT_BOUNDS = new double[]{-65 , +65};
        final double[] LNG_BOUNDS = new double[]{-180, +180};

        // Convert X coordinate to Longitude
        double lng = rangeConversion(floor.getXBounds(), LNG_BOUNDS, x);
        // Convert Y coordinate to Latitude
        double lat = rangeConversion(floor.getYBounds(), LAT_BOUNDS, y);

        return new LatLng(lat,lng);
    }


    // Converts old value from old_bounds range to new_bounds range
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
