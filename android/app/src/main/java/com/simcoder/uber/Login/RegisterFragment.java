package com.simcoder.uber.Login;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.simcoder.uber.R;

import org.jetbrains.annotations.NotNull;


/**
 * Fragment Responsible for registering a new user
 */
public class RegisterFragment extends Fragment implements View.OnClickListener {

    private EditText    mEmail,
                        mPassword;

    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null)
            view = inflater.inflate(R.layout.fragment_registration, container, false);
        else
            container.removeView(view);


        return view;
    }
    @Override
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeObjects();
    }



    /**
     * Register the user, but before that check if every field is correct.
     * After that registers the user and creates an entry for it oin the database
     */
    private void register(){
        if(mEmail.getText().length()==0) {
            mEmail.setError("please fill this field");
            return;
        }
        if(mPassword.getText().length()==0) {
            mPassword.setError("please fill this field");
            return;
        }
        if(mPassword.getText().length()< 6) {
            mPassword.setError("password must have at least 6 characters");
            return;
        }



        final String email = mEmail.getText().toString();
        final String password = mPassword.getText().toString();


        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(getActivity(), task -> {
            if(!task.isSuccessful()){
                Snackbar.make(view.findViewById(R.id.layout), "sign up error", Snackbar.LENGTH_SHORT).show();
            }
        });

    }


    /**
     * Initializes the design Elements and calls clickListeners for them
     */
    private void initializeObjects(){
        mEmail = view.findViewById(R.id.email);
        mPassword = view.findViewById(R.id.password);
        Button mRegister = view.findViewById(R.id.register);

        mRegister.setOnClickListener(this);
    }
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.register) {
            register();
        }
    }
}