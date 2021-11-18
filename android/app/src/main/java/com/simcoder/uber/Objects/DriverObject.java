package com.simcoder.uber.Objects;

import com.google.firebase.database.DataSnapshot;

import java.text.DecimalFormat;

public class DriverObject {


    private String id = "",
            name = "",
            phone = "",
            car = "",
            profileImage = "default",
            service,
            notificationKey = "",
            license = "";

    private Boolean connect = false;

    private float ratingsAvg = 0, payoutAmount = 0;

    private LocationObject mLocation;

    private Boolean active = true;

    public DriverObject(String id) {
        this.id = id;
    }

    /**
     * DriverObject constructor
     * Creates an empty object
     */
    public DriverObject() {}


    /**
     * Parse datasnapshot into this object
     *
     * @param dataSnapshot - customer info fetched from the database
     */
    public void parseData(DataSnapshot dataSnapshot) {

        id = dataSnapshot.getKey();

        if (dataSnapshot.child("name").getValue() != null) {
            name = dataSnapshot.child("name").getValue().toString();
        }
        if (dataSnapshot.child("phone").getValue() != null) {
            phone = dataSnapshot.child("phone").getValue().toString();
        }
        if (dataSnapshot.child("car").getValue() != null) {
            car = dataSnapshot.child("car").getValue().toString();
        }
        if (dataSnapshot.child("license").getValue() != null) {
            license = dataSnapshot.child("license").getValue().toString();
        }
        if (dataSnapshot.child("profileImageUrl").getValue() != null) {
            profileImage = dataSnapshot.child("profileImageUrl").getValue().toString();
        }
        if (dataSnapshot.child("activated").getValue() != null) {
            active = Boolean.parseBoolean(dataSnapshot.child("activated").getValue().toString());
        }
        if (dataSnapshot.child("connect_set").getValue() != null) {
            connect = Boolean.parseBoolean(dataSnapshot.child("connect_set").getValue().toString());
        }
        if (dataSnapshot.child("payout_amount").getValue() != null) {
            payoutAmount = Float.parseFloat(dataSnapshot.child("payout_amount").getValue().toString());
        }
        if (dataSnapshot.child("service").getValue() != null) {
            service = dataSnapshot.child("service").getValue().toString();
        }

        if (dataSnapshot.child("notificationKey").getValue() != null) {
            notificationKey = dataSnapshot.child("notificationKey").getValue().toString();
        }
        int ratingSum = 0;
        float ratingsTotal = 0;
        for (DataSnapshot child : dataSnapshot.child("rating").getChildren()) {
            ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
            ratingsTotal++;
        }
        if (ratingsTotal != 0) {
            ratingsAvg = ratingSum / ratingsTotal;
        }
    }


    public String getService() {
        return service;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCar() {
        return car;
    }

    public String getLicense() {
        return license;
    }



    public String getServiceDash() {
        if(service.isEmpty()){
            return "--";
        }
        return service;
    }
    public String getNameDash() {
        if(name.isEmpty()){
            return "--";
        }
        return name;
    }
    public String getPhoneDash() {
        if(phone.isEmpty()){
            return "--";
        }
        return phone;
    }
    public String getCarDash() {
        if(car.isEmpty()){
            return "--";
        }
        return car;
    }
    public String getLicenseDash() {
        if(license.isEmpty()){
            return "--";
        }
        return license;
    }



    public String getDriverRatingString(){
        DecimalFormat df = new DecimalFormat("#.#");
        return df.format(ratingsAvg);
    }

    public void setCar(String car) {
        this.car = car;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public float getRatingsAvg() {
        return ratingsAvg;
    }

    public void setRatingsAvg(float ratingsAvg) {
        this.ratingsAvg = ratingsAvg;
    }

    public LocationObject getLocation() {
        return mLocation;
    }

    public void setLocation(LocationObject mLocation) {
        this.mLocation = mLocation;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Boolean getActive() {
        return active;
    }

    public String getNotificationKey() {
        return notificationKey;
    }

    public float getPayoutAmount() {
        return payoutAmount;
    }

    public Boolean getConnect() {
        return connect;
    }
}
