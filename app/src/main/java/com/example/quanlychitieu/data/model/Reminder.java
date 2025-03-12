package com.example.quanlychitieu.data.model;

import java.util.Date;

public class Reminder {
    private long id;
    private String title;   // Tiêu đề nhắc nhở
    private Date dateTime;  // Ngày giờ nhắc nhở
    private boolean isCompleted; // Trạng thái hoàn thành

    public Reminder(long id, String title, Date dateTime, boolean isCompleted) {
        this.id = id;
        this.title = title;
        this.dateTime = dateTime;
        this.isCompleted = isCompleted;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Date getDateTime() {
        return dateTime;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}