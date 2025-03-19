package com.example.quanlychitieu.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long reminderId = intent.getLongExtra("REMINDER_ID", -1);
        String title = intent.getStringExtra("REMINDER_TITLE");
        double amount = intent.getDoubleExtra("REMINDER_AMOUNT", 0);

        if (reminderId != -1 && title != null) {
            ReminderNotificationService notificationService = new ReminderNotificationService(context);
            notificationService.showNotification(reminderId, title, amount);
        }
    }
}
