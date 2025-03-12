package com.example.quanlychitieu.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.databinding.ProfileFragmentBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private ProfileFragmentBinding binding;
    private AuthViewModel authViewModel;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = ProfileFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(view);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        binding.btnBack.setOnClickListener(v -> navController.navigateUp());

        // Lấy thông tin người dùng hiện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            binding.tvUserName.setText(currentUser.getDisplayName());
            binding.tvUserEmail.setText(currentUser.getEmail());

            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(binding.ivUserProfile);
            }
        } else {
            // Người dùng không đăng nhập, điều hướng về màn hình đăng nhập
            navController.navigate(R.id.action_profileFragment_to_loginFragment);
        }

        // Xử lý sự kiện click
        binding.tvLogout.setOnClickListener(v -> showLogoutConfirmationDialog());

        binding.tvEditProfile.setOnClickListener(v -> {
            // TODO: Chuyển đến màn hình chỉnh sửa thông tin cá nhân
        });

        binding.tvChangePassword.setOnClickListener(v -> {
            // TODO: Chuyển đến màn hình đổi mật khẩu
        });

        // Theo dõi trạng thái đăng xuất
        authViewModel.getLoggedOutLiveData().observe(getViewLifecycleOwner(), loggedOut -> {
            if (loggedOut) {
                // Chuyển đến màn hình đăng nhập
                navController.navigate(R.id.action_profileFragment_to_loginFragment);
            }
        });

    }

    private void showLogoutConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    // Thực hiện đăng xuất
                    logOut();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void logOut() {
        authViewModel.logOut();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
