package com.example.quanlychitieu.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.example.quanlychitieu.data.model.Reminder;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.data.repository.ReminderRepository;
import com.example.quanlychitieu.data.repository.TransactionRepository;

import java.util.Date;

public class ReminderActionReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderAction";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        long reminderId = intent.getLongExtra("REMINDER_ID", -1);
        String documentId = intent.getStringExtra("REMINDER_DOCUMENT_ID");

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

                            // Lấy thông tin reminder để tạo giao dịch
                            Reminder reminder = querySnapshot.getDocuments().get(0).toObject(Reminder.class);
                            if (reminder != null) {
                                // Tạo giao dịch chi tiêu từ reminder
                                createExpenseTransaction(context, reminder);
                            }

                            repository.markReminderAsCompleted(docId)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(context, "Đã đánh dấu thanh toán thành công", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(context, "Không thể đánh dấu thanh toán: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(context, "Không tìm thấy nhắc nhở", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /**
     * Tạo giao dịch chi tiêu từ nhắc nhở
     */
    private void createExpenseTransaction(Context context, Reminder reminder) {
        // Khởi tạo TransactionRepository
        TransactionRepository transactionRepository = TransactionRepository.getInstance();
        transactionRepository.setContext(context);

        // Tạo đối tượng Transaction mới
        Transaction transaction = new Transaction();
        transaction.setId(System.currentTimeMillis());

        // Sử dụng danh mục của nhắc nhở làm mô tả cho giao dịch
        String category = reminder.getCategory();
        if (category != null && !category.isEmpty()) {
            transaction.setDescription(category);
        } else {
            transaction.setDescription(reminder.getTitle());
        }

        transaction.setAmount(reminder.getAmount());
        transaction.setCategory("Khác"); // Danh mục mặc định là "Khác"
        transaction.setDate(new Date()); // Ngày hiện tại
        transaction.setIncome(false); // Đánh dấu là chi tiêu
        transaction.setNote(reminder.getTitle()); // Ghi chú là tiêu đề của nhắc nhở
        transaction.setRepeat(false);

        // Thêm giao dịch vào cơ sở dữ liệu
        transactionRepository.addTransaction(transaction);
    }
}
