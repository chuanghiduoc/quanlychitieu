package com.example.quanlychitieu.adapter.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.google.android.material.button.MaterialButton;

public class ReminderViewHolder extends RecyclerView.ViewHolder {
    public ImageView iconImageView;
    public TextView titleTextView;
    public TextView dateTextView;
    public TextView timeRemainingTextView;
    public TextView amountTextView;
    public MaterialButton markAsPaidButton;

    public ReminderViewHolder(@NonNull View itemView) {
        super(itemView);
        iconImageView = itemView.findViewById(R.id.reminder_icon);
        titleTextView = itemView.findViewById(R.id.reminder_title);
        dateTextView = itemView.findViewById(R.id.reminder_date);
        timeRemainingTextView = itemView.findViewById(R.id.reminder_time_remaining);
        amountTextView = itemView.findViewById(R.id.reminder_amount);
        markAsPaidButton = itemView.findViewById(R.id.mark_as_paid_button);
    }
}