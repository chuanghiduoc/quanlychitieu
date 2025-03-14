package com.example.quanlychitieu.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.quanlychitieu.MainActivity;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.databinding.ActivityLoginBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private static final String TAG = "LoginActivity";
    private FirebaseFirestore db;
    private static final int RC_SIGN_IN = 9001;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ViewModel
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up click listeners
        binding.btnLogin.setOnClickListener(v -> loginWithEmailPassword());
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.tvRegister.setOnClickListener(v -> navigateToRegister());
        binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Observe ViewModel LiveData
        observeViewModel();
    }

    private void observeViewModel() {
        // Observe user authentication state
        authViewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                navigateToMainScreen();
            }
        });

        // Observe password reset progress
        authViewModel.getResetPasswordInProgress().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                showLoading(true);
            }
        });

        // Observe password reset success
        authViewModel.getResetPasswordSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) {
                showLoading(false);
                String email = binding.etEmail.getText().toString().trim();
                Toast.makeText(this,
                        "Email đặt lại mật khẩu đã được gửi đến " + email,
                        Toast.LENGTH_LONG).show();
                authViewModel.clearPasswordResetState();
            }
        });

        // Observe password reset error
        authViewModel.getResetPasswordError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showLoading(false);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                authViewModel.clearPasswordResetState();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainScreen();
        }
    }

    private void loginWithEmailPassword() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email không được để trống");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Mật khẩu không được để trống");
            return;
        }

        // Clear errors
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        // Show loading
        showLoading(true);

        // Sign in with email and password
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithEmail:success");
                        navigateToMainScreen();
                    } else {
                        // If sign in fails, display a message to the user
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(this, "Đăng nhập thất bại: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    private void signInWithGoogle() {
        // Clear previous sign in
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            // Show loading
            showLoading(true);

            // Start Google sign in flow
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Đăng nhập Google thất bại: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Check if this is a new user
                            boolean isNewUser = task.getResult().getAdditionalUserInfo() != null &&
                                    task.getResult().getAdditionalUserInfo().isNewUser();

                            if (isNewUser) {
                                // Save user data to Firestore
                                saveUserDataToFirestore(user);
                            } else {
                                // Navigate to main screen directly
                                navigateToMainScreen();
                            }
                        }
                    } else {
                        // If sign in fails, display a message to the user
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(this, "Đăng nhập Google thất bại: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    private void saveUserDataToFirestore(FirebaseUser user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("displayName", user.getDisplayName());
        userData.put("email", user.getEmail());
        userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        userData.put("createdAt", System.currentTimeMillis());

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved to Firestore");
                    navigateToMainScreen();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving user data", e);
                    // Still navigate to main screen even if saving data fails
                    navigateToMainScreen();
                });
    }

    private void handleForgotPassword() {
        String email = binding.etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Vui lòng nhập email để đặt lại mật khẩu");
            return;
        }

        // Clear error
        binding.tilEmail.setError(null);

        // Use ViewModel to handle password reset
        authViewModel.sendPasswordResetEmail(email);
    }

    private void navigateToMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish(); // Close LoginActivity
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnGoogleSignIn.setEnabled(!isLoading);
        binding.tvRegister.setEnabled(!isLoading);
        binding.tvForgotPassword.setEnabled(!isLoading);
        binding.etEmail.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
    }
}