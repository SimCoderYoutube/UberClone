package com.simcoder.uber.Objects;

import com.google.android.gms.maps.model.LatLng;

/**
 * Location Object used to know pickup and destination location
 */
public class LocationObject {

    private LatLng coordinates;
    private String name = "";

    /**
     * LocationObject constructor
     * @param coordinates - latLng of the location
     * @param name - name of the location
     */
    public LocationObject(LatLng coordinates, String name){
        this.coordinates = coordinates;
        this.name = name;
    }

    /**
     * LocationObject constructor
     * Creates an empty object
     */
    public LocationObject(){
    }


    public LatLng getCoordinates() {
        return coordinates;
    }
    public void setCoordinates(LatLng coordinates) {
        this.coordinates = coordinates;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
