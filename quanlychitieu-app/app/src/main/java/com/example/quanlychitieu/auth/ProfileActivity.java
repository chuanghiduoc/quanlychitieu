package com.example.quanlychitieu.auth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.quanlychitieu.MainActivity;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.databinding.ActivityProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private boolean isGoogleAccount = false;
    private static final String TAG = "ProfileActivity";
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        Glide.with(this)
                                .load(selectedImageUri)
                                .placeholder(R.drawable.ic_profile_placeholder)
                                .into(binding.ivUserProfile);
                    }
                }
            }
    );

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Handle back button
        binding.btnBack.setOnClickListener(v -> finish());

        // Get current user
        currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            binding.tvUserName.setText(currentUser.getDisplayName());
            binding.tvUserEmail.setText(currentUser.getEmail());

            // Kiểm tra nếu là tài khoản Google
            for (UserInfo userInfo : currentUser.getProviderData()) {
                if ("google.com".equals(userInfo.getProviderId())) {
                    isGoogleAccount = true;
                    break;
                }
            }

            // Ẩn tùy chọn đổi mật khẩu nếu là tài khoản Google
            if (isGoogleAccount) {
                binding.tvChangePassword.setVisibility(View.GONE);
            }

            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(binding.ivUserProfile);
            }
        } else {
            // User not logged in, navigate to login
            navigateToLogin();
        }

        // Xử lý sự kiện click vào ảnh đại diện để thay đổi
        binding.ivUserProfile.setOnClickListener(v -> openImagePicker());

        // Xử lý sự kiện click vào các tùy chọn
        binding.tvEditProfile.setOnClickListener(v -> openEditProfileDialog());
        binding.tvChangePassword.setOnClickListener(v -> openChangePasswordDialog());

        // Thêm xử lý sự kiện cho nút quản lý danh mục

        binding.tvLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    // Phương thức mới để chuyển đến màn hình quản lý danh mục
    private void navigateToCategoryManagement() {
        // Tạo Intent để mở MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        // Thêm flag để chỉ định rằng bạn muốn mở màn hình quản lý danh mục
        intent.putExtra("open_category_management", true);
        startActivity(intent);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void openEditProfileDialog() {
        EditProfileDialogFragment dialogFragment = new EditProfileDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "EditProfileDialog");
    }

    private void openChangePasswordDialog() {
        ChangePasswordDialogFragment dialogFragment = new ChangePasswordDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), "ChangePasswordDialog");
    }

    private void showLogoutConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> logOut())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void logOut() {
        auth.signOut();
        Toast.makeText(this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish(); // Close ProfileActivity
    }

    // Phương thức cập nhật thông tin người dùng
    public void updateUserProfile(String displayName) {
        if (currentUser == null) return;

        // Hiển thị loading
        showLoading(true);

        // Nếu có ảnh mới được chọn, tải lên Firebase Storage
        if (selectedImageUri != null) {
            uploadImageAndUpdateProfile(displayName);
        } else {
            // Nếu không có ảnh mới, chỉ cập nhật tên hiển thị
            updateProfileWithoutImage(displayName);
        }
    }

    private void uploadImageAndUpdateProfile(String displayName) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_images")
                .child(currentUser.getUid() + "_" + System.currentTimeMillis() + ".jpg");

        storageRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .setPhotoUri(uri)
                                .build();

                        updateUserProfileInFirebase(profileUpdates);
                    });
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(ProfileActivity.this, "Lỗi khi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfileWithoutImage(String displayName) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build();

        updateUserProfileInFirebase(profileUpdates);
    }

    private void updateUserProfileInFirebase(UserProfileChangeRequest profileUpdates) {
        currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        // Cập nhật UI
                        binding.tvUserName.setText(currentUser.getDisplayName());
                        if (currentUser.getPhotoUrl() != null) {
                            Glide.with(ProfileActivity.this)
                                    .load(currentUser.getPhotoUrl())
                                    .placeholder(R.drawable.ic_profile_placeholder)
                                    .into(binding.ivUserProfile);
                        }
                        Toast.makeText(ProfileActivity.this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Phương thức đổi mật khẩu
    public void changePassword(String currentPassword, String newPassword) {
        if (currentUser == null || isGoogleAccount) return;

        // Hiển thị loading
        showLoading(true);

        // Xác thực lại người dùng trước khi đổi mật khẩu
        String email = currentUser.getEmail();
        if (email == null) {
            showLoading(false);
            Toast.makeText(this, "Không thể xác định email người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xác thực lại với mật khẩu hiện tại
        auth.signInWithEmailAndPassword(email, currentPassword)
                .addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        // Xác thực thành công, tiến hành đổi mật khẩu
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(task -> {
                                    showLoading(false);
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ProfileActivity.this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ProfileActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        showLoading(false);
                        Toast.makeText(ProfileActivity.this, "Mật khẩu hiện tại không đúng!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        // Thêm logic hiển thị loading nếu cần
        // Ví dụ: binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
