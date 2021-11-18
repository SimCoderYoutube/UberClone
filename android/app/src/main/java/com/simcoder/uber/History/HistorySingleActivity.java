package com.simcoder.uber.History;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.paypal.android.sdk.payments.PayPalService;
import com.simcoder.uber.Objects.RideObject;
import com.simcoder.uber.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This activity displays a single previous ride in detail.
 *
 * If you are a customer then it allows you to both rate the driver
 * and pay for the ride.
 */
public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, DirectionCallback {
    private String rideId;

    private TextView mPickup;
    private TextView mDestination;
    private TextView mPrice;
    private TextView mCar;
    private TextView mDate;
    private TextView userName;
    private TextView userPhone;

    private ImageView userImage;

    private RatingBar mRatingBar;

    private DatabaseReference historyRideInfoDb;

    private GoogleMap mMap;

    LinearLayout mRatingBarContainer;

    RideObject mRide = new RideObject();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        polyline = new ArrayList<>();

        rideId = getIntent().getExtras().getString("rideId");

        SupportMapFragment mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mMapFragment != null) {
            mMapFragment.getMapAsync(this);
        }


        mDestination = findViewById(R.id.destination_location);
        mPickup = findViewById(R.id.pickup_location);
        mCar = findViewById(R.id.car);
        mDate = findViewById(R.id.time);
        mPrice = findViewById(R.id.price);


        userName = findViewById(R.id.userName);
        userPhone = findViewById(R.id.userPhone);
        TextView userMail = findViewById(R.id.email);
        userImage = findViewById(R.id.userImage);

        mRatingBar = findViewById(R.id.ratingBar);
        mRatingBarContainer = findViewById(R.id.ratingBar_container);

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("ride_info").child(rideId);
        getRideInformation();
        setupToolbar();
    }

    /**
     * Sets up toolbar with custom text and a listener
     * to go back to the previous activity
     */
    private void setupToolbar() {
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(getString(R.string.your_trips));
        myToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        myToolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Fetches the info on the current ride and populates the design elements
     */
    private void getRideInformation() {
        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){return;}
                mRide.parseData(dataSnapshot);
                if(mRide.getDriver().getId().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                    getUserInformation("Customers", mRide.getCustomer().getId());
                    mRatingBarContainer.setVisibility(View.GONE);
                }else{
                    getUserInformation("Drivers", mRide.getDriver().getId());
                    displayCustomerRelatedObjects();
                }


                mDate.setText(mRide.getDate());
                mPrice.setText(mRide.getPriceString() + " €");
                mDestination.setText(mRide.getDestination().getName());
                mPickup.setText(mRide.getPickup().getName());
                mCar.setText(mRide.getCar());
                mRatingBar.setRating(mRide.getRating());



                getRouteToMarker();
            }
            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
            }
        });
    }

    /**
     * Displays the elements that are only available to the customer:
     *  - Rating bar
     *  - pay button
     */
    private void displayCustomerRelatedObjects() {
        mRatingBarContainer.setVisibility(View.VISIBLE);

        mRatingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            historyRideInfoDb.child("rating").setValue(rating);
            DatabaseReference mDriverRatingDb = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(mRide.getDriver().getId()).child("rating");
            mDriverRatingDb.child(rideId).setValue(rating);
        });
    }


    @Override
    protected void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        super.onDestroy();
    }


    /**
     * Fetches other user information and populates the relevant design elements
     * @param otherUserDriverOrCustomer - String "customer" or "driver"
     * @param otherUserId - id of the user whom we want to fetch the info of
     */
    private void getUserInformation(String otherUserDriverOrCustomer, String otherUserId) {
        DatabaseReference mOtherUserDB = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserDriverOrCustomer).child(otherUserId);
        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if(map == null){
                        return;
                    }
                    if (map.get("name") != null) {
                        userName.setText(map.get("name").toString());
                    }
                    if(map.get("phone") != null){
                        userPhone.setText(map.get("phone").toString());
                    }
                    if(map.get("profileImageUrl") != null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).apply(RequestOptions.circleCropTransform()).into(userImage);
                    }
                }

            }
            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
            }
        });
    }


    /**
     * Get Route from pickup to destination, showing the route to the user
     */
    private void getRouteToMarker() {
        String serverKey = getResources().getString(R.string.google_maps_key);
        if (mRide.getPickup() != null && mRide.getDestination() != null){
            GoogleDirection.withServerKey(serverKey)
                    .from(mRide.getDestination().getCoordinates())
                    .to(mRide.getPickup().getCoordinates())
                    .transportMode(TransportMode.DRIVING)
                    .execute(this);

            mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_finish)).position(mRide.getDestination().getCoordinates()).title("destination"));
            mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_start)).position(mRide.getPickup().getCoordinates()).title("pickup"));
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap=googleMap;
        googleMap.setMapStyle(new MapStyleOptions(getResources()
                .getString(R.string.style_json)));

    }


    private List<Polyline> polyline;
    /**
     * Show map within the pickup and destination marker,
     * This will make sure everything is displayed to the user
     * @param route - route between pickup and destination
     */
    private void setCameraWithCoordinationBounds(Route route) {
        LatLng southwest = route.getBound().getSouthwestCoordination().getCoordination();
        LatLng northeast = route.getBound().getNortheastCoordination().getCoordination();
        LatLngBounds bounds = new LatLngBounds(southwest, northeast);
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
    }

    /**
     * Checks if route where fetched successfully, if yes then
     * add them to the map
     * @param direction - Direction object
     * @param rawBody - data of the route
     */
    @Override
    public void onDirectionSuccess(Direction direction, String rawBody) {
        if (direction.isOK()) {
            Route route = direction.getRouteList().get(0);

            ArrayList<LatLng> directionPositionList = route.getLegList().get(0).getDirectionPoint();
            Polyline polyline = mMap.addPolyline(DirectionConverter.createPolyline(this, directionPositionList, 5, Color.BLACK));
            this.polyline.add(polyline);
            setCameraWithCoordinationBounds(route);

        }
    }

    @Override
    public void onDirectionFailure(Throwable t) {
    }


}
