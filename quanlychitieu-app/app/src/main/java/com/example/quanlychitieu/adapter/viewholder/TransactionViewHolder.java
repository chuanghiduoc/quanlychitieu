package com.example.quanlychitieu.adapter.viewholder;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.TransactionAdapter;
import com.example.quanlychitieu.data.model.Transaction;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransactionViewHolder extends RecyclerView.ViewHolder {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault());
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    private final ImageView categoryIcon;
    private final TextView titleTextView;
    private final TextView amountTextView;
    private final TextView dateTextView;
    private Transaction transaction;

    public TransactionViewHolder(@NonNull View itemView, TransactionAdapter.OnTransactionClickListener listener) {
        super(itemView);
        categoryIcon = itemView.findViewById(R.id.category_icon);
        titleTextView = itemView.findViewById(R.id.transaction_title);
        amountTextView = itemView.findViewById(R.id.transaction_amount);
        dateTextView = itemView.findViewById(R.id.transaction_date);

        // Set click listener for the entire item
        if (listener != null) {
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && transaction != null) {
                    listener.onTransactionClick(transaction);
                }
            });
        }
    }

    public void bind(Transaction transaction) {
        this.transaction = transaction;

        // Set transaction title (description)
        titleTextView.setText(transaction.getDescription());

        // Format and set date
        dateTextView.setText(DATE_FORMAT.format(transaction.getDate()));

        // Format currency amount
        double displayAmount = Math.abs(transaction.getAmount());
        String formattedAmount = CURRENCY_FORMAT.format(displayAmount)
                .replace("₫", "đ")
                .replace(",", ".");

        // Hiển thị dấu và màu sắc dựa trên isIncome thay vì giá trị amount
        if (transaction.isIncome()) {
            // Thu nhập: hiển thị dấu + và màu xanh
            amountTextView.setText("+" + formattedAmount);
            amountTextView.setTextColor(Color.parseColor("#4CAF50")); // Green color
        } else {
            // Chi tiêu: hiển thị dấu - và màu đỏ
            amountTextView.setText("-" + formattedAmount);
            amountTextView.setTextColor(Color.parseColor("#F44336")); // Red color
        }

        // Set category icon based on category name
        setCategoryIcon(transaction.getCategory());
    }


    private void setCategoryIcon(String category) {
        // Set the appropriate icon based on the category
        // This is an example implementation - adjust according to your available drawables
        int iconResId;

        switch (category.toLowerCase()) {
            case "ăn uống":
                iconResId = R.drawable.ic_food; // Icon for food expenses
                break;
            case "di chuyển":
                iconResId = R.drawable.ic_move; // Icon for transportation expenses
                break;
            case "mua sắm":
                iconResId = R.drawable.ic_shopping; // Icon for shopping expenses
                break;
            case "hóa đơn":
                iconResId = R.drawable.ic_invoice; // Icon for bills
                break;
            case "lương":
                iconResId = R.drawable.ic_salary; // Icon for salary income
                break;
            case "thưởng":
                iconResId = R.drawable.ic_bonus; // Icon for bonuses
                break;
            case "quà tặng":
                iconResId = R.drawable.ic_gift; // Icon for gifts
                break;
            case "khác":
                iconResId = R.drawable.ic_more; // General icon for others
                break;
            default:
                iconResId = R.drawable.ic_more; // Default icon if not recognized
                break;
        }
        categoryIcon.setImageResource(iconResId);
    }
}
