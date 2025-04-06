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

    private static final String TAG = "AuthViewModel";
    private final FirebaseAuth auth;
    private final MutableLiveData<FirebaseUser> userLiveData;
    private final MutableLiveData<Boolean> loggedOutLiveData;
    private final GoogleSignInClient googleSignInClient;

    // LiveData for password reset process
    private final MutableLiveData<Boolean> resetPasswordInProgress;
    private final MutableLiveData<Boolean> resetPasswordSuccess;
    private final MutableLiveData<String> resetPasswordError;

    public AuthViewModel(@NonNull Application application) {
        super(application);

        auth = FirebaseAuth.getInstance();
        userLiveData = new MutableLiveData<>();
        loggedOutLiveData = new MutableLiveData<>();

        // Initialize password reset LiveData
        resetPasswordInProgress = new MutableLiveData<>(false);
        resetPasswordSuccess = new MutableLiveData<>(false);
        resetPasswordError = new MutableLiveData<>();

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
                        Log.d(TAG, "Successfully signed out from Google.");
                    } else {
                        Log.e(TAG, "Failed to sign out from Google.", task.getException());
                    }
                });

        // Update logout state
        userLiveData.postValue(null);
        loggedOutLiveData.postValue(true);
    }

    /**
     * Gửi email đặt lại mật khẩu đến địa chỉ email được chỉ định
     *
     * @param email Địa chỉ email để gửi liên kết đặt lại mật khẩu tới
     */
    public void sendPasswordResetEmail(String email) {
        if (email == null || email.isEmpty()) {
            resetPasswordError.postValue("Email không được để trống");
            return;
        }

        // Set loading state
        resetPasswordInProgress.postValue(true);
        resetPasswordSuccess.postValue(false);
        resetPasswordError.postValue(null);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    resetPasswordInProgress.postValue(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent successfully to: " + email);
                        resetPasswordSuccess.postValue(true);
                    } else {
                        Log.w(TAG, "Failed to send password reset email", task.getException());
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Lỗi không xác định";
                        resetPasswordError.postValue("Không thể gửi email đặt lại mật khẩu: " + errorMessage);
                    }
                });
    }

    // LiveData getters
    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getLoggedOutLiveData() {
        return loggedOutLiveData;
    }

    // Password reset LiveData getters
    public LiveData<Boolean> getResetPasswordInProgress() {
        return resetPasswordInProgress;
    }

    public LiveData<Boolean> getResetPasswordSuccess() {
        return resetPasswordSuccess;
    }

    public LiveData<String> getResetPasswordError() {
        return resetPasswordError;
    }

    /**
     * Đặt lại trạng thái đặt lại mật khẩu
     * Gọi phương thức này sau khi xử lý thông báo thành công hoặc lỗi
     */
    public void clearPasswordResetState() {
        resetPasswordInProgress.postValue(false);
        resetPasswordSuccess.postValue(false);
        resetPasswordError.postValue(null);
    }
}