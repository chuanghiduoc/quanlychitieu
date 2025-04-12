package com.example.quanlychitieu.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import java.util.Date;

public class Reminder {
    private long id;
    private String title;
    private Date dateTime;
    private boolean isCompleted;
    private double amount;
    private String category;
    private String note;
    private boolean isRepeating;
    private String repeatType;
    private Date endDate;
    private String userId;
    private Date createdAt;
    private long numericId;

    @Exclude
    private String documentId;

    // Constructor không tham số (cần thiết cho Firestore)
    public Reminder() {}

    // Constructor với tham số
    public Reminder(long id, String title, Date dateTime, boolean isCompleted,
                    double amount, String category, String note, boolean isRepeating,
                    String repeatType, Date endDate, String userId) {
        this.id = id;
        this.title = title;
        this.dateTime = dateTime;
        this.isCompleted = isCompleted;
        this.amount = amount;
        this.category = category;
        this.note = note;
        this.isRepeating = isRepeating;
        this.repeatType = repeatType;
        this.endDate = endDate;
        this.userId = userId;
        this.numericId = id;
        this.createdAt = new Date();
    }

    // Getters và Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public void setDateTime(Date dateTime) {
        this.dateTime = dateTime;
    }

    @PropertyName("isCompleted")
    public boolean isCompleted() {
        return isCompleted;
    }

    @PropertyName("isCompleted")
    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @PropertyName("isRepeating")
    public boolean isRepeating() {
        return isRepeating;
    }

    @PropertyName("isRepeating")
    public void setRepeating(boolean repeating) {
        isRepeating = repeating;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Getter và Setter cho createdAt
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    // Getter và Setter cho numericId
    public long getNumericId() {
        return numericId;
    }

    public void setNumericId(long numericId) {
        this.numericId = numericId;
    }

    @Exclude
    public String getDocumentId() {
        return documentId;
    }

    @Exclude
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    // Phương thức để tính số ngày còn lại đến deadline
    @Exclude
    public long getDaysRemaining() {
        if (dateTime == null) return 0;

        long diff = dateTime.getTime() - new Date().getTime();
        return diff / (24 * 60 * 60 * 1000);
    }

    // Phương thức kiểm tra xem nhắc nhở đã quá hạn chưa
    @Exclude
    public boolean isOverdue() {
        if (dateTime == null) return false;
        return !isCompleted && dateTime.before(new Date());
    }
}
