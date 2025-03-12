package com.example.quanlychitieu.data.model;

import java.util.Date;

public class Budget {
    private long id;
    private String category; // Danh mục ngân sách
    private double amount;   // Số tiền ngân sách
    private double spent;    // Số tiền đã chi
    private Date startDate;  // Ngày bắt đầu ngân sách
    private Date endDate;    // Ngày kết thúc ngân sách

    public Budget(long id, String category, double amount, double spent, Date startDate, Date endDate) {
        this.id = id;
        this.category = category;
        this.amount = amount;
        this.spent = spent;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public long getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public double getSpent() {
        return spent;
    }

    public double getRemaining() {
        return amount - spent;
    }

    public int getProgressPercentage() {
        return amount > 0 ? (int) ((spent / amount) * 100) : 0;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }
}
