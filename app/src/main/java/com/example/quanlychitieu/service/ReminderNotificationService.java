package com.example.quanlychitieu.service;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.Reminder;
import com.example.quanlychitieu.MainActivity;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderNotificationService {
    private static final String CHANNEL_ID = "reminder_channel";
    private static final String CHANNEL_NAME = "Nhắc nhở thanh toán";
    private final Context context;
    private final NotificationManager notificationManager;
    private final AlarmManager alarmManager;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public ReminderNotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Kênh thông báo cho các nhắc nhở thanh toán");
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void scheduleNotification(Reminder reminder) {
        // Tạo intent cho notification
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("REMINDER_ID", reminder.getId());
        intent.putExtra("REMINDER_DOCUMENT_ID", reminder.getDocumentId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) reminder.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tạo intent cho broadcast receiver
        Intent alarmIntent = new Intent(context, ReminderBroadcastReceiver.class);
        alarmIntent.putExtra("REMINDER_ID", reminder.getId());
        alarmIntent.putExtra("REMINDER_DOCUMENT_ID", reminder.getDocumentId());
        alarmIntent.putExtra("REMINDER_TITLE", reminder.getTitle());
        alarmIntent.putExtra("REMINDER_AMOUNT", reminder.getAmount());

        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(
                context,
                (int) reminder.getId(),
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Lên lịch thông báo
        Date reminderDate = reminder.getDateTime();
        if (reminderDate != null) {
            // Lên lịch thông báo trước 1 ngày
            Calendar notificationTime = Calendar.getInstance();
            notificationTime.setTime(reminderDate);
            notificationTime.add(Calendar.DAY_OF_MONTH, -1);

            // Chỉ lên lịch nếu thời gian nhắc nhở trong tương lai
            if (notificationTime.getTimeInMillis() > System.currentTimeMillis()) {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        notificationTime.getTimeInMillis(),
                        alarmPendingIntent
                );
            }

            // Lên lịch thông báo vào đúng ngày
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminderDate.getTime(),
                    alarmPendingIntent
            );
        }
    }


    public void cancelNotification(long reminderId) {
        // Hủy notification nếu đã hiển thị
        notificationManager.cancel((int) reminderId);

        // Hủy alarm đã lên lịch
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    public void showNotification(long reminderId, String title, double amount) {
        // Tạo intent khi nhấn vào notification
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("REMINDER_ID", reminderId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tạo intent cho nút "Đánh dấu đã thanh toán"
        Intent markAsPaidIntent = new Intent(context, ReminderActionReceiver.class);
        markAsPaidIntent.setAction("MARK_AS_PAID");
        markAsPaidIntent.putExtra("REMINDER_ID", reminderId);
        markAsPaidIntent.putExtra("REMINDER_DOCUMENT_ID", "");

        // Sử dụng request code khác cho PendingIntent này
        PendingIntent markAsPaidPendingIntent = PendingIntent.getBroadcast(
                context,
                (int) reminderId + 1000,
                markAsPaidIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tạo notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Nhắc nhở thanh toán")
                .setContentText(title + ": " + currencyFormat.format(amount))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_check, "Đã thanh toán? Click vào đây", markAsPaidPendingIntent)
                .setAutoCancel(true);

        // Hiển thị notification
        notificationManager.notify((int) reminderId, builder.build());
    }



}