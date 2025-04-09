package com.example.quanlychitieu;

import android.app.Application;
import android.util.Log;
import java.util.Locale;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class QuanLyChiTieuApplication extends Application {
    private static final String TAG = "QuanLyChiTieuApp";

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Khởi tạo Firebase
            FirebaseApp.initializeApp(this);

            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.useAppLanguage();

            // Tắt xác minh reCAPTCHA cho môi trường phát triển
            auth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
    }
}
