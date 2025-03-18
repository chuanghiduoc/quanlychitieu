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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryStatisticsAdapter extends RecyclerView.Adapter<CategoryStatisticsAdapter.ViewHolder> {

    private final List<CategoryItem> items = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final Context context;
    private final int[] chartColors;
    private double totalAmount = 0;

    public CategoryStatisticsAdapter(Context context) {
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

    public void updateData(Map<String, Double> categoryExpenses) {
        items.clear();

        // Tính tổng chi tiêu
        totalAmount = 0;
        for (Double amount : categoryExpenses.values()) {
            totalAmount += amount;
        }

        // Thêm các mục vào danh sách
        int colorIndex = 0;
        for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
            String category = entry.getKey();
            double amount = entry.getValue();

            // Tính phần trăm
            int percentage = 0;
            if (totalAmount > 0) {
                percentage = (int) Math.round((amount / totalAmount) * 100);
            }

            // Lấy màu cho danh mục
            int color = chartColors[colorIndex % chartColors.length];

            // Thêm vào danh sách
            items.add(new CategoryItem(category, amount, percentage, color));

            colorIndex++;
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_statistics, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryItem item = items.get(position);

        // Hiển thị tên danh mục
        holder.categoryName.setText(item.category);

        // Hiển thị màu chỉ báo
        holder.colorIndicator.setBackgroundColor(item.color);

        // Format và hiển thị số tiền
        String formattedAmount = formatCurrency(item.amount);

        // Hiển thị theo định dạng "số tiền (phần trăm)"
        String amountText = formattedAmount + " (" + item.percentage + "%)";
        holder.amountText.setText(amountText);

        // Cập nhật thanh tiến trình
        holder.progressBar.setProgress(item.percentage);
        holder.progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(item.color));
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

    static class CategoryItem {
        String category;
        double amount;
        int percentage;
        int color;

        public CategoryItem(String category, double amount, int percentage, int color) {
            this.category = category;
            this.amount = amount;
            this.percentage = percentage;
            this.color = color;
        }
    }
}
