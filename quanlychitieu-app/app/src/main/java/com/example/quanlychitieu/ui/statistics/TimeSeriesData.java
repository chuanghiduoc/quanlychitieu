package com.example.quanlychitieu.ui.statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp để chứa dữ liệu chuỗi thời gian cho biểu đồ cột
 */
public class TimeSeriesData {
    private final List<String> labels;
    private final List<Float> incomeValues;
    private final List<Float> expenseValues;

    public TimeSeriesData() {
        this.labels = new ArrayList<>();
        this.incomeValues = new ArrayList<>();
        this.expenseValues = new ArrayList<>();
    }

    public TimeSeriesData(List<String> labels, List<Float> incomeValues, List<Float> expenseValues) {
        this.labels = labels;
        this.incomeValues = incomeValues;
        this.expenseValues = expenseValues;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<Float> getIncomeValues() {
        return incomeValues;
    }

    public List<Float> getExpenseValues() {
        return expenseValues;
    }
}