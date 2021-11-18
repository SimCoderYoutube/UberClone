package com.simcoder.uber.Utils;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.firebase.geofire.GeoFire;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * On App Killed disconnect driver from the database
 */
public class onAppKilled extends Service {

    /**
     * Disconnects driver by removing the driversWorking driver id from the database
     */
    void disconnectDriver(){
        DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
        GeoFire geoFireWorking = new GeoFire(refWorking);
        geoFireWorking.removeLocation(FirebaseAuth.getInstance().getCurrentUser().getUid());
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        disconnectDriver();
    }
}

