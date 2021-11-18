package com.simcoder.uber.Login;

import android.content.Intent;

import androidx.fragment.app.FragmentManager;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.simcoder.uber.R;

/**
 * This Activity controls the display of auth fragments of the app:
 * -MenuFragment
 * -LoginFragment
 * -RegisterFragment
 * <p>
 * It is also responsible for taking the user to the main activity if the login or register process is successful
 */
public class AuthenticationActivity extends AppCompatActivity {

    FragmentManager fm = getSupportFragmentManager();

    MenuFragment menuFragment = new MenuFragment();

    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);


        //Listens for changes in the auth state
        firebaseAuthListener = firebaseAuth -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                Intent intent = new Intent(AuthenticationActivity.this, LauncherActivity.class);
                startActivity(intent);
                finish();
            }
        };

        fm.beginTransaction()
                .replace(R.id.container, menuFragment, "StartFragment")
                .addToBackStack(null)
                .commit();
    }

    /**
     * Displays the RegisterFragment
     */
    public void registrationClick() {
        fm.beginTransaction()
                .replace(R.id.container, new RegisterFragment(), "RegisterFragment")
                .addToBackStack(null)
                .commit();
    }

    /**
     * Displays the LoginFragment
     */
    public void loginClick() {
        fm.beginTransaction()
                .replace(R.id.container, new LoginFragment(), "RegisterFragment")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(firebaseAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseAuth.getInstance().removeAuthStateListener(firebaseAuthListener);
    }
}
