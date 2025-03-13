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

    // Để gửi thông báo cho các khoản chi tiêu định kỳ
    private Map<String, Boolean> recurringExpenseNotifications; // Bản đồ chứa ID giao dịch và trạng thái thông báo

    // Constructor không tham số dành cho Firebase
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

    // Các phương thức hỗ trợ

    @Exclude
    public double getRemaining() {
        return amount - spent; // Số tiền còn lại trong ngân sách
    }

    @Exclude
    public int getProgressPercentage() {
        return amount > 0 ? (int) ((spent / amount) * 100) : 0; // Tính phần trăm đã chi tiêu
    }

    @Exclude
    public boolean isOverBudget() {
        return spent > amount; // Kiểm tra xem ngân sách có bị vượt quá không
    }

    @Exclude
    public boolean shouldSendNotification() {
        if (!notificationsEnabled || notificationSent) {
            return false;
        }

        int currentPercentage = getProgressPercentage();
        return currentPercentage >= notificationThreshold; // Kiểm tra xem có cần gửi thông báo không
    }

    @Exclude
    public boolean isActive() {
        Date now = new Date();
        return now.after(startDate) && now.before(endDate); // Kiểm tra xem ngân sách có đang hoạt động không
    }

    @Exclude
    public boolean shouldNotifyForRecurringExpense(String transactionId) {
        // Kiểm tra xem đã gửi thông báo cho giao dịch này chưa
        Boolean notified = recurringExpenseNotifications.get(transactionId);
        return notified == null || !notified;
    }

    public void markRecurringExpenseNotified(String transactionId) {
        recurringExpenseNotifications.put(transactionId, true); // Đánh dấu đã gửi thông báo cho giao dịch định kỳ
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

    public void addExpense(double expense) {
        this.spent += expense; // Thêm một khoản chi tiêu vào ngân sách
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

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public int getNotificationThreshold() {
        return notificationThreshold;
    }

    public void setNotificationThreshold(int notificationThreshold) {
        this.notificationThreshold = notificationThreshold;
    }

    public boolean isNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public Map<String, Boolean> getRecurringExpenseNotifications() {
        return recurringExpenseNotifications;
    }

    public void setRecurringExpenseNotifications(Map<String, Boolean> recurringExpenseNotifications) {
        this.recurringExpenseNotifications = recurringExpenseNotifications;
    }
}
