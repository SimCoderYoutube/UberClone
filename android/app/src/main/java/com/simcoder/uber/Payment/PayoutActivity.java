package com.simcoder.uber.Payment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.simcoder.uber.Adapters.PayoutAdapter;
import com.simcoder.uber.Objects.DriverObject;
import com.simcoder.uber.Objects.PayoutObject;
import com.simcoder.uber.R;
import com.simcoder.uber.Utils.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class PayoutActivity extends AppCompatActivity {

    PaymentUtils.ResponseCallback cb;

    private RecyclerView.Adapter mAdapter;

    ArrayList<PayoutObject> payoutArrayList = new ArrayList<>();

    Button mPayout;

    TextView mPayoutAmount;

    DriverObject mDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payout);

        setupToolbar();

        mPayout = findViewById(R.id.payout);
        mPayoutAmount = findViewById(R.id.payout_amount);

        mPayout.setOnClickListener(v -> {
            if(mDriver == null){return;}

            //If driver does not have a connect account then start the process before it is able to payout
            if(mDriver.getConnect()){
                new PaymentUtils().payoutRequest(cb, PayoutActivity.this);
            }else{
                new PaymentUtils().startStripeConnect(PayoutActivity.this, getApplicationContext());
            }
        });


        cb = response -> {
            runOnUiThread(() -> {
                if(response == 200){
                    Snackbar.make(findViewById(R.id.layout), "Payout successful", Snackbar.LENGTH_LONG)
                            .show();
                }else{
                    Snackbar.make(findViewById(R.id.layout), "Something went Wrong", Snackbar.LENGTH_LONG)
                            .show();
                }
            });
        };


        initializeRecyclerView();
        getPayouts();
        getData();
    }

    /**
     * Get the info of the driver and update the payout amount displayed to the driver
     */
    private void getData() {
        FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mDriver = new DriverObject();
                    mDriver.parseData(dataSnapshot);
                    String s = String.valueOf(new Utils().round(mDriver.getPayoutAmount(), 2));

                    mPayoutAmount.setText(s + getString(R.string.money_type));

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * fetches previous payouts made by the user and populates the recycler view
     */
    private void getPayouts(){
        FirebaseDatabase.getInstance().getReference().child("stripe_customers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("payouts").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for(DataSnapshot mData : dataSnapshot.getChildren()){
                        PayoutObject mPayout = new PayoutObject(PayoutActivity.this);
                        mPayout.parseData(mData);

                        boolean exists = false;
                        for(PayoutObject mPayoutIt : payoutArrayList){
                            if (mPayoutIt.getId().equals(mPayout.getId())) {
                                exists = true;
                                break;
                            }
                        }
                        if(!exists){
                            payoutArrayList.add(0, mPayout);
                            mAdapter.notifyDataSetChanged();
                            getData();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    /**
     * Init the payout recycler view
     */
    public void initializeRecyclerView(){
        RecyclerView mRecyclerView = findViewById(R.id.recyclerView);
        mRecyclerView.setNestedScrollingEnabled(false);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new PayoutAdapter(payoutArrayList, PayoutActivity.this);
        mRecyclerView.setAdapter(mAdapter);
    }

    /**
     * Sets up toolbar with custom text and a listener
     * to go back to the previous activity
     */
    private void setupToolbar() {
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(getString(R.string.payout));
        myToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        myToolbar.setNavigationOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        getData();
    }
}
