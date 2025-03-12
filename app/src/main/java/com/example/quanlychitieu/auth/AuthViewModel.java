package com.example.quanlychitieu.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends AndroidViewModel {

    private final FirebaseAuth auth;
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<Boolean> loggedOutLiveData;

    public AuthViewModel(@NonNull Application application) {
        super(application);

        auth = FirebaseAuth.getInstance();
        userLiveData = new MutableLiveData<>();
        loggedOutLiveData = new MutableLiveData<>();

        if (auth.getCurrentUser() != null) {
            userLiveData.postValue(auth.getCurrentUser());
            loggedOutLiveData.postValue(false);
        }
    }

    public void logOut() {
        auth.signOut();
        loggedOutLiveData.postValue(true);
    }

    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getLoggedOutLiveData() {
        return loggedOutLiveData;
    }
}
