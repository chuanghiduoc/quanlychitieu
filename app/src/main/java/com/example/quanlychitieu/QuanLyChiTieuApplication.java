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
            Log.d(TAG, "Initializing Firebase...");

            // Khởi tạo Firebase
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");

            // Đặt locale cho Firebase
            Locale currentLocale = getResources().getConfiguration().getLocales().get(0);
            Log.d(TAG, "Current locale: " + currentLocale.toString());

            // Tắt xác minh reCAPTCHA một cách rõ ràng
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.useAppLanguage(); // Đặt ngôn ngữ cho Firebase Auth

            // Tắt xác minh reCAPTCHA cho môi trường phát triển
            auth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);
            Log.d(TAG, "App verification disabled for testing");

            Log.d(TAG, "Firebase Auth configured successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
    }
}
