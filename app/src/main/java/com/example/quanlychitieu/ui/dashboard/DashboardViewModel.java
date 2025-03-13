package com.example.quanlychitieu.ui.dashboard;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.model.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DashboardViewModel extends ViewModel {

    private final MutableLiveData<Double> income = new MutableLiveData<>();
    private final MutableLiveData<Double> expenses = new MutableLiveData<>();
    private final MutableLiveData<Double> balance = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> recentTransactions = new MutableLiveData<>();

    public DashboardViewModel() {
        // Load initial data
        loadFinancialData();
        loadRecentTransactions();
    }

    private void loadFinancialData() {
        // Dữ liệu mẫu, trong ứng dụng thực tế sẽ lấy từ Repository
        double incomeValue = 5000000;
        double expensesValue = 3200000;
        double balanceValue = incomeValue - expensesValue;

        income.setValue(incomeValue);
        expenses.setValue(expensesValue);
        balance.setValue(balanceValue);
    }

    private void loadRecentTransactions() {
        // Dữ liệu mẫu, trong ứng dụng thực tế sẽ lấy từ Repository
        List<Transaction> transactions = new ArrayList<>();

        // Thêm một số giao dịch mẫu
        transactions.add(new Transaction(1, "Ăn trưa", -75000, "Ăn uống", new Date(), false, "Chi phí ăn uống", false));
        transactions.add(new Transaction(2, "Xăng xe", -150000, "Di chuyển", new Date(), false, "Chi phí đi lại", false));
        transactions.add(new Transaction(3, "Lương tháng 3", 5000000, "Thu nhập", new Date(), true, "Lương tháng", false));

        recentTransactions.setValue(transactions);
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
}
