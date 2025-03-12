package com.example.quanlychitieu.auth;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.quanlychitieu.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class AuthViewModel extends AndroidViewModel {

    private final FirebaseAuth auth;
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<Boolean> loggedOutLiveData;
    private final GoogleSignInClient googleSignInClient;

    public AuthViewModel(@NonNull Application application) {
        super(application);

        auth = FirebaseAuth.getInstance();
        userLiveData = new MutableLiveData<>();
        loggedOutLiveData = new MutableLiveData<>();

        // Initialize Google Sign-In options and client
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(application.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(application, gso);

        // Check if the user is already signed in
        if (auth.getCurrentUser() != null) {
            userLiveData.postValue(auth.getCurrentUser());
            loggedOutLiveData.postValue(false);
        }
    }

    public void logOut() {
        auth.signOut();
        googleSignInClient.signOut()
                .addOnCompleteListener(task -> {
                    // Handle completion if needed, e.g., log success or failure
                    if (task.isSuccessful()) {
                        Log.d("AuthViewModel", "Successfully signed out from Google.");
                    } else {
                        Log.e("AuthViewModel", "Failed to sign out from Google.", task.getException());
                    }
                });

        // Update logout state
        userLiveData.postValue(null);
        loggedOutLiveData.postValue(true);
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getLoggedOutLiveData() {
        return loggedOutLiveData;
    }
}
