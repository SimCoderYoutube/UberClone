package com.simcoder.uber.Payment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.simcoder.uber.R;
import com.stripe.android.ApiResultCallback;
import com.stripe.android.SetupIntentResult;
import com.stripe.android.Stripe;
import com.stripe.android.model.ConfirmSetupIntentParams;
import com.stripe.android.model.PaymentMethod;
import com.stripe.android.model.PaymentMethodCreateParams;
import com.stripe.android.model.SetupIntent;
import com.stripe.android.view.CardInputWidget;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AddPaymentActivity extends AppCompatActivity {

    private OkHttpClient httpClient = new OkHttpClient();
    private String setupIntentClientSecret;
    private Stripe stripe;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_payment);
        loadPage();
        setupToolbar();
    }

    /**
     * Sets up toolbar with custom text and a listener
     * to go back to the previous activity
     */
    private void setupToolbar() {
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setTitle(getString(R.string.add_card));
        myToolbar.setTitleTextColor(getResources().getColor(R.color.white));
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setDisplayHomeAsUpEnabled(true);
        myToolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadPage() {
        // Create a SetupIntent by calling the sample server's /create-setup-intent endpoint.

        JSONObject jsonObject = new JSONObject ();
        try {
            jsonObject.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, String.valueOf(jsonObject));

        Request request = new Request.Builder()
                .url(getResources().getString(R.string.firebase_functions_base_url) + "create_setup_intent")
                .post(body)
                .build();
        httpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        runOnUiThread(() -> {
                            Context applicationContext = getApplicationContext();
                            Toast.makeText(applicationContext, "Error: " + e.toString(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull final Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            runOnUiThread(() -> {
                                Context applicationContext = getApplicationContext();
                                Toast.makeText(applicationContext, "Error: " + response.toString(), Toast.LENGTH_LONG).show();
                            });
                        } else {
                            Gson gson = new Gson();
                            Type type = new TypeToken<Map<String, String>>(){}.getType();
                            assert response.body() != null;
                            Map<String, String> responseMap = gson.fromJson(response.body().string(), type);

                            // The response from the server includes the Stripe publishable key and
                            // SetupIntent details.
                            setupIntentClientSecret = responseMap.get("clientSecret");

                            // Use the key from the server to initialize the Stripe instance.
                            stripe = new Stripe(getApplicationContext(), responseMap.get("publishableKey"));
                        }
                    }
                });

        // Hook up the pay button to the card widget and stripe instance
        Button payButton = findViewById(R.id.payButton);
        payButton.setOnClickListener((View view) -> {
            // Collect card details
            CardInputWidget cardInputWidget = findViewById(R.id.cardInputWidget);
            PaymentMethodCreateParams.Card card = cardInputWidget.getPaymentMethodCard();

            // Later, you will need to attach the PaymentMethod to the Customer it belongs to.
            // This example collects the customer's email to know which customer the PaymentMethod belongs to, but your app might use an account id, session cookie, etc.
            EditText emailInput = findViewById(R.id.emailInput);
            PaymentMethod.BillingDetails billingDetails = (new PaymentMethod.BillingDetails.Builder())
                    .setEmail(emailInput.getText().toString())
                    .build();
            if (card != null) {
                // Create SetupIntent confirm parameters with the above
                PaymentMethodCreateParams paymentMethodParams = PaymentMethodCreateParams
                        .create(card, billingDetails);
                ConfirmSetupIntentParams confirmParams = ConfirmSetupIntentParams
                        .create(paymentMethodParams, setupIntentClientSecret);
                stripe.confirmSetupIntent(this, confirmParams);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        WeakReference<Activity> weakActivity = new WeakReference<>(this);

        // Handle the result of stripe.confirmSetupIntent
        stripe.onSetupResult(requestCode, data, new ApiResultCallback<SetupIntentResult>() {
            @Override
            public void onSuccess(@NonNull SetupIntentResult result) {
                SetupIntent setupIntent = result.getIntent();
                SetupIntent.Status status = setupIntent.getStatus();
                if (status == SetupIntent.Status.Succeeded) {
                    // Setup completed successfully
                    runOnUiThread(() -> {
                        if (weakActivity.get() != null) {
                            finish();
                        }
                    });
                } else if (status == SetupIntent.Status.RequiresPaymentMethod) {
                    // Setup failed – allow retrying using a different payment method
                    runOnUiThread(() -> {
                        Activity activity = weakActivity.get();
                        if (activity != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                            builder.setTitle("Setup failed");
                            builder.setMessage(setupIntent.getLastSetupError().getMessage());
                            builder.setPositiveButton("Ok", (DialogInterface dialog, int index) -> {
                                CardInputWidget cardInputWidget = findViewById(R.id.cardInputWidget);
                                cardInputWidget.clear();
                                EditText emailInput = findViewById(R.id.emailInput);
                                emailInput.setText(null);
                            });
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull Exception e) {
                // Setup request failed – allow retrying using the same payment method
                runOnUiThread(() -> {
                    Activity activity = weakActivity.get();
                    if (activity != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setMessage(e.toString());
                        builder.setPositiveButton("Ok", null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
            }
        });
    }
}