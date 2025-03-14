package com.example.quanlychitieu.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.quanlychitieu.data.repository.ReminderRepository;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ReminderActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderAction";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        long reminderId = intent.getLongExtra("REMINDER_ID", -1);
        String documentId = intent.getStringExtra("REMINDER_DOCUMENT_ID");

        Log.d(TAG, "Received action: " + action + " for reminder: " + reminderId);

        if ("MARK_AS_PAID".equals(action) && reminderId != -1) {
            // Hiển thị Toast để người dùng biết hành động đang được xử lý
            Toast.makeText(context, "Đang đánh dấu đã thanh toán...", Toast.LENGTH_SHORT).show();

            // Hủy thông báo ngay lập tức
            ReminderNotificationService notificationService = new ReminderNotificationService(context);
            notificationService.cancelNotification(reminderId);

            // Tìm reminder theo ID và đánh dấu là đã thanh toán
            ReminderRepository repository = new ReminderRepository();
            repository.getReminderByNumericId(reminderId)
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String docId = querySnapshot.getDocuments().get(0).getId();
                            Log.d(TAG, "Found reminder document: " + docId);

                            repository.markReminderAsCompleted(docId)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Marked reminder as completed: " + docId);
                                        Toast.makeText(context, "Đã đánh dấu thanh toán thành công", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to mark as completed", e);
                                        Toast.makeText(context, "Không thể đánh dấu thanh toán: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Log.e(TAG, "No reminder found with ID: " + reminderId);
                            Toast.makeText(context, "Không tìm thấy nhắc nhở", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to find reminder", e);
                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
