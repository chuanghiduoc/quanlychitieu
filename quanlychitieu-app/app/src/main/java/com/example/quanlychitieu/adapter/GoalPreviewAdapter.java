package com.example.quanlychitieu.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.data.model.FinancialGoal;
import com.example.quanlychitieu.databinding.ItemGoalPreviewBinding;

import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class GoalPreviewAdapter extends RecyclerView.Adapter<GoalPreviewAdapter.GoalPreviewViewHolder> {

    private List<FinancialGoal> goals;
    private final GoalClickListener listener;
    private final NumberFormat currencyFormat;

    public GoalPreviewAdapter(List<FinancialGoal> goals, GoalClickListener listener) {
        this.goals = goals;
        this.listener = listener;
        this.currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    public void updateGoals(List<FinancialGoal> newGoals) {
        this.goals = newGoals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GoalPreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGoalPreviewBinding binding = ItemGoalPreviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new GoalPreviewViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalPreviewViewHolder holder, int position) {
        holder.bind(goals.get(position));
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    class GoalPreviewViewHolder extends RecyclerView.ViewHolder {
        private final ItemGoalPreviewBinding binding;

        public GoalPreviewViewHolder(ItemGoalPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FinancialGoal goal) {
            binding.goalName.setText(goal.getName());

            // Định dạng số tiền hiện tại và mục tiêu (rút gọn)
            String currentAmount = formatCompactAmount(goal.getCurrentAmount());
            String targetAmount = formatCompactAmount(goal.getTargetAmount());

            binding.goalCurrentAmount.setText(currentAmount);
            binding.goalTargetAmount.setText("/" + targetAmount);

            // Thiết lập tiến độ
            binding.goalProgress.setProgress(goal.getProgressPercentage());

            // Tính và hiển thị thời gian còn lại
            binding.goalDeadline.setText(formatDaysRemaining(goal.getEndDate()));

            // Xử lý sự kiện click
            binding.getRoot().setOnClickListener(v -> listener.onGoalClick(goal));
        }

        private String formatCompactAmount(double amount) {
            if (amount >= 1_000_000_000) {
                return String.format("%.1fB", amount / 1_000_000_000);
            } else if (amount >= 1_000_000) {
                return String.format("%.1fM", amount / 1_000_000);
            } else if (amount >= 1_000) {
                return String.format("%.1fK", amount / 1_000);
            } else {
                return currencyFormat.format(amount);
            }
        }

        private String formatDaysRemaining(Date endDate) {
            Date today = new Date();
            long diffInMillis = endDate.getTime() - today.getTime();
            long days = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS);

            if (days < 0) {
                return "Đã kết thúc";
            } else if (days == 0) {
                return "Hôm nay";
            } else {
                return "Còn " + days + " ngày";
            }
        }
    }

    public interface GoalClickListener {
        void onGoalClick(FinancialGoal goal);
    }
}
