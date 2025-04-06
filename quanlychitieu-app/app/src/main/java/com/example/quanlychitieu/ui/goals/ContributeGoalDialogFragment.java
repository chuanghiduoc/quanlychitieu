package com.example.quanlychitieu.ui.goals;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.FinancialGoal;
import com.example.quanlychitieu.databinding.DialogContributeGoalBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class ContributeGoalDialogFragment extends DialogFragment {

    private String goalId;
    private GoalsViewModel viewModel;
    private DialogContributeGoalBinding binding;
    private FinancialGoal currentGoal;

    public static ContributeGoalDialogFragment newInstance(String goalId) {
        ContributeGoalDialogFragment fragment = new ContributeGoalDialogFragment();
        Bundle args = new Bundle();
        args.putString("goal_id", goalId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            goalId = getArguments().getString("goal_id");
        }
        viewModel = new ViewModelProvider(this).get(GoalsViewModel.class);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogContributeGoalBinding.inflate(LayoutInflater.from(getContext()));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle("Đóng góp vào mục tiêu");
        builder.setView(binding.getRoot());
        builder.setPositiveButton("Đóng góp", null); // Sẽ thiết lập sau
        builder.setNegativeButton("Hủy", (dialog, which) -> dismiss());

        // Thiết lập định dạng số tiền
        setupAmountFormatting();

        // Tải thông tin mục tiêu
        loadGoalInfo();

        Dialog dialog = builder.create();

        // Thiết lập sự kiện cho nút đóng góp sau khi dialog được tạo
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = ((androidx.appcompat.app.AlertDialog) dialog).getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                if (validateAmount()) {
                    contributeToGoal();
                }
            });
        });

        return dialog;
    }

    private void loadGoalInfo() {
        viewModel.getGoalById(goalId).observe(this, goal -> {
            if (goal != null) {
                currentGoal = goal;
                binding.goalName.setText(goal.getName());

                // Định dạng số tiền còn lại
                double remainingAmount = goal.getTargetAmount() - goal.getCurrentAmount();

                NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
                String formattedRemaining = formatter.format(remainingAmount) + " đ";

                binding.goalRemaining.setText("Còn lại: " + formattedRemaining);
            }
        });
    }

    private void setupAmountFormatting() {
        binding.amountInput.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    binding.amountInput.removeTextChangedListener(this);

                    // Remove all non-digit characters
                    String cleanString = s.toString().replaceAll("[^\\d]", "");

                    if (!cleanString.isEmpty()) {
                        try {
                            // Parse to a long (no decimals for VND)
                            long parsed = Long.parseLong(cleanString);

                            // Format with Vietnamese locale
                            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("vi", "VN"));
                            formatter.applyPattern("#,###");
                            formatter.setDecimalSeparatorAlwaysShown(false);
                            formatter.setGroupingUsed(true);
                            formatter.setGroupingSize(3);

                            String formatted = formatter.format(parsed);
                            // Replace comma with dot for Vietnamese display
                            formatted = formatted.replace(",", ".");

                            current = formatted;
                            s.replace(0, s.length(), formatted);
                        } catch (NumberFormatException e) {
                            s.clear();
                        }
                    }

                    binding.amountInput.addTextChangedListener(this);
                }
            }
        });
    }

    private boolean validateAmount() {
        String amountText = binding.amountInput.getText().toString().trim();
        if (amountText.isEmpty()) {
            binding.amountLayout.setError("Vui lòng nhập số tiền");
            return false;
        }

        try {
            // Bỏ định dạng trước khi parse
            String cleanAmount = amountText.replace(".", "");
            double amount = Double.parseDouble(cleanAmount);

            if (amount <= 0) {
                binding.amountLayout.setError("Số tiền phải lớn hơn 0");
                return false;
            }

            if (currentGoal != null) {
                double remainingAmount = currentGoal.getTargetAmount() - currentGoal.getCurrentAmount();
                if (amount > remainingAmount && remainingAmount > 0) {
                    binding.amountLayout.setError("Số tiền không thể lớn hơn số tiền còn lại cần đạt mục tiêu");
                    return false;
                }
            }

            binding.amountLayout.setError(null);
            return true;
        } catch (NumberFormatException e) {
            binding.amountLayout.setError("Số tiền không hợp lệ");
            return false;
        }
    }

    private void contributeToGoal() {
        String amountText = binding.amountInput.getText().toString().trim();
        String cleanAmount = amountText.replace(".", "");
        double amount = Double.parseDouble(cleanAmount);

        viewModel.contributeToGoal(goalId, amount);

        // Force the parent fragment to reload goal details
        if (getParentFragment() instanceof GoalDetailsFragment) {
            ((GoalDetailsFragment) getParentFragment()).refreshGoalDetails();
        }

        Toast.makeText(requireContext(), "Đã đóng góp " + amountText + " đ vào mục tiêu", Toast.LENGTH_SHORT).show();
        dismiss();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
