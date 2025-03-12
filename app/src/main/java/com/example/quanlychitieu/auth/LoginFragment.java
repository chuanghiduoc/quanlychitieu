package com.example.quanlychitieu.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.databinding.LoginFragmentBinding;
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

public class LoginFragment extends Fragment {

    private LoginFragmentBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private static final String TAG = "LoginFragment";
    private NavController navController;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == -1) { // RESULT_OK
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) {
                            firebaseAuthWithGoogle(account.getIdToken());
                        }
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(requireContext(), "Đăng nhập Google thất bại: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                } else {
                    showLoading(false);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = LoginFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // Set up click listeners
        binding.btnLogin.setOnClickListener(v -> loginWithEmailPassword());
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.tvRegister.setOnClickListener(v ->
                navController.navigate(R.id.action_loginFragment_to_registerFragment));
        binding.tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
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
                        Toast.makeText(requireContext(), "Đăng nhập thất bại: " +
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
            googleSignInLauncher.launch(signInIntent);
        });
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
                        Toast.makeText(requireContext(), "Đăng nhập Google thất bại: " +
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

        // Show loading
        showLoading(true);

        // Send password reset email
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(requireContext(),
                                "Email đặt lại mật khẩu đã được gửi đến " + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(),
                                "Không thể gửi email đặt lại mật khẩu: " +
                                        (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void navigateToMainScreen() {
        // Navigate to the main dashboard
        navController.navigate(R.id.action_loginFragment_to_navigation_dashboard);
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

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in and update UI accordingly
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, navigate to main screen
            navigateToMainScreen();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
