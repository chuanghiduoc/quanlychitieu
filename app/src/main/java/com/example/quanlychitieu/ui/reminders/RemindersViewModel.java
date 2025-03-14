package com.example.quanlychitieu.ui.reminders;

import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.model.Reminder;
import com.example.quanlychitieu.data.repository.ReminderRepository;
import com.example.quanlychitieu.service.ReminderNotificationService;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class RemindersViewModel extends ViewModel {
    private final ReminderRepository repository;
    private final MutableLiveData<List<Reminder>> upcomingReminders = new MutableLiveData<>();
    private final MutableLiveData<List<Reminder>> pastReminders = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Reminder> selectedReminder = new MutableLiveData<>();
    private ListenerRegistration upcomingRemindersListener;
    private ListenerRegistration pastRemindersListener;

    public RemindersViewModel() {
        repository = new ReminderRepository();
        loadUpcomingReminders();
        loadPastReminders();
    }

    public LiveData<List<Reminder>> getUpcomingReminders() {
        return upcomingReminders;
    }

    public LiveData<List<Reminder>> getPastReminders() {
        return pastReminders;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadUpcomingReminders() {
        isLoading.setValue(true);
        repository.getUpcomingReminders()
                .addOnSuccessListener(this::processUpcomingReminders)
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Không thể tải nhắc nhở: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    public void loadPastReminders() {
        isLoading.setValue(true);
        repository.getPastReminders()
                .addOnSuccessListener(this::processPastReminders)
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Không thể tải nhắc nhở: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    private void processUpcomingReminders(QuerySnapshot querySnapshot) {
        List<Reminder> reminders = new ArrayList<>();
        Date now = new Date();

        Log.d(TAG, "Upcoming documents count: " + querySnapshot.size());

        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
            Reminder reminder = document.toObject(Reminder.class);
            if (reminder != null) {
                reminder.setDocumentId(document.getId());

                // Kiểm tra xem nhắc nhở có thực sự sắp tới không
                Date reminderDate = reminder.getDateTime();
                if (reminderDate != null && reminderDate.after(now) && !reminder.isCompleted()) {
                    reminders.add(reminder);
                    Log.d(TAG, "Added upcoming reminder: " + reminder.getTitle());
                } else {
                    Log.d(TAG, "Skipped non-upcoming reminder: " + reminder.getTitle());
                }
            }
        }

        upcomingReminders.setValue(reminders);
        isLoading.setValue(false);
    }

    private void processPastReminders(QuerySnapshot querySnapshot) {
        List<Reminder> reminders = new ArrayList<>();
        Date now = new Date();

        Log.d(TAG, "Processing past reminders from query");

        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
            Reminder reminder = document.toObject(Reminder.class);
            if (reminder != null) {
                reminder.setDocumentId(document.getId());

                // Thêm vào danh sách nếu đã hoàn thành
                if (reminder.isCompleted()) {
                    reminders.add(reminder);
                    Log.d(TAG, "Added completed reminder: " + reminder.getTitle());
                }
            }
        }

        // Thêm truy vấn để lấy các nhắc nhở quá hạn
        repository.getOverdueReminders()
                .addOnSuccessListener(overdueSnapshot -> {
                    for (DocumentSnapshot document : overdueSnapshot.getDocuments()) {
                        Reminder reminder = document.toObject(Reminder.class);
                        if (reminder != null && !reminder.isCompleted()) {
                            reminder.setDocumentId(document.getId());
                            reminders.add(reminder);
                            Log.d(TAG, "Added overdue reminder: " + reminder.getTitle());
                        }
                    }

                    // Sắp xếp lại danh sách theo thời gian giảm dần
                    Collections.sort(reminders, (r1, r2) ->
                            r2.getDateTime().compareTo(r1.getDateTime()));

                    pastReminders.setValue(reminders);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error loading overdue reminders", e));
    }

    public void markReminderAsCompleted(String documentId, long reminderId,
                                        ReminderNotificationService notificationService) {
        repository.markReminderAsCompleted(documentId)
                .addOnSuccessListener(aVoid -> {
                    // Hủy thông báo
                    if (notificationService != null) {
                        notificationService.cancelNotification(reminderId);
                    }

                    // Refresh data
                    loadUpcomingReminders();
                    loadPastReminders();
                })
                .addOnFailureListener(e ->
                        errorMessage.setValue("Không thể cập nhật nhắc nhở: " + e.getMessage())
                );
    }

    // Phiên bản không có notification service (cho tương thích ngược)
    public void markReminderAsCompleted(String documentId) {
        markReminderAsCompleted(documentId, 0, null);
    }

    public void addReminder(Reminder reminder, ReminderNotificationService notificationService) {
        repository.addReminder(reminder)
                .addOnSuccessListener(documentReference -> {
                    // Gán ID từ Firestore cho reminder
                    String id = documentReference.getId();
                    reminder.setDocumentId(id);

                    // Lên lịch thông báo
                    if (notificationService != null) {
                        notificationService.scheduleNotification(reminder);
                    }

                    // Refresh data
                    loadUpcomingReminders();
                })
                .addOnFailureListener(e ->
                        errorMessage.setValue("Không thể thêm nhắc nhở: " + e.getMessage())
                );
    }

    // Phiên bản không có notification service (cho tương thích ngược)
    public void addReminder(Reminder reminder) {
        addReminder(reminder, null);
    }

    public void updateReminder(String documentId, Reminder reminder,
                               ReminderNotificationService notificationService) {
        repository.updateReminder(documentId, reminder)
                .addOnSuccessListener(aVoid -> {
                    // Hủy thông báo cũ và lên lịch lại
                    if (notificationService != null) {
                        notificationService.cancelNotification(reminder.getId());
                        notificationService.scheduleNotification(reminder);
                    }

                    // Refresh data
                    loadUpcomingReminders();
                    loadPastReminders();
                })
                .addOnFailureListener(e ->
                        errorMessage.setValue("Không thể cập nhật nhắc nhở: " + e.getMessage())
                );
    }

    // Phiên bản không có notification service (cho tương thích ngược)
    public void updateReminder(String documentId, Reminder reminder) {
        updateReminder(documentId, reminder, null);
    }

    public void deleteReminder(String documentId, long reminderId,
                               ReminderNotificationService notificationService) {
        repository.deleteReminder(documentId)
                .addOnSuccessListener(aVoid -> {
                    // Hủy thông báo
                    if (notificationService != null) {
                        notificationService.cancelNotification(reminderId);
                    }

                    // Refresh data
                    loadUpcomingReminders();
                    loadPastReminders();
                })
                .addOnFailureListener(e ->
                        errorMessage.setValue("Không thể xóa nhắc nhở: " + e.getMessage())
                );
    }

    // Phiên bản không có notification service (cho tương thích ngược)
    public void deleteReminder(String documentId) {
        deleteReminder(documentId, 0, null);
    }

    public LiveData<Reminder> getReminderByNumericId(long numericId) {
        MutableLiveData<Reminder> result = new MutableLiveData<>();

        repository.getReminderByNumericId(numericId)
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        Reminder reminder = document.toObject(Reminder.class);
                        if (reminder != null) {
                            reminder.setDocumentId(document.getId());
                            result.setValue(reminder);
                        } else {
                            result.setValue(null);
                        }
                    } else {
                        result.setValue(null);
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Không thể tải nhắc nhở: " + e.getMessage());
                    result.setValue(null);
                });
        return result;
    }

    public void startListeningForReminders() {
        if (upcomingRemindersListener != null) {
            upcomingRemindersListener.remove();
        }

        upcomingRemindersListener = repository.listenForUpcomingReminders((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening for upcoming reminders", error);
                errorMessage.setValue("Lỗi khi lắng nghe nhắc nhở: " + error.getMessage());
                return;
            }

            if (value != null) {
                processUpcomingReminders(value);
            }
        });

        if (pastRemindersListener != null) {
            pastRemindersListener.remove();
        }

        pastRemindersListener = repository.listenForPastReminders((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Error listening for past reminders", error);
                errorMessage.setValue("Lỗi khi lắng nghe nhắc nhở: " + error.getMessage());
                return;
            }

            if (value != null) {
                processPastReminders(value);
            }
        });
    }

    public void stopListeningForReminders() {
        if (upcomingRemindersListener != null) {
            upcomingRemindersListener.remove();
            upcomingRemindersListener = null;
        }

        if (pastRemindersListener != null) {
            pastRemindersListener.remove();
            pastRemindersListener = null;
        }
    }
    public LiveData<Reminder> getReminderByDocumentId(String documentId) {
        MutableLiveData<Reminder> result = new MutableLiveData<>();

        repository.getReminderById(documentId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Reminder reminder = documentSnapshot.toObject(Reminder.class);
                        if (reminder != null) {
                            reminder.setDocumentId(documentId);
                            result.setValue(reminder);
                            Log.d("RemindersViewModel", "Found reminder by documentId: " + documentId);
                        } else {
                            result.setValue(null);
                            Log.e("RemindersViewModel", "Failed to convert document to Reminder");
                        }
                    } else {
                        result.setValue(null);
                        Log.e("RemindersViewModel", "Document does not exist: " + documentId);
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Không thể tải nhắc nhở: " + e.getMessage());
                    result.setValue(null);
                    Log.e("RemindersViewModel", "Error loading reminder by documentId: " + e.getMessage());
                });
        return result;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        stopListeningForReminders();
    }
}
