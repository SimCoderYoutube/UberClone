package com.simcoder.uber.historyRecyclerView;

/**
 * Created by manel on 10/10/2017.
 */

public class HistoryObject {
    private String rideId;

    public HistoryObject(String rideId){
        this.rideId = rideId;
    }

    public String getRideId(){return rideId;}
    public void setRideId(String rideId) {
        this.rideId = rideId;
    }
}
