package com.example.quanlychitieu.data.model;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.Date;

@IgnoreExtraProperties
public class Transaction {
    @Exclude
    private String firebaseId; // Firebase document ID

    private long id;
    private String description; // Mô tả giao dịch
    private double amount;      // Số tiền
    private String category;    // Danh mục
    private Date date;          // Ngày giao dịch
    private boolean isIncome;   // true nếu là thu nhập, false nếu là chi tiêu
    private String note;        // Ghi chú cho giao dịch
    private boolean repeat;     // true nếu giao dịch lặp lại, false nếu không
    private String userId;      // ID của người dùng sở hữu giao dịch
    private String repeatType;    // "daily", "weekly", "monthly", "yearly"
    private Date endDate;         // Ngày kết thúc (tùy chọn)

    public Transaction() {
    }

    public Transaction(long id, String description, double amount, String category,
                       Date date, boolean isIncome, String note, boolean repeat, String repeatType, Date endDate) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.isIncome = isIncome;
        this.note = note;
        this.repeat = repeat;
        this.repeatType = repeatType;
        this.endDate = endDate;
    }

    public Transaction(long id, String description, double amount, String category, Date date, boolean isIncome, String note, boolean repeat) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.isIncome = isIncome;
        this.note = note;
        this.repeat = repeat;
    }

    // Constructor sao chép (dùng để tạo giao dịch lặp lại)
    public Transaction(Transaction transaction) {
        this.firebaseId = transaction.firebaseId;
        this.id = transaction.id;
        this.description = transaction.description;
        this.amount = transaction.amount;
        this.category = transaction.category;
        this.date = transaction.date;
        this.isIncome = transaction.isIncome;
        this.note = transaction.note;
        this.repeat = transaction.repeat;
        this.userId = transaction.userId;
        this.repeatType = transaction.repeatType;
        this.endDate = transaction.endDate;
    }

    // Constructor with Firebase ID and user ID
    public Transaction(String firebaseId, long id, String description, double amount,
                       String category, Date date, boolean isIncome, String note,
                       boolean repeat, String userId, String repeatType, Date endDate) {
        this.firebaseId = firebaseId;
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.isIncome = isIncome;
        this.note = note;
        this.repeat = repeat;
        this.userId = userId;
        this.repeatType = repeatType;
        this.endDate = endDate;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isIncome() {
        return isIncome;
    }

    public void setIncome(boolean income) {
        isIncome = income;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

}
