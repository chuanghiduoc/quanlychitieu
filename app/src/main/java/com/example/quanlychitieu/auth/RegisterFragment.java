package com.example.quanlychitieu.auth;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
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
import com.example.quanlychitieu.databinding.RegisterFragmentBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterFragment extends Fragment {

    private RegisterFragmentBinding binding;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private static final String TAG = "RegisterFragment";
    private NavController navController;
    private FirebaseFirestore db;
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;

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
        binding = RegisterFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Đảm bảo reCAPTCHA verification đã bị tắt
        auth.getFirebaseAuthSettings().setAppVerificationDisabledForTesting(true);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // Set up click listeners
        binding.btnRegister.setOnClickListener(v -> registerWithEmailPassword());
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.tvLogin.setOnClickListener(v -> navController.navigateUp());
        binding.ivBack.setOnClickListener(v -> navController.navigateUp());

        // Khởi tạo timeout handler
        timeoutHandler = new Handler(Looper.getMainLooper());
    }

    private void registerWithEmailPassword() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(name)) {
            binding.tilName.setError("Họ và tên không được để trống");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError("Email không được để trống");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Email không hợp lệ");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError("Mật khẩu không được để trống");
            return;
        }

        if (password.length() < 6) {
            binding.tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            binding.tilConfirmPassword.setError("Vui lòng xác nhận mật khẩu");
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Mật khẩu không khớp");
            return;
        }

        // Kiểm tra kết nối mạng
        if (!isNetworkAvailable()) {
            Toast.makeText(requireContext(),
                    "Không có kết nối mạng. Vui lòng kiểm tra kết nối và thử lại.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Clear errors
        binding.tilName.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        // Show loading
        showLoading(true);

        // Thiết lập timeout
        setupTimeout();


        // Create user with email and password
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    // Hủy timeout
                    cancelTimeout();

                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Update user profile
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "User profile updated");
                                        } else {
                                            Log.w(TAG, "Failed to update user profile", profileTask.getException());
                                        }
                                        // Save user data to Firestore
                                        saveUserDataToFirestore(user, name);
                                    });
                        }
                    } else {
                        // Xử lý lỗi chi tiết
                        String errorMessage = "Lỗi không xác định";
                        Exception exception = task.getException();

                        if (exception != null) {
                            Log.e(TAG, "createUserWithEmail:failure", exception);

                            if (exception instanceof FirebaseAuthWeakPasswordException) {
                                errorMessage = "Mật khẩu quá yếu, vui lòng chọn mật khẩu mạnh hơn";
                                binding.tilPassword.setError(errorMessage);
                            } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                errorMessage = "Email không hợp lệ";
                                binding.tilEmail.setError(errorMessage);
                            } else if (exception instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "Email này đã được sử dụng bởi tài khoản khác";
                                binding.tilEmail.setError(errorMessage);
                            } else {
                                errorMessage = exception.getMessage();
                            }
                        }

                        final String finalErrorMessage = errorMessage;
                        Toast.makeText(requireContext(), "Đăng ký thất bại: " + finalErrorMessage,
                                Toast.LENGTH_LONG).show();
                        showLoading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    // Hủy timeout
                    cancelTimeout();

                    Log.e(TAG, "Registration failed with exception", e);
                    Toast.makeText(requireContext(), "Đăng ký thất bại: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    showLoading(false);
                });
    }

    private void setupTimeout() {
        // Hủy timeout cũ nếu có
        cancelTimeout();

        // Tạo timeout mới
        timeoutRunnable = () -> {
            if (binding != null) {
                Toast.makeText(requireContext(),
                        "Kết nối đến máy chủ quá lâu, vui lòng thử lại sau",
                        Toast.LENGTH_LONG).show();
                showLoading(false);
            }
        };

        // Đặt timeout 20 giây
        timeoutHandler.postDelayed(timeoutRunnable, 20000);
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) requireContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
        return false;
    }

    private void signInWithGoogle() {
        // Clear previous sign in
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            // Show loading
            showLoading(true);

            // Thiết lập timeout
            setupTimeout();

            // Start Google sign in flow
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    // Hủy timeout
                    cancelTimeout();

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
                                saveUserDataToFirestore(user, user.getDisplayName());
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
                })
                .addOnFailureListener(e -> {
                    // Hủy timeout
                    cancelTimeout();

                    Log.e(TAG, "Google sign in failed with exception", e);
                    Toast.makeText(requireContext(), "Đăng nhập Google thất bại: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void saveUserDataToFirestore(FirebaseUser user, String name) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("displayName", name);
        userData.put("email", user.getEmail());
        userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        userData.put("createdAt", System.currentTimeMillis());

        Log.d(TAG, "Saving user data to Firestore for user: " + user.getUid());

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data saved to Firestore");
                    Toast.makeText(requireContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    navigateToMainScreen();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving user data", e);
                    // Still navigate to main screen even if saving data fails
                    Toast.makeText(requireContext(),
                            "Đăng ký thành công nhưng không thể lưu thông tin người dùng",
                            Toast.LENGTH_SHORT).show();
                    navigateToMainScreen();
                });
    }

    private void navigateToMainScreen() {
        // Navigate to the main dashboard
        navController.navigate(R.id.action_registerFragment_to_navigation_dashboard);
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnRegister.setEnabled(!isLoading);
        binding.btnGoogleSignIn.setEnabled(!isLoading);
        binding.tvLogin.setEnabled(!isLoading);
        binding.ivBack.setEnabled(!isLoading);
        binding.etName.setEnabled(!isLoading);
        binding.etEmail.setEnabled(!isLoading);
        binding.etPassword.setEnabled(!isLoading);
        binding.etConfirmPassword.setEnabled(!isLoading);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Đảm bảo hủy timeout khi fragment bị hủy
        cancelTimeout();
        binding = null;
    }
}
