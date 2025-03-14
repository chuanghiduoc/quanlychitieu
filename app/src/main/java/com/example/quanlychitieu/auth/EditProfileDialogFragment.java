package com.example.quanlychitieu.auth;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.quanlychitieu.databinding.DialogEditProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditProfileDialogFragment extends DialogFragment {

    private DialogEditProfileBinding binding;
    private FirebaseUser currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_MinWidth);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Thiết lập tiêu đề
        getDialog().setTitle("Chỉnh sửa thông tin cá nhân");

        // Thiết lập giá trị hiện tại
        if (currentUser != null && currentUser.getDisplayName() != null) {
            binding.etDisplayName.setText(currentUser.getDisplayName());
        }

        // Xử lý nút Hủy
        binding.btnCancel.setOnClickListener(v -> dismiss());

        // Xử lý nút Lưu
        binding.btnSave.setOnClickListener(v -> {
            String displayName = binding.etDisplayName.getText().toString().trim();
            if (displayName.isEmpty()) {
                binding.etDisplayName.setError("Vui lòng nhập tên hiển thị");
                return;
            }

            // Cập nhật thông tin
            if (getActivity() instanceof ProfileActivity) {
                ((ProfileActivity) getActivity()).updateUserProfile(displayName);
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
