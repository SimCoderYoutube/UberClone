package com.simcoder.uber.Payment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.simcoder.uber.Objects.CardObject;
import com.simcoder.uber.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PaymentUtils {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json");

    public interface CardListCallback {
        void onResult(ArrayList<CardObject> cardArrayList);
    }

    public interface ResponseCallback {
        void onResult(int result);
    }


    /**
     * Start Stripe connect process by first creating a hash which will ensure the user security on the server side.
     * @param activity - activity that calls the function
     * @param context - context of the activity that calls the function
     */
    void startStripeConnect(Activity activity, Context context) {
        String hash = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).push().getKey();
        FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("connect_code").setValue(hash);

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://connect.stripe.com/express/oauth/authorize?redirect_uri=" + context.getResources().getString(R.string.firebase_hosting_url) + "&client_id=" + context.getResources().getString(R.string.stripe_client_key) + "&state=" + hash));
        activity.startActivity(browserIntent);
    }

    /**
     * Get the list of cards the Customer has available in the stripe dash.
     * The returned object is a json so this function does the parse and saves it into an array list
     */
    void fetchCardsList(CardListCallback callback, Context context) {
        final OkHttpClient client = new OkHttpClient();

        final Request request = new Request.Builder()
                .url(context.getResources().getString(R.string.firebase_functions_base_url) + "/listCustomerCards?uid=" + FirebaseAuth.getInstance().getCurrentUser().getUid())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                String jsonDataString = null;
                try {
                    jsonDataString = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String defaultCard;
                try {
                    JSONObject jsonData = null;
                    if (jsonDataString != null) {
                        jsonData = new JSONObject(jsonDataString);
                    }
                    else{
                        return;
                    }


                    defaultCard = jsonData.getString("default_payment_method");

                    JSONArray cardJsonArray = new JSONArray(jsonData.getString("cards"));

                    ArrayList<CardObject> cardArrayList = new ArrayList<>();

                    for (int i = 0; i < cardJsonArray.length(); i++) {
                        JSONObject cardJson = cardJsonArray.getJSONObject(i);
                        CardObject mCard = new CardObject(cardJson.getString("id"));
                        JSONObject cardDetailsJson = cardJson.getJSONObject("card");
                        mCard.setBrand(cardDetailsJson.getString("brand"));
                        mCard.setExpMonth(cardDetailsJson.getInt("exp_month"));
                        mCard.setExpYear(cardDetailsJson.getInt("exp_year"));
                        mCard.setLastDigits(cardDetailsJson.getInt("last4"));
                        if(mCard.getId().equals(defaultCard)){
                            mCard.setDefaultCard(true);
                        }
                        cardArrayList.add(mCard);
                    }

                    if(callback != null){
                        callback.onResult(cardArrayList);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    /**
     * Start a payout request to the server.
     * @param context - context of the activity that called the function
     */
    void payoutRequest(ResponseCallback cb, Context context) {

        AlertDialog mAlert = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.payout_issued))
                .setMessage(context.getString(R.string.wait_send_payout))
                .setCancelable(false)
                .setIcon(R.drawable.ic_attach_money_black_24dp)
                .show();

        final OkHttpClient client = new OkHttpClient();
        JSONObject postData = new JSONObject();
        try {
            postData.put("uid", FirebaseAuth.getInstance().getUid());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());

        final Request request = new Request.Builder()
                .url(context.getResources().getString(R.string.firebase_functions_base_url) + "payout")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("cache-control", "no-cache")
                .addHeader("Authorization", "Your Token")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

                if(cb != null){
                    cb.onResult(400);
                }
                mAlert.dismiss();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if(cb != null){
                    cb.onResult(200);
                }
                mAlert.dismiss();
            }
        });
    }


    /**
     * Set default card for the current logged in user, this is just an http request, the
     * server handles all the hard work.
     * @param paymentId - id of the card to set to default
     * @param callback - Callback which will send the details back to the frontend
     * @param context  - context of the activity that called the function
     */
    void setDefaultCard(String paymentId, CardListCallback callback, Context context) {
        final OkHttpClient client = new OkHttpClient();
        JSONObject postData = new JSONObject();
        try {
            postData.put("uid", FirebaseAuth.getInstance().getUid());
            postData.put("payment_id", paymentId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());


        final Request request = new Request.Builder()
                .url(context.getResources().getString(R.string.firebase_functions_base_url) + "setDefaultCard")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("cache-control", "no-cache")
                .addHeader("Authorization", "Your Token")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if(callback != null){
                    fetchCardsList(callback, context);
                }
            }
        });
    }

    /**
     * remove a card for the current logged in user, this is just an http request, the
     * server handles all the hard work.
     * @param paymentId - id of the card to remove
     * @param callback - Callback which will send the details back to the frontend
     * @param context  - context of the activity that called the function
     */
    void removeCard(String paymentId, CardListCallback callback, Context context) {
        final OkHttpClient client = new OkHttpClient();
        JSONObject postData = new JSONObject();
        try {
            postData.put("uid", FirebaseAuth.getInstance().getUid());
            postData.put("payment_id", paymentId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(MEDIA_TYPE, postData.toString());


        final Request request = new Request.Builder()
                .url(context.getResources().getString(R.string.firebase_functions_base_url) + "removeCard")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("cache-control", "no-cache")
                .addHeader("Authorization", "Your Token")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if(callback != null){
                    fetchCardsList(callback, context);
                }
            }
        });
    }
}
