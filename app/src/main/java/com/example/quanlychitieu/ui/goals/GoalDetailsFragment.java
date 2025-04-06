package com.example.quanlychitieu.ui.goals;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.FinancialGoal;
import com.example.quanlychitieu.databinding.FragmentGoalDetailsBinding;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class GoalDetailsFragment extends Fragment {

    private FragmentGoalDetailsBinding binding;
    private GoalsViewModel viewModel;
    private String goalId;
    private FinancialGoal currentGoal;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    public void onResume() {
        super.onResume();
        if (goalId != null) {
            loadGoalDetails(goalId);
        }
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGoalDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(GoalsViewModel.class);

        // Định dạng tiền tệ
        currencyFormat.setMaximumFractionDigits(0);

        // Lấy ID mục tiêu từ arguments
        if (getArguments() != null) {
            goalId = getArguments().getString("goal_id");
            if (goalId != null) {
                loadGoalDetails(goalId);
            }
        }

        // Thiết lập các nút
        setupButtons();
        setupToolbar();
    }

    private void loadGoalDetails(String id) {
        viewModel.getGoalById(id).observe(getViewLifecycleOwner(), goal -> {
            if (goal != null) {
                currentGoal = goal;
                updateUI(goal);
            }
        });
    }

    private void updateUI(FinancialGoal goal) {
        // Thiết lập tên và mô tả
        binding.goalName.setText(goal.getName());
        binding.goalDescription.setText(goal.getDescription());

        // Định dạng số tiền
        String formattedCurrent = currencyFormat.format(goal.getCurrentAmount())
                .replace("₫", "đ")
                .replace(",", ".");
        String formattedTarget = currencyFormat.format(goal.getTargetAmount())
                .replace("₫", "đ")
                .replace(",", ".");
        String formattedRemaining = currencyFormat.format(goal.getRemainingAmount())
                .replace("₫", "đ")
                .replace(",", ".");

        binding.currentAmount.setText(formattedCurrent);
        binding.targetAmount.setText(formattedTarget);
        binding.remainingAmount.setText(formattedRemaining);

        // Thiết lập thanh tiến độ
        int progress = goal.getProgressPercentage();
        binding.goalProgress.setProgress(progress);
        binding.progressPercentage.setText(progress + "%");

        // Định dạng ngày tháng
        binding.startDate.setText(dateFormat.format(goal.getStartDate()));
        binding.endDate.setText(dateFormat.format(goal.getEndDate()));

        // Tính toán thời gian còn lại
        long daysRemaining = calculateDaysRemaining(goal.getEndDate());
        binding.timeRemaining.setText(formatDaysRemaining(daysRemaining));

        // Thiết lập trạng thái
        if (goal.isCompleted()) {
            binding.goalStatus.setText("Hoàn thành");
            binding.goalStatus.setBackgroundResource(R.drawable.bg_status_completed);
            binding.contributeButton.setVisibility(View.GONE);
        } else {
            binding.goalStatus.setText("Đang tiến hành");
            binding.goalStatus.setBackgroundResource(R.drawable.bg_status_in_progress);
            binding.contributeButton.setVisibility(View.VISIBLE);
        }

        // Hiển thị danh mục nếu có
        if (goal.getCategory() != null && !goal.getCategory().isEmpty()) {
            binding.categoryLabel.setVisibility(View.VISIBLE);
            binding.category.setVisibility(View.VISIBLE);
            binding.category.setText(goal.getCategory());
        } else {
            binding.categoryLabel.setVisibility(View.GONE);
            binding.category.setVisibility(View.GONE);
        }
    }

    private long calculateDaysRemaining(Date endDate) {
        Date today = new Date();
        long diffInMillis = endDate.getTime() - today.getTime();
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);
    }

    private String formatDaysRemaining(long days) {
        if (days < 0) {
            return "Đã kết thúc";
        } else if (days == 0) {
            return "Hôm nay là hạn chót";
        } else {
            return "Còn " + days + " ngày";
        }
    }

    private void setupButtons() {
        // Nút đóng góp
        binding.contributeButton.setOnClickListener(v -> {
            showContributeDialog();
        });

        // Nút chỉnh sửa
        binding.editButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("goal_id", goalId);
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_goalDetailsFragment_to_addEditGoalFragment, args);
        });

        // Nút xóa
        binding.deleteButton.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });
    }

    private void showContributeDialog() {
        ContributeGoalDialogFragment dialog = ContributeGoalDialogFragment.newInstance(goalId);
        dialog.show(getChildFragmentManager(), "ContributeDialog");
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa mục tiêu")
                .setMessage("Bạn có chắc chắn muốn xóa mục tiêu này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteGoal();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    public void refreshGoalDetails() {
        if (goalId != null) {
            loadGoalDetails(goalId);
        }
    }
    private void deleteGoal() {
        viewModel.deleteGoal(goalId);
        Navigation.findNavController(requireView()).popBackStack();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
