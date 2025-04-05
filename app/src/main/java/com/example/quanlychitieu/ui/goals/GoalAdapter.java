package com.example.quanlychitieu.ui.goals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.FinancialGoal;
import com.example.quanlychitieu.databinding.ItemGoalBinding;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    private List<FinancialGoal> goals;
    private final GoalClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public GoalAdapter(List<FinancialGoal> goals, GoalClickListener listener) {
        this.goals = goals;
        this.listener = listener;

        // Định dạng tiền tệ cho Việt Nam
        currencyFormat.setMaximumFractionDigits(0);
    }

    public void updateGoals(List<FinancialGoal> newGoals) {
        this.goals = newGoals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGoalBinding binding = ItemGoalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new GoalViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        FinancialGoal goal = goals.get(position);
        holder.bind(goal);
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    class GoalViewHolder extends RecyclerView.ViewHolder {
        private final ItemGoalBinding binding;

        public GoalViewHolder(ItemGoalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FinancialGoal goal) {
            // Thiết lập dữ liệu cho item
            binding.goalName.setText(goal.getName());
            binding.goalDescription.setText(goal.getDescription());

            // Định dạng số tiền
            String formattedCurrent = currencyFormat.format(goal.getCurrentAmount())
                    .replace("₫", "đ")
                    .replace(",", ".");
            String formattedTarget = currencyFormat.format(goal.getTargetAmount())
                    .replace("₫", "đ")
                    .replace(",", ".");

            binding.goalCurrentAmount.setText(formattedCurrent);
            binding.goalTargetAmount.setText("/ " + formattedTarget);

            // Thiết lập thanh tiến độ
            int progress = goal.getProgressPercentage();
            binding.goalProgress.setProgress(progress);
            binding.goalProgressText.setText(progress + "% hoàn thành");

            // Định dạng ngày tháng
            String dateRange = dateFormat.format(goal.getStartDate()) + " - " +
                    dateFormat.format(goal.getEndDate());
            binding.goalDateRange.setText(dateRange);

            // Thiết lập trạng thái
            if (goal.isCompleted()) {
                binding.goalStatus.setText("Hoàn thành");
                binding.goalStatus.setBackgroundResource(R.drawable.bg_status_completed);
            } else {
                binding.goalStatus.setText("Đang tiến hành");
                binding.goalStatus.setBackgroundResource(R.drawable.bg_status_in_progress);
            }

            // Thiết lập sự kiện click
            binding.getRoot().setOnClickListener(v -> listener.onGoalClick(goal));
            binding.btnContribute.setOnClickListener(v -> listener.onContributeClick(goal));
            binding.btnDetails.setOnClickListener(v -> listener.onGoalClick(goal));

            // Ẩn nút đóng góp nếu mục tiêu đã hoàn thành
            if (goal.isCompleted()) {
                binding.btnContribute.setVisibility(View.GONE);
            } else {
                binding.btnContribute.setVisibility(View.VISIBLE);
            }
        }
    }

    public interface GoalClickListener {
        void onGoalClick(FinancialGoal goal);
        void onContributeClick(FinancialGoal goal);
    }
}
