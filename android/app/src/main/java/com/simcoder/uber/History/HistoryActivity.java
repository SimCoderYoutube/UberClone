package com.simcoder.uber.History;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.LinearLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.simcoder.uber.Objects.RideObject;
import com.simcoder.uber.Adapters.HistoryAdapter;
import com.simcoder.uber.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


/**
 * This activity displays a list of all the previous drives made
 * by the user.
 *
 * If the current user is a driver then it also displays a space with the
 * current money available for payout and a space for the user to place
 * the paypal email to which it is sent
 */
public class HistoryActivity extends AppCompatActivity {

    private RecyclerView.Adapter mHistoryAdapter;

    LinearLayout mEmpty;

    String idRef = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        RecyclerView mHistoryRecyclerView = findViewById(R.id.historyRecyclerView);
        mHistoryRecyclerView.setNestedScrollingEnabled(false);
        mHistoryRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mHistoryLayoutManager = new LinearLayoutManager(HistoryActivity.this);
        mHistoryRecyclerView.setLayoutManager(mHistoryLayoutManager);
        mHistoryAdapter = new HistoryAdapter(resultsHistory, HistoryActivity.this);
        mHistoryRecyclerView.setAdapter(mHistoryAdapter);

        mEmpty = findViewById(R.id.empty_layout);

        String customerOrDriver = getIntent().getExtras().getString("customerOrDriver");
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if(customerOrDriver.equals("Drivers")){
            idRef = "driverId";
        }else{
            idRef = "customerId";
        }


        getUserHistoryIds();
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
     * Fetch all of the rides that are completed and populate the
     * design elements
     */
    private void getUserHistoryIds() {

        String driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Query query = FirebaseDatabase.getInstance().getReference().child("ride_info").orderByChild(idRef).equalTo(driverId);

        query.addChildEventListener(new ChildEventListener() {

            @SuppressLint("SetTextI18n")
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(!dataSnapshot.exists()){return;}

                RideObject mRide = new RideObject();
                mRide.parseData(dataSnapshot);

                if(mRide.getCancelled()){
                    return;
                }
                mEmpty.setVisibility(View.GONE);
                resultsHistory.add(0,  mRide);
                mHistoryAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
            }
        });


    }
    private ArrayList<RideObject> resultsHistory = new ArrayList<>();


}















