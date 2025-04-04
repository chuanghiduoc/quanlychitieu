package com.example.quanlychitieu.data.repository;

import android.util.Log;

import com.example.quanlychitieu.data.model.Reminder;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ReminderRepository {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private static final String COLLECTION_USERS = "users";
    private static final String SUBCOLLECTION_REMINDERS = "reminders";

    public ReminderRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        ensureUserCollectionExists();
    }


    private CollectionReference getRemindersCollection() {
        String userId = auth.getCurrentUser().getUid();


        CollectionReference subCollection = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_REMINDERS);

        return subCollection;
    }


    // Lấy danh sách nhắc nhở sắp tới (chưa hoàn thành)
    public Task<QuerySnapshot> getUpcomingReminders() {
        String userId = auth.getCurrentUser().getUid();
        Date now = new Date();

        Log.d("ReminderRepository", "Getting upcoming reminders, current time: " + now);

        return getRemindersCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", false)
                .whereGreaterThanOrEqualTo("dateTime", now) // Chỉ lấy nhắc nhở có thời gian từ hiện tại trở đi
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .get();
    }

    // Lấy danh sách nhắc nhở đã qua (đã hoàn thành hoặc quá hạn)
    public Task<QuerySnapshot> getPastReminders() {
        String userId = auth.getCurrentUser().getUid();

        Log.d("ReminderRepository", "Getting past reminders for user: " + userId);

        return getRemindersCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("ReminderRepository", "Found " + querySnapshot.size() + " completed reminders");
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d("ReminderRepository", "Completed reminder: " + doc.getId() + ", title: " + doc.getString("title"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ReminderRepository", "Error getting completed reminders", e);
                });
    }

    public Task<QuerySnapshot> getOverdueReminders() {
        String userId = auth.getCurrentUser().getUid();
        Date now = new Date();

        return getRemindersCollection()
                .whereEqualTo("userId", userId)
                .whereEqualTo("isCompleted", false)
                .whereLessThan("dateTime", now)
                .orderBy("dateTime", Query.Direction.DESCENDING)
                .get();
    }
    // Thêm nhắc nhở mới
    public Task<DocumentReference> addReminder(Reminder reminder) {
        // Đảm bảo reminder có userId
        if (reminder.getUserId() == null && auth.getCurrentUser() != null) {
            reminder.setUserId(auth.getCurrentUser().getUid());
        }

        Map<String, Object> reminderMap = convertReminderToMap(reminder);
        return getRemindersCollection().add(reminderMap);
    }

    // Cập nhật nhắc nhở
    public Task<Void> updateReminder(String documentId, Reminder reminder) {
        Map<String, Object> reminderMap = convertReminderToMap(reminder);
        return getRemindersCollection().document(documentId).update(reminderMap);
    }

    // Đánh dấu nhắc nhở là đã hoàn thành
    public Task<Void> markReminderAsCompleted(String documentId) {
        return getRemindersCollection()
                .document(documentId)
                .update("isCompleted", true);
    }

    // Xóa nhắc nhở
    public Task<Void> deleteReminder(String documentId) {
        return getRemindersCollection().document(documentId).delete();
    }

    // Lấy nhắc nhở theo ID
    public Task<DocumentSnapshot> getReminderById(String documentId) {
        return getRemindersCollection().document(documentId).get();
    }

    // Lấy nhắc nhở theo ID số
    public Task<QuerySnapshot> getReminderByNumericId(long numericId) {
        return getRemindersCollection()
                .whereEqualTo("numericId", numericId)
                .limit(1)
                .get();
    }

    // Chuyển đổi Reminder thành Map để lưu vào Firebase
    private Map<String, Object> convertReminderToMap(Reminder reminder) {
        Map<String, Object> map = new HashMap<>();
        map.put("numericId", reminder.getId());
        map.put("title", reminder.getTitle());

        // Đảm bảo dateTime không null trước khi lưu
        if (reminder.getDateTime() != null) {
            // Lưu trực tiếp Date, Firestore sẽ chuyển đổi thành Timestamp
            map.put("dateTime", reminder.getDateTime());
        }

        map.put("isCompleted", reminder.isCompleted());
        map.put("amount", reminder.getAmount());
        map.put("category", reminder.getCategory());
        map.put("note", reminder.getNote());
        map.put("isRepeating", reminder.isRepeating());
        map.put("repeatType", reminder.getRepeatType());

        // Đảm bảo endDate không null trước khi lưu
        if (reminder.getEndDate() != null) {
            map.put("endDate", reminder.getEndDate());
        }

        map.put("userId", reminder.getUserId());
        map.put("createdAt", new Date());
        return map;
    }
    public void ensureUserCollectionExists() {
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            DocumentReference userDoc = db.collection(COLLECTION_USERS).document(userId);

            userDoc.get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) {
                    // Tạo document cho user nếu chưa tồn tại
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("createdAt", new Date());
                    userDoc.set(userData);
                }
            });
        }
    }
    public ListenerRegistration listenForUpcomingReminders(EventListener<QuerySnapshot> listener) {
        return getRemindersCollection()
                .whereEqualTo("isCompleted", false)
                .whereGreaterThanOrEqualTo("dateTime", new Date())
                .orderBy("dateTime", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    public ListenerRegistration listenForPastReminders(EventListener<QuerySnapshot> listener) {
        return getRemindersCollection()
                .whereEqualTo("isCompleted", true)
                .orderBy("dateTime", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }
}