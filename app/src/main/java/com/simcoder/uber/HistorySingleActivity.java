package com.simcoder.uber;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
    }
}
