package com.simcoder.uber.Objects;

import android.content.Context;
import android.text.format.DateFormat;

import com.google.firebase.database.DataSnapshot;

import java.math.BigDecimal;
import java.util.Calendar;


/**
 * Type of car object, contains the info of each type of car
 * to be used by the type adapter.
 */
public class PayoutObject {

    String id, amount, date;
    Long timestamp;
    Context context;

    /**
     * RideObject constructor
     * @param context - context of the parent activity
     */
    public PayoutObject(Context context){
        this.context = context;
    }


    /**
     * Parse the Datasnapshot retrieved from the database and puts it into a RideObject
     * @param dataSnapshot - datasnapshot of the ride
     */
    public void parseData(DataSnapshot dataSnapshot){
        id = dataSnapshot.getKey();

        if(dataSnapshot.child("amount").getValue()!=null){
            float amountInt = Float.parseFloat(dataSnapshot.child("amount").getValue().toString()) / 100;
            amount = String.valueOf(amountInt);
        }
        if(dataSnapshot.child("created").getValue() != null){
            timestamp = Long.valueOf(dataSnapshot.child("created").getValue().toString());
        }

        Calendar cal = Calendar.getInstance(context.getResources().getConfiguration().locale);
        cal.setTimeInMillis(timestamp * 1000);
        date = DateFormat.format("dd-MM-yyyy", cal).toString();
    }
    public String getId() {
        return id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }


    /**
     * Rounds number up
     * @param decimalPlace - int to round
     * @return rounded number
     */
    public BigDecimal round(int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(Float.parseFloat(amount)));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

}
