package com.example.quanlychitieu.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.CategoryManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BudgetCategoryAdapter extends RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder> {

    private final List<CategoryBudgetItem> items = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final Context context;
    private final int[] chartColors;

    public BudgetCategoryAdapter(Context context) {
        this.context = context;
        currencyFormat.setMaximumFractionDigits(0);

        // Lấy màu từ resources
        chartColors = new int[] {
                ContextCompat.getColor(context, R.color.chart_1),
                ContextCompat.getColor(context, R.color.chart_2),
                ContextCompat.getColor(context, R.color.chart_3),
                ContextCompat.getColor(context, R.color.chart_4),
                ContextCompat.getColor(context, R.color.chart_5),
                ContextCompat.getColor(context, R.color.chart_6),
                ContextCompat.getColor(context, R.color.chart_7),
                ContextCompat.getColor(context, R.color.chart_8)
        };
    }

    public void updateData(Map<String, Double> expenses, Map<String, Double> budgets) {
        items.clear();

        // Lấy tất cả các danh mục chi tiêu
        List<String> categories = CategoryManager.getInstance().getExpenseCategories();

        int colorIndex = 0;
        for (String category : categories) {
            // Lấy giá trị chi tiêu và ngân sách cho danh mục
            double spent = expenses.getOrDefault(category, 0.0);
            double budget = budgets.getOrDefault(category, 0.0);

            // Tính phần trăm chính xác, không giới hạn ở 100%
            int percentage = 0;
            if (budget > 0) {
                percentage = (int) Math.round((spent / budget) * 100);
            }

            // Lấy màu cho danh mục
            int color = chartColors[colorIndex % chartColors.length];

            // Thêm vào danh sách
            items.add(new CategoryBudgetItem(category, spent, budget, percentage, color));

            colorIndex++;
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_budget_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryBudgetItem item = items.get(position);

        // Hiển thị tên danh mục
        holder.categoryName.setText(item.category);

        // Hiển thị màu chỉ báo
        holder.colorIndicator.setBackgroundColor(item.color);

        // Format và hiển thị số tiền
        String formattedSpent = formatCurrency(item.spent);
        String formattedBudget = formatCurrency(item.budget);

        // Hiển thị theo định dạng "đã chi / ngân sách (phần trăm)"
        String amountText = formattedSpent + " / " + formattedBudget + " (" + item.percentage + "%)";
        holder.amountText.setText(amountText);

        // Cập nhật thanh tiến trình - giới hạn ở 100% chỉ cho UI
        holder.progressBar.setProgress(Math.min(100, item.percentage));

        // Đổi màu thanh tiến trình dựa trên phần trăm
        int progressColor;
        if (item.percentage >= 100) {
            progressColor = ContextCompat.getColor(context, R.color.expense_red);
        } else if (item.percentage >= 80) {
            progressColor = ContextCompat.getColor(context, R.color.chart_2);
        } else {
            progressColor = item.color;
        }
        holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(progressColor));

        // Đổi màu chữ dựa trên phần trăm
        if (item.percentage >= 100) {
            holder.amountText.setTextColor(ContextCompat.getColor(context, R.color.expense_red));
        } else if (item.percentage >= 80) {
            holder.amountText.setTextColor(ContextCompat.getColor(context, R.color.chart_2));
        } else {
            holder.amountText.setTextColor(ContextCompat.getColor(context, R.color.black));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatCurrency(double amount) {
        return currencyFormat.format(amount)
                .replace("₫", "đ")
                .replace(",", ".");
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View colorIndicator;
        TextView categoryName;
        TextView amountText;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
            categoryName = itemView.findViewById(R.id.category_name);
            amountText = itemView.findViewById(R.id.amount_text);
            progressBar = itemView.findViewById(R.id.progress_bar);
        }
    }

    static class CategoryBudgetItem {
        String category;
        double spent;
        double budget;
        int percentage;
        int color;

        public CategoryBudgetItem(String category, double spent, double budget, int percentage, int color) {
            this.category = category;
            this.spent = spent;
            this.budget = budget;
            this.percentage = percentage;
            this.color = color;
        }
    }
}
