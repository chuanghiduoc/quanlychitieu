package com.example.quanlychitieu.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.viewholder.ReminderViewHolder;
import com.example.quanlychitieu.data.model.Reminder;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderViewHolder> {
    private List<Reminder> reminders = new ArrayList<>();
    private final Context context;
    private final OnReminderActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public interface OnReminderActionListener {
        void onMarkAsPaid(Reminder reminder);
        void onReminderClick(Reminder reminder);
    }

    public ReminderAdapter(Context context, OnReminderActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminders.get(position);

        // Thiết lập tiêu đề
        holder.titleTextView.setText(reminder.getTitle());

        // Thiết lập ngày và giờ
        Date reminderDate = reminder.getDateTime();
        if (reminderDate != null) {
            String dateStr = dateFormat.format(reminderDate);
            String timeStr = timeFormat.format(reminderDate);

            // Xác định tần suất lặp lại
            String repeatInfo = "";
            if (reminder.isRepeating() && reminder.getRepeatType() != null) {
                switch (reminder.getRepeatType()) {
                    case "DAILY":
                        repeatInfo = "Hàng ngày";
                        break;
                    case "WEEKLY":
                        repeatInfo = "Hàng tuần, " + getDayOfWeek(reminderDate);
                        break;
                    case "MONTHLY":
                        repeatInfo = "Hàng tháng, ngày " + getDayOfMonth(reminderDate);
                        break;
                    case "YEARLY":
                        repeatInfo = "Hàng năm, ngày " + dateStr;
                        break;
                }
            }

            if (!repeatInfo.isEmpty()) {
                holder.dateTextView.setText(repeatInfo + " lúc " + timeStr);
            } else {
                holder.dateTextView.setText(dateStr + " lúc " + timeStr);
            }

            // Thiết lập thời gian còn lại
            long daysRemaining = reminder.getDaysRemaining();
            if (daysRemaining > 0) {
                holder.timeRemainingTextView.setText("Còn " + daysRemaining + " ngày nữa");
                holder.timeRemainingTextView.setTextColor(context.getResources().getColor(R.color.chart_1));
            } else if (daysRemaining == 0) {
                holder.timeRemainingTextView.setText("Hôm nay");
                holder.timeRemainingTextView.setTextColor(context.getResources().getColor(R.color.chart_2));
            } else {
                holder.timeRemainingTextView.setText("Quá hạn " + Math.abs(daysRemaining) + " ngày");
                holder.timeRemainingTextView.setTextColor(context.getResources().getColor(R.color.expense));
            }
        }

        // Thiết lập số tiền
        holder.amountTextView.setText(currencyFormat.format(reminder.getAmount()));

        if (reminder.isCompleted()) {
            holder.markAsPaidButton.setVisibility(View.GONE);
            holder.timeRemainingTextView.setText("Đã thanh toán");
            holder.timeRemainingTextView.setTextColor(context.getResources().getColor(R.color.income_green));
        } else {
            holder.markAsPaidButton.setVisibility(View.VISIBLE);
            holder.markAsPaidButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMarkAsPaid(reminder);
                }
            });
        }

        // Thiết lập sự kiện click cho item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReminderClick(reminder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    public void setReminders(List<Reminder> reminders) {

        if (reminders != null) {
            for (Reminder reminder : reminders) {
                Log.d("ReminderAdapter", "Reminder: " + reminder.getTitle() + ", date: " + reminder.getDateTime());
            }
        }

        this.reminders = reminders != null ? reminders : new ArrayList<>();
        notifyDataSetChanged();
    }


    private String getDayOfWeek(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        String[] days = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
        return days[dayOfWeek - 1];
    }

    private String getDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
    }
    public Reminder getReminderAt(int position) {
        if (position >= 0 && position < reminders.size()) {
            return reminders.get(position);
        }
        return null;
    }

}