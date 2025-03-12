package com.example.quanlychitieu.data.model;

import java.util.Date;

public class Transaction {
    private long id;
    private String description; // Mô tả giao dịch
    private double amount;       // Số tiền
    private String category;     // Danh mục
    private Date date;          // Ngày giao dịch
    private boolean isIncome;    // true nếu là thu nhập, false nếu là chi tiêu
    private String note;         // Ghi chú cho giao dịch

    public Transaction(long id, String description, double amount, String category, Date date, boolean isIncome, String note) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.isIncome = isIncome;
        this.note = note;
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public Date getDate() {
        return date;
    }

    public boolean isIncome() {
        return isIncome;
    }

    public String getNote() {
        return note;
    }
}