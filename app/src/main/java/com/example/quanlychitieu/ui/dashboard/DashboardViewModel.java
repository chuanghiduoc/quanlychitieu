package com.example.quanlychitieu.ui.dashboard;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.data.repository.TransactionRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DashboardViewModel extends ViewModel {
    private static final String TAG = "DashboardViewModel";

    private final TransactionRepository repository;
    private final MutableLiveData<Double> income = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> expenses = new MutableLiveData<>(0.0);
    private final MutableLiveData<Double> balance = new MutableLiveData<>(0.0);
    private final MediatorLiveData<List<Transaction>> recentTransactions = new MediatorLiveData<>();

    public DashboardViewModel() {
        repository = TransactionRepository.getInstance();

        // Load data for the current month
        loadCurrentMonthData();

        // Load recent transactions for the current month
        loadRecentTransactions();
    }

    private void loadCurrentMonthData() {
        // Get date range for current month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();

        calendar = Calendar.getInstance(); // Reset to today
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endDate = calendar.getTime();


        // Get transactions for current month
        LiveData<List<Transaction>> monthlyTransactions =
                repository.getFilteredTransactions(startDate, endDate, "Tất cả danh mục", "Tất cả giao dịch");

        // Observe continuously instead of using MediatorLiveData with removal
        if (monthDataObserver != null) {
            monthlyTransactions.removeObserver(monthDataObserver);
        }

        monthDataObserver = transactions -> {
            if (transactions != null) {
                calculateFinancialSummary(transactions);
            } else {
                income.setValue(0.0);
                expenses.setValue(0.0);
                balance.setValue(0.0);
            }
        };

        monthlyTransactions.observeForever(monthDataObserver);
    }

    // Add this field to the class
    private Observer<List<Transaction>> monthDataObserver;

    // Clean up in onCleared
    @Override
    protected void onCleared() {
        if (monthDataObserver != null) {
            // Clean up any observers
        }
        super.onCleared();
    }

    private void calculateFinancialSummary(List<Transaction> transactions) {
        double totalIncome = 0;
        double totalExpenses = 0;

        for (Transaction transaction : transactions) {
            double originalAmount = transaction.getAmount();
            double amount = Math.abs(originalAmount);
            boolean isIncome = transaction.isIncome();

            if (isIncome) {
                totalIncome += amount;
                Log.d(TAG, "Added to income, new total: " + totalIncome);
            } else {
                totalExpenses += amount;
                Log.d(TAG, "Added to expenses, new total: " + totalExpenses);
            }
        }


        // Update the LiveData values
        income.setValue(totalIncome);
        expenses.setValue(totalExpenses);
        // Calculate balance as income minus expenses
        double calculatedBalance = totalIncome - totalExpenses;
        balance.setValue(calculatedBalance);

    }


    private void loadRecentTransactions() {
        // Get date range for current month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startDate = calendar.getTime();

        calendar = Calendar.getInstance(); // Reset to today
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endDate = calendar.getTime();

        // Get transactions for the current month
        LiveData<List<Transaction>> monthlyTransactions =
                repository.getFilteredTransactions(startDate, endDate, "Tất cả danh mục", "Tất cả giao dịch");

        recentTransactions.addSource(monthlyTransactions, transactions -> {
            if (transactions != null && !transactions.isEmpty()) {
                // Sort by date (newest first) and take the first 3
                List<Transaction> recent = transactions.stream()
                        .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
                        .limit(3)
                        .collect(Collectors.toList());

                recentTransactions.setValue(recent);
            } else {
                recentTransactions.setValue(new ArrayList<>());
            }

            // Remove the source after processing
            recentTransactions.removeSource(monthlyTransactions);
        });
    }

    public LiveData<Double> getIncome() {
        return income;
    }

    public LiveData<Double> getExpenses() {
        return expenses;
    }

    public LiveData<Double> getBalance() {
        return balance;
    }

    public LiveData<List<Transaction>> getRecentTransactions() {
        return recentTransactions;
    }

    // Method to refresh data
    public void refreshData() {
        loadCurrentMonthData();
        loadRecentTransactions();
    }
}
