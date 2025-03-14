package com.example.quanlychitieu.auth;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.quanlychitieu.databinding.DialogChangePasswordBinding;

public class ChangePasswordDialogFragment extends DialogFragment {

    private DialogChangePasswordBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_MinWidth);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogChangePasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Thiết lập tiêu đề
        getDialog().setTitle("Đổi mật khẩu");

        // Xử lý nút Hủy
        binding.btnCancel.setOnClickListener(v -> dismiss());

        // Xử lý nút Đổi mật khẩu
        binding.btnChangePassword.setOnClickListener(v -> {
            String currentPassword = binding.etCurrentPassword.getText().toString().trim();
            String newPassword = binding.etNewPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

            // Kiểm tra đầu vào
            if (TextUtils.isEmpty(currentPassword)) {
                binding.etCurrentPassword.setError("Vui lòng nhập mật khẩu hiện tại");
                return;
            }

            if (TextUtils.isEmpty(newPassword)) {
                binding.etNewPassword.setError("Vui lòng nhập mật khẩu mới");
                return;
            }

            if (newPassword.length() < 6) {
                binding.etNewPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                binding.etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
                return;
            }

            // Thực hiện đổi mật khẩu
            if (getActivity() instanceof ProfileActivity) {
                ((ProfileActivity) getActivity()).changePassword(currentPassword, newPassword);
                dismiss();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
