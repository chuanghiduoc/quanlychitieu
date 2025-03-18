package com.example.quanlychitieu.ui.statistics;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.data.repository.TransactionRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class StatisticsViewModel extends ViewModel {
    private static final String TAG = "StatisticsViewModel";

    private final TransactionRepository repository;
    private final MutableLiveData<Double> income = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> expenses = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> balance = new MutableLiveData<>(0.0);
    private final MutableLiveData<Map<String, Double>> categoryExpenses = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<TimeSeriesData> timeSeriesData = new MutableLiveData<>(new TimeSeriesData());

    public StatisticsViewModel() {
        repository = TransactionRepository.getInstance();
    }

    public void loadFinancialData(Date startDate, Date endDate) {
        // Get transactions for the period
        repository.getFilteredTransactions(startDate, endDate, "Tất cả danh mục", "Tất cả giao dịch")
                .observeForever(transactions -> {
                    if (transactions != null) {
                        processTransactions(transactions, startDate, endDate);
                    } else {
                        resetData();
                    }
                });
    }

    private void processTransactions(List<Transaction> transactions, Date startDate, Date endDate) {
        double totalIncome = 0;
        double totalExpenses = 0;
        Map<String, Double> expensesByCategory = new HashMap<>();

        // Process each transaction
        for (Transaction transaction : transactions) {
            double amount = Math.abs(transaction.getAmount());

            if (transaction.isIncome()) {
                totalIncome += amount;
            } else {
                totalExpenses += amount;

                // Add to category expenses
                String category = transaction.getCategory();
                double currentAmount = expensesByCategory.getOrDefault(category, 0.0);
                expensesByCategory.put(category, currentAmount + amount);
            }
        }

        // Update LiveData values
        income.setValue(totalIncome);
        expenses.setValue(totalExpenses);
        balance.setValue(totalIncome - totalExpenses);

        // Sort categories by amount (descending)
        Map<String, Double> sortedExpenses = sortCategoriesByAmount(expensesByCategory);
        categoryExpenses.setValue(sortedExpenses);

        // Process time series data
        processTimeSeriesData(transactions, startDate, endDate);
    }

    private Map<String, Double> sortCategoriesByAmount(Map<String, Double> unsortedMap) {
        // Convert to list for sorting
        List<Map.Entry<String, Double>> list = new ArrayList<>(unsortedMap.entrySet());

        // Sort by value (amount) in descending order
        Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        // Put back into LinkedHashMap to maintain order
        Map<String, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    private void processTimeSeriesData(List<Transaction> transactions, Date startDate, Date endDate) {
        // Determine time interval based on date range
        Calendar start = Calendar.getInstance();
        start.setTime(startDate);

        Calendar end = Calendar.getInstance();
        end.setTime(endDate);

        long rangeDays = (end.getTimeInMillis() - start.getTimeInMillis()) / (24 * 60 * 60 * 1000);
        String interval;

        if (rangeDays <= 31) {
            interval = "day";
        } else if (rangeDays <= 366) {
            interval = "month";
        } else {
            interval = "year";
        }

        // Create time series data
        TimeSeriesData data = createTimeSeriesData(transactions, startDate, endDate, interval);
        timeSeriesData.setValue(data);
    }

    private TimeSeriesData createTimeSeriesData(List<Transaction> transactions,
                                                Date startDate,
                                                Date endDate,
                                                String interval) {
        // Create maps for income and expenses by time period
        Map<String, Double> incomeByPeriod = new TreeMap<>();
        Map<String, Double> expensesByPeriod = new TreeMap<>();

        // Format pattern based on interval
        String pattern;
        int calendarField;

        switch (interval) {
            case "day":
                pattern = "dd/MM";
                calendarField = Calendar.DAY_OF_MONTH;
                break;
            case "month":
                pattern = "MM/yyyy";
                calendarField = Calendar.MONTH;
                break;
            case "year":
                pattern = "yyyy";
                calendarField = Calendar.YEAR;
                break;
            default:
                pattern = "dd/MM/yyyy";
                calendarField = Calendar.DAY_OF_MONTH;
                break;
        }

        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.getDefault());

        // Initialize periods
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(endDate);

        while (!calendar.after(endCalendar)) {
            String periodKey = formatter.format(calendar.getTime());
            incomeByPeriod.put(periodKey, 0.0);
            expensesByPeriod.put(periodKey, 0.0);

            // Increment to next period
            calendar.add(calendarField, 1);
        }

        // Process transactions
        for (Transaction transaction : transactions) {
            String periodKey = formatter.format(transaction.getDate());
            double amount = Math.abs(transaction.getAmount());

            if (transaction.isIncome()) {
                double currentAmount = incomeByPeriod.getOrDefault(periodKey, 0.0);
                incomeByPeriod.put(periodKey, currentAmount + amount);
            } else {
                double currentAmount = expensesByPeriod.getOrDefault(periodKey, 0.0);
                expensesByPeriod.put(periodKey, currentAmount + amount);
            }
        }

        // Convert to lists for chart
        List<String> labels = new ArrayList<>(incomeByPeriod.keySet());
        List<Float> incomeValues = new ArrayList<>();
        List<Float> expenseValues = new ArrayList<>();

        for (String label : labels) {
            incomeValues.add(incomeByPeriod.get(label).floatValue());
            expenseValues.add(expensesByPeriod.get(label).floatValue());
        }

        return new TimeSeriesData(labels, incomeValues, expenseValues);
    }

    private void resetData() {
        income.setValue(0.0);
        expenses.setValue(0.0);
        balance.setValue(0.0);
        categoryExpenses.setValue(new HashMap<>());
        timeSeriesData.setValue(new TimeSeriesData());
    }

    // Getters for LiveData
    public LiveData<Double> getIncome() {
        return income;
    }

    public LiveData<Double> getExpenses() {
        return expenses;
    }

    public LiveData<Double> getBalance() {
        return balance;
    }

    public LiveData<Map<String, Double>> getCategoryExpenses() {
        return categoryExpenses;
    }

    public LiveData<TimeSeriesData> getTimeSeriesData() {
        return timeSeriesData;
    }
}