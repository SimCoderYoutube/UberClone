package com.simcoder.uber.Driver;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.Gravity;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.lorentzos.flingswipe.SwipeFlingAdapterView;
import com.ncorti.slidetoact.SlideToActView;
import com.simcoder.uber.Adapters.CardRequestAdapter;
import com.simcoder.uber.Objects.DriverObject;
import com.simcoder.uber.History.HistoryActivity;
import com.simcoder.uber.Login.LauncherActivity;
import com.simcoder.uber.Payment.PayoutActivity;
import com.simcoder.uber.R;
import com.simcoder.uber.Objects.RideObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Main Activity displayed to the driver
 */
public class DriverMapActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, DirectionCallback {

    int MAX_SEARCH_DISTANCE = 20;

    private GoogleMap mMap;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private FusedLocationProviderClient mFusedLocationClient;

    private SlideToActView mRideStatus;

    private Switch mWorkingSwitch;


    private LinearLayout mCustomerInfo, mBringUpBottomLayout;

    private TextView mCustomerName;
    DatabaseReference mUser;

    RideObject mCurrentRide;

    Marker pickupMarker, destinationMarker;

    DriverObject mDriver = new DriverObject();

    TextView mUsername, mLogout;

    private ValueEventListener driveHasEndedRefListener;

    private CardRequestAdapter cardRequestAdapter;

    List<RideObject> requestList = new ArrayList<>();

    View mBottomSheet;
    BottomSheetBehavior<View> mBottomSheetBehavior;

    GeoQuery geoQuery;

    boolean started = false;
    boolean zoomUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_driver);

        Toolbar toolbar = findViewById(R.id.toolbar);


        polylines = new ArrayList<>();


        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        mUser = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(FirebaseAuth.getInstance().getUid());
        mCustomerInfo = findViewById(R.id.customerInfo);

        mBringUpBottomLayout = findViewById(R.id.bringUpBottomLayout);

        mCustomerName = findViewById(R.id.name);
        mUsername = navigationView.getHeaderView(0).findViewById(R.id.usernameDrawer);
        FloatingActionButton mMaps = findViewById(R.id.openMaps);
        FloatingActionButton mCall = findViewById(R.id.phone);
        ImageView mCancel = findViewById(R.id.cancel);
        mRideStatus = findViewById(R.id.rideStatus);
        mLogout = findViewById(R.id.logout);

        mWorkingSwitch = findViewById(R.id.workingSwitch);

        mLogout.setOnClickListener(v -> logOut());

        mWorkingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!mDriver.getActive()) {
                Toast.makeText(DriverMapActivity.this, R.string.not_approved, Toast.LENGTH_LONG).show();
                mWorkingSwitch.setChecked(false);
                return;
            }
            if (isChecked) {
                connectDriver();
            } else {
                disconnectDriver();
            }
        });

        mRideStatus.setOnSlideCompleteListener(v -> {
            switch (mCurrentRide.getState()) {
                case 1:
                    if (mCurrentRide == null) {
                        return;
                    }
                    mCurrentRide.pickedCustomer();
                    break;
                case 2:
                    if (mCurrentRide != null)
                        mCurrentRide.recordRide();
                    break;
            }
        });

        mMaps.setOnClickListener(view -> {
            if (mCurrentRide.getState() == 1) {
                openMaps(mCurrentRide.getPickup().getCoordinates().latitude, mCurrentRide.getPickup().getCoordinates().longitude);
            } else {
                openMaps(mCurrentRide.getDestination().getCoordinates().latitude, mCurrentRide.getDestination().getCoordinates().longitude);
            }
        });

        mCall.setOnClickListener(view -> {
            if (mCurrentRide == null) {
                Snackbar.make(findViewById(R.id.drawer_layout), getString(R.string.driver_no_phone), Snackbar.LENGTH_LONG).show();
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mCurrentRide.getCustomer().getPhone()));
                startActivity(intent);
            } else {
                Snackbar.make(findViewById(R.id.drawer_layout), getString(R.string.no_phone_call_permissions), Snackbar.LENGTH_LONG).show();
            }
        });

        mCancel.setOnClickListener(v -> {
            mCurrentRide.cancelRide();
            endRide();
        });
        ImageView mDrawerButton = findViewById(R.id.drawerButton);
        mDrawerButton.setOnClickListener(v -> drawer.openDrawer(Gravity.LEFT));

        mBringUpBottomLayout = findViewById(R.id.bringUpBottomLayout);
        mBringUpBottomLayout.setOnClickListener(v -> {
            if (mBottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            else
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

            if (mCurrentRide == null) {
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        getUserData();
        //TODO getAssignedCustomer();
        initializeRequestCardSwipe();
        isRequestInProgress();

        ViewTreeObserver vto = mBringUpBottomLayout.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(this::initializeBottomLayout);

    }

    /**
     * Open a maps application to show the driver the route between the pickup point and destination.
     * It tries to first open waze, if it fails it will try to open up maps
     *
     * @param latitude  - destination's latitude
     * @param longitude - destination's longitude
     */
    private void openMaps(double latitude, double longitude) {
        try {
            String url = "https://waze.com/ul?ll=" + latitude + "," + longitude + "&navigate=yes";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                    Uri.parse("http://maps.google.com/maps?daddr=" + latitude + "," + longitude));
            startActivity(intent);
        }
    }


    /**
     * Initialize swipe cards and add listeners that will control events fo it:
     * - Left swipe: Dismiss the ride request
     * - right swipe: accept ride request
     */
    private void initializeRequestCardSwipe() {
        cardRequestAdapter = new CardRequestAdapter(getApplicationContext(), R.layout.item__card_request, requestList);

        final SwipeFlingAdapterView flingContainer = findViewById(R.id.frame);

        flingContainer.setAdapter(cardRequestAdapter);

        //Handling swipe of cards
        flingContainer.setFlingListener(new SwipeFlingAdapterView.onFlingListener() {
            @Override
            public void removeFirstObjectInAdapter() {
                requestList.remove(0);
                cardRequestAdapter.notifyDataSetChanged();
            }

            @Override
            public void onLeftCardExit(Object dataObject) {
                RideObject mRide = (RideObject) dataObject;
                requestList.remove(mRide);
                cardRequestAdapter.notifyDataSetChanged();
            }

            @Override
            public void onRightCardExit(Object dataObject) {
                RideObject mRide = (RideObject) dataObject;

                if (mRide.getDriver() == null) {

                    try {
                        mCurrentRide = (RideObject) mRide.clone();
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                    mCurrentRide.confirmDriver();
                    requestListener();
                }

            }

            @Override
            public void onAdapterAboutToEmpty(int itemsInAdapter) {
            }

            @Override
            public void onScroll(float scrollProgressPercent) {
            }
        });

    }

    /**
     * Listener for the bottom popup. This will control
     * when it is shown and when it isn't according to the actions of the users
     * of pulling on it or just clicking on it.
     */
    private void initializeBottomLayout() {
        mBottomSheet = findViewById(R.id.bottomSheet);
        mBottomSheetBehavior = BottomSheetBehavior.from(mBottomSheet);
        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setPeekHeight(mBringUpBottomLayout.getHeight());


        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (mCurrentRide == null) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

    }


    /**
     * Fetches current user's info and populates the design element.
     * Also checks if the user was working before closing the app and, if so,
     * connect the driver and set the radio button to "working"
     */
    private void getUserData() {
        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCustomerRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverId);
        assignedCustomerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mDriver.parseData(dataSnapshot);

                    mUsername.setText(mDriver.getName());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        FirebaseDatabase.getInstance().getReference("driversWorking").child(driverId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    connectDriver();
                } else {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    disconnectDriver();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Checks if request is in progress by looking at the last ride_info child that the
     * current driver was a part of and if that last ride is still ongoing then
     * start all of the relevant variables up, with that ride info.
     */
    private void isRequestInProgress() {
        FirebaseDatabase.getInstance().getReference().child("ride_info").orderByChild("driverId").equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid()).limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }

                for(DataSnapshot mData : dataSnapshot.getChildren()){
                    mCurrentRide = new RideObject();
                    mCurrentRide.parseData(mData);

                    if (mCurrentRide.getCancelled() || mCurrentRide.getEnded()) {
                        endRide();
                        return;
                    }

                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    requestListener();
                }

            }

            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
            }
        });
    }

    /**
     * Use the mCurrentRide variable to check which is the current state of it and do all
     * The necessary work according to that state.
     */
    private void checkRequestState() {
        switch (mCurrentRide.getState()) {
            case 1:
                destinationMarker = mMap.addMarker(new MarkerOptions().position(mCurrentRide.getDestination().getCoordinates()).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_filled)));
                pickupMarker = mMap.addMarker(new MarkerOptions().position(mCurrentRide.getPickup().getCoordinates()).title("Pickup").icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio)));

                mRideStatus.setText(getResources().getString(R.string.picked_customer));
                mRideStatus.resetSlider();

                mCustomerName.setText(mCurrentRide.getDestination().getName());

                getAssignedCustomerInfo();

                requestList.clear();
                cardRequestAdapter.notifyDataSetChanged();
                erasePolylines();
                getRouteToMarker(mCurrentRide.getPickup().getCoordinates());
                break;
            case 2:
                erasePolylines();
                if (mCurrentRide.getDestination().getCoordinates().latitude != 0.0 && mCurrentRide.getDestination().getCoordinates().longitude != 0.0) {
                    getRouteToMarker(mCurrentRide.getDestination().getCoordinates());
                }
                mRideStatus.setText(getResources().getString(R.string.drive_complete));
                mRideStatus.resetSlider();
                break;
            default:
                endRide();

        }
    }

    /**
     * Get Closest Rider by getting all the requests available
     * within a radius of MAX_SEARCH_DISTANCE and around the driver current location.
     * If a request is found and the driver is not attributed to a request at the moment
     * then call getRequestInfo(key), key being the id of the request.
     */
    private void getRequestsAround() {
        if (mLastLocation == null) {
            return;
        }

        DatabaseReference requestLocation = FirebaseDatabase.getInstance().getReference().child("customer_requests");

        GeoFire geoFire = new GeoFire(requestLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), MAX_SEARCH_DISTANCE);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!mWorkingSwitch.isChecked()){
                    return;
                }

                if (mCurrentRide == null) {
                    for (RideObject mRideIt : requestList) {
                        if (mRideIt.getId().equals(key)) {
                            return;
                        }
                    }

                    getRequestInfo(key);

                }else{
                    requestList.clear();
                }
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }

    /**
     * Get info of a request and if it has not ended or been cancelled then add it to the
     * requestList which will push a card of the request to the driver screen.
     *
     * @param key - id of the request to fetch the info of
     */
    private void getRequestInfo(String key) {
        FirebaseDatabase.getInstance().getReference().child("ride_info").child(key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    return;
                }

                if (mCurrentRide != null) {
                    return;
                }


                RideObject mRide = new RideObject();
                mRide.parseData(dataSnapshot);


                if(!mRide.getRequestService().equals(mDriver.getService())){
                    return;
                }


                for (RideObject mRideIt : requestList) {
                    if (mRideIt.getId().equals(mRide.getId())) {
                        if (mRide.getCancelled() || mRide.getEnded() || mRide.getDriver() != null) {
                            requestList.remove(mRideIt);
                            cardRequestAdapter.notifyDataSetChanged();
                        }
                        return;
                    }
                }

                if (!mRide.getCancelled() && !mRide.getEnded() && mRide.getDriver() == null && mRide.getState() == 0) {
                    requestList.add(mRide);
                    cardRequestAdapter.notifyDataSetChanged();
                    makeSound();

                    HashMap<String, Object> map = new HashMap<String, Object>();
                    map.put("timestamp_last_driver_read", ServerValue.TIMESTAMP);
                    FirebaseDatabase.getInstance().getReference().child("ride_info").child(key).updateChildren(map);
                }
            }

            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
            }
        });
    }

    /**
     * Issues a notification sound to the driver.
     */
    private void makeSound() {
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.driver_notification);
        mp.start();
    }


    /**
     * Listener for the request the driver is currently assigned to.
     */
    private void requestListener() {
        if (mCurrentRide == null) {
            return;
        }

        driveHasEndedRefListener = mCurrentRide.getRideRef().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    return;
                }
                mCurrentRide.parseData(dataSnapshot);

                //if drive has ended or been cancelled then call endRide to retrieve all variables to their default state
                if (mCurrentRide.getCancelled() || mCurrentRide.getEnded()) {
                    endRide();
                    return;
                }

                checkRequestState();
            }

            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
            }
        });
    }


    /**
     * Get Route from pickup to destination, showing the route to the user
     * @param destination - LatLng of the location to go to
     */
    private void getRouteToMarker(LatLng destination) {
        String serverKey = getResources().getString(R.string.google_maps_key);
        if (destination != null && mLastLocation != null) {
            GoogleDirection.withServerKey(serverKey)
                    .from(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                    .to(destination)
                    .transportMode(TransportMode.DRIVING)
                    .execute(this);
        }
    }


    /**
     * Fetch assigned customer's info and display it in the Bottom sheet
     */
    private void getAssignedCustomerInfo() {
        if (mCurrentRide.getCustomer().getId() == null) {
            return;
        }
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(mCurrentRide.getCustomer().getId());
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    return;
                }

                if (mCurrentRide != null) {
                    mCurrentRide.getCustomer().parseData(dataSnapshot);

                    mCustomerName.setText(mCurrentRide.getCustomer().getName());
                }

                mCustomerInfo.setVisibility(View.VISIBLE);
                mBottomSheetBehavior.setHideable(false);
                mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }

            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
            }
        });
    }

    /**
     * End Ride by removing all of the active listeners,
     * returning all of the values to the default state
     * and clearing the map from markers
     */
    private void endRide() {
        if (mCurrentRide == null) {
            return;
        }

        if (driveHasEndedRefListener != null) {
            mCurrentRide.getRideRef().removeEventListener(driveHasEndedRefListener);
        }


        mRideStatus.setText(getString(R.string.picked_customer));
        erasePolylines();

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(userId).child("customerRequest");
        driverRef.removeValue();

        //Remove the request from the geofire child so that other drivers don't have to check this request in the future
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(mCurrentRide.getId(), (key, error) -> {
        });

        mCurrentRide = null;

        if (pickupMarker != null) {
            pickupMarker.remove();
        }
        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        mBottomSheetBehavior.setHideable(true);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        mCustomerName.setText("");

        mMap.clear();
        getRequestsAround();

        //This will allow the map to re-zoom on the current location
        zoomUpdated = false;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        googleMap.setMapStyle(new MapStyleOptions(getResources().getString(R.string.style_json)));

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);//interval with which the driver location will be updated
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            } else {
                checkLocationPermission();
            }
        }
    }

    /**
     * Gets location of the current user and in the case of the driver updates
     * the database with the most current location so that customers can get
     * info on the drivers around them.
     */
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }

            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {

                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("driversWorking");
                    GeoFire geoFireWorking = new GeoFire(refWorking);

                    if (!mWorkingSwitch.isChecked()) {
                        geoFireWorking.removeLocation(userId, (key, error) -> {
                        });
                        return;
                    }


                    geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()), (key, error) -> {
                    });

                    if (mCurrentRide != null && mLastLocation != null) {
                        mCurrentRide.setRideDistance(mCurrentRide.getRideDistance() + mLastLocation.distanceTo(location) / 1000);
                    }

                    mLastLocation = location;
                    
                    if (!started) {
                        getRequestsAround();
                        started = true;
                    }

                    Map<String, Object> newUserMap = new HashMap<>();
                    newUserMap.put("last_updated", ServerValue.TIMESTAMP);
                    mUser.updateChildren(newUserMap);

                    if (!zoomUpdated) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
                        zoomUpdated = true;
                    }
                }
            }
        }
    };


    /**
     * Get permissions for our app if they didn't previously exist.
     * requestCode: the number assigned to the request that we've made. Each
     * |                request has it's own unique request code.
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION) && ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                new android.app.AlertDialog.Builder(this)
                        .setTitle("give permission")
                        .setMessage("give permission message")
                        .setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1))
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(DriverMapActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CALL_PHONE}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(getApplicationContext(), "Please provide the permission", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void logOut() {
        disconnectDriver();

        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(DriverMapActivity.this, LauncherActivity.class);
        startActivity(intent);
        finish();
    }


    /**
     * Connects driver, waking up the code that fetches current location
     */
    private void connectDriver() {
        mWorkingSwitch.setChecked(true);
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }

    /**
     * Disconnects driver, putting to sleep the code that fetches current location
     */
    private void disconnectDriver() {
        mWorkingSwitch.setChecked(false);
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("driversAvailable").child(userId);
        ref.removeValue();
    }

    private List<Polyline> polylines;

    /**
     * Remove route polylines from the map
     */
    private void erasePolylines() {
        for (Polyline line : polylines) {
            line.remove();
        }
        polylines.clear();
    }

    /**
     * Show map within the pickup and destination marker,
     * This will make sure everything is displayed to the user
     *
     * @param route - route between pickup and destination
     */
    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
    }

    /**
     * Checks if route where fetched successfully, if yes then
     * add them to the map
     *
     * @param direction - Direction object
     * @param rawBody   - data of the route
     */
    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {
        if (direction.isOK()) {
            Route route = direction.getRouteList().get(0);

            ArrayList<LatLng> directionPositionList = route.getLegList().get(0).getDirectionPoint();
            Polyline polyline = mMap.addPolyline(DirectionConverter.createPolyline(this, directionPositionList, 5, Color.BLACK));
            polylines.add(polyline);
            setCameraWithCoordinationBounds(route);
        }
    }

    @Override
    public void onDirectionFailure(Throwable t) {

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.history) {
            Intent intent = new Intent(DriverMapActivity.this, HistoryActivity.class);
            intent.putExtra("customerOrDriver", "Drivers");
            startActivity(intent);
        } else if (id == R.id.settings) {
            Intent intent = new Intent(DriverMapActivity.this, DriverSettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.payout) {
            Intent intent = new Intent(DriverMapActivity.this, PayoutActivity.class);
            startActivity(intent);
        } else if (id == R.id.logout) {
            logOut();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
