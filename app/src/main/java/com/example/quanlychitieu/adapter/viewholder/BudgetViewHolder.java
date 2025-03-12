package com.example.quanlychitieu.adapter.viewholder;

import android.graphics.Color;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.BudgetAdapter;
import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.databinding.ItemBudgetBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class BudgetViewHolder extends RecyclerView.ViewHolder {

    private final ItemBudgetBinding binding;
    private final BudgetAdapter.BudgetClickListener clickListener;
    private final NumberFormat currencyFormat;

    public BudgetViewHolder(ItemBudgetBinding binding, BudgetAdapter.BudgetClickListener clickListener) {
        super(binding.getRoot());
        this.binding = binding;
        this.clickListener = clickListener;
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    }

    public void bind(Budget budget) {
        // Set category name
        binding.categoryName.setText(budget.getCategory());

        // Set category icon based on category name
        setCategoryIcon(budget.getCategory());

        // Format and set amounts
        String formattedSpent = formatCurrency(budget.getSpent());
        String formattedAmount = formatCurrency(budget.getAmount());
        binding.budgetSpent.setText(String.format("%s / %s", formattedSpent, formattedAmount));

        // Set progress
        int progressPercentage = budget.getProgressPercentage();
        binding.budgetProgress.setProgress(progressPercentage);

        // Set percentage text with appropriate color
        binding.budgetPercentage.setText(String.format("%d%%", progressPercentage));

        // Set color based on percentage
        if (progressPercentage > 90) {
            binding.budgetPercentage.setTextColor(Color.parseColor("#F44336")); // Red
            binding.budgetProgress.setIndicatorColor(Color.parseColor("#F44336")); // Red
        } else if (progressPercentage > 75) {
            binding.budgetPercentage.setTextColor(Color.parseColor("#FF9800")); // Orange
            binding.budgetProgress.setIndicatorColor(Color.parseColor("#FF9800")); // Orange
        } else {
            binding.budgetPercentage.setTextColor(Color.parseColor("#4CAF50")); // Green
            binding.budgetProgress.setIndicatorColor(Color.parseColor("#4CAF50")); // Green
        }

        // Set edit button click listener
        binding.editBudgetButton.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onBudgetClick(budget);
            }
        });
    }

    private void setCategoryIcon(String category) {
        // Set the appropriate icon based on the category
        int iconResId;

        switch (category.toLowerCase()) {
            case "ăn uống":
                iconResId = R.drawable.ic_food;
                break;
            case "di chuyển":
                iconResId = R.drawable.ic_move;
                break;
            case "mua sắm":
                iconResId = R.drawable.ic_shopping;
                break;
            case "hóa đơn":
                iconResId = R.drawable.ic_invoice;
                break;
            default:
                iconResId = R.drawable.ic_more;
                break;
        }

        binding.categoryIcon.setImageResource(iconResId);
    }

    private String formatCurrency(double amount) {
        return currencyFormat.format(amount)
                .replace("₫", "đ")
                .replace(",", ".");
    }
}
