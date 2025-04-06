package com.example.quanlychitieu.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class Budget {
    @Exclude
    private String firebaseId; // ID tài liệu trên Firebase

    private long id;
    private String userId;     // ID của người dùng sở hữu ngân sách
    private String category;   // Danh mục ngân sách
    private double amount;     // Số tiền ngân sách
    private double spent;      // Số tiền đã chi tiêu
    private Date startDate;    // Ngày bắt đầu ngân sách
    private Date endDate;      // Ngày kết thúc ngân sách
    private String note;       // Ghi chú tùy chọn về ngân sách này

    // Cài đặt thông báo
    private boolean notificationsEnabled;  // Có bật thông báo hay không
    private int notificationThreshold;     // Ngưỡng phần trăm để gửi thông báo (80, 90, 100)
    private boolean notificationSent;      // Đã gửi thông báo cho ngưỡng này chưa

    private Map<String, Boolean> recurringExpenseNotifications;

    public Budget() {
        // Khởi tạo bản đồ cho thông báo chi tiêu định kỳ
        recurringExpenseNotifications = new HashMap<>();
    }

    // Constructor cơ bản
    public Budget(long id, String userId, String category, double amount, Date startDate, Date endDate) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.amount = amount;
        this.spent = 0;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notificationsEnabled = true;
        this.notificationThreshold = 80; // Mặc định là 80%
        this.notificationSent = false;
        this.recurringExpenseNotifications = new HashMap<>();
    }

    // Constructor đầy đủ
    public Budget(long id, String userId, String category, double amount, double spent,
                  Date startDate, Date endDate, String note, boolean notificationsEnabled,
                  int notificationThreshold, boolean notificationSent) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.amount = amount;
        this.spent = spent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.note = note;
        this.notificationsEnabled = notificationsEnabled;
        this.notificationThreshold = notificationThreshold;
        this.notificationSent = notificationSent;
        this.recurringExpenseNotifications = new HashMap<>();
    }


    @Exclude
    public double getRemaining() {
        return amount - spent; // Số tiền còn lại trong ngân sách
    }

    @Exclude
    public int getProgressPercentage() {
        return amount > 0 ? (int) ((spent / amount) * 100) : 0; // Tính phần trăm đã chi tiêu
    }

    /**
     * Reset trạng thái thông báo để gửi thông báo mới
     */
    public void resetNotificationStatus() {
        this.notificationSent = false;
    }
    // Getter và Setter

    @Exclude
    public String getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getSpent() {
        return spent;
    }

    public void setSpent(double spent) {
        this.spent = spent;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }



    public int getNotificationThreshold() {
        return notificationThreshold;
    }


    public boolean isNotificationSent() {
        return notificationSent;
    }

    public Map<String, Boolean> getRecurringExpenseNotifications() {
        return recurringExpenseNotifications;
    }

    public void setRecurringExpenseNotifications(Map<String, Boolean> recurringExpenseNotifications) {
        this.recurringExpenseNotifications = recurringExpenseNotifications;
    }
}
