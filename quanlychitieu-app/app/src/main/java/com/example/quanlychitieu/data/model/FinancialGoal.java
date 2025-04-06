package com.example.quanlychitieu.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Date;

@IgnoreExtraProperties
public class FinancialGoal {
    @Exclude
    private String firebaseId; // Firebase document ID

    private long id;
    private String userId;     // ID của người dùng sở hữu mục tiêu
    private String name;       // Tên mục tiêu
    private String description; // Mô tả mục tiêu
    private double targetAmount; // Số tiền mục tiêu
    private double currentAmount; // Số tiền hiện tại đã tiết kiệm
    private Date startDate;    // Ngày bắt đầu
    private Date endDate;      // Ngày kết thúc
    private boolean completed; // Trạng thái hoàn thành
    private String category;   // Danh mục của mục tiêu (tùy chọn)

    public FinancialGoal() {
        // Constructor rỗng cần thiết cho Firestore
    }

    // Constructor đầy đủ
    public FinancialGoal(long id, String userId, String name, String description,
                         double targetAmount, double currentAmount,
                         Date startDate, Date endDate, boolean completed, String category) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.completed = completed;
        this.category = category;
    }

    // Constructor cơ bản
    public FinancialGoal(long id, String userId, String name, double targetAmount,
                         Date startDate, Date endDate) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.targetAmount = targetAmount;
        this.currentAmount = 0;
        this.startDate = startDate;
        this.endDate = endDate;
        this.completed = false;
    }

    @Exclude
    public int getProgressPercentage() {
        return targetAmount > 0 ? (int) ((currentAmount / targetAmount) * 100) : 0;
    }

    @Exclude
    public double getRemainingAmount() {
        return targetAmount - currentAmount;
    }

    // Getters và Setters
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(double targetAmount) {
        this.targetAmount = targetAmount;
    }

    public double getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(double currentAmount) {
        this.currentAmount = currentAmount;
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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
