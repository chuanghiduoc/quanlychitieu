package com.example.quanlychitieu.ui.dashboard;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.data.repository.BudgetRepository;
import com.example.quanlychitieu.data.repository.TransactionRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardViewModel extends ViewModel {
    private static final String TAG = "DashboardViewModel";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TRANSACTIONS = "transactions";

    public enum LoadingState { LOADING, SUCCESS, ERROR }
    private final MutableLiveData<LoadingState> loadingState = new MutableLiveData<>(LoadingState.LOADING);

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    // LiveData cho tất cả các thành phần trong dashboard
    private final MutableLiveData<Double> income = new MutableLiveData<>();
    private final MutableLiveData<Double> expenses = new MutableLiveData<>();
    private final MutableLiveData<Double> balance = new MutableLiveData<>();
    private final MutableLiveData<List<Transaction>> recentTransactions = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Double>> categoryExpensesData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Double>> categoryBudgetsData = new MutableLiveData<>();

    private ListenerRegistration transactionsListener;
    private Observer<List<Budget>> budgetObserver;

    // Biến để theo dõi xem dữ liệu đã được tải lần đầu chưa
    private boolean isInitialDataLoaded = false;

    public DashboardViewModel() {
        transactionRepository = TransactionRepository.getInstance();
        budgetRepository = BudgetRepository.getInstance();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Đặt trạng thái đang tải
        loadingState.setValue(LoadingState.LOADING);

        // Tải tất cả dữ liệu cùng một lúc
        loadAllDashboardData();
    }

    /**
     * Tải tất cả dữ liệu dashboard cùng một lúc
     */
    private void loadAllDashboardData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            // Đặt trạng thái lỗi nếu người dùng chưa đăng nhập
            loadingState.setValue(LoadingState.ERROR);
            return;
        }

        // Lấy ngày đầu và cuối tháng hiện tại
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfMonth = calendar.getTime();

        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        Date endOfMonth = calendar.getTime();

        // Hủy đăng ký listener cũ nếu có
        if (transactionsListener != null) {
            transactionsListener.remove();
        }

        // Đặt trạng thái đang tải
        loadingState.setValue(LoadingState.LOADING);

        // Lắng nghe thay đổi giao dịch theo thời gian thực
        transactionsListener = db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThanOrEqualTo("date", endOfMonth)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading transactions", error);
                        loadingState.setValue(LoadingState.ERROR);
                        return;
                    }

                    List<Transaction> transactions = new ArrayList<>();
                    if (snapshots != null && !snapshots.isEmpty()) {
                        // Chuyển đổi dữ liệu từ Firestore
                        for (QueryDocumentSnapshot document : snapshots) {
                            Transaction transaction = transactionRepository.documentToTransaction(document);
                            transactions.add(transaction);
                        }
                    }

                    // Xử lý dữ liệu ngay cả khi danh sách trống
                    processTransactions(transactions);

                    // Đặt trạng thái thành công
                    loadingState.setValue(LoadingState.SUCCESS);
                    isInitialDataLoaded = true;
                });

        // Tải dữ liệu ngân sách
        loadBudgetData();
    }

    /**
     * Xử lý danh sách giao dịch để cập nhật tất cả các thành phần
     */
    private void processTransactions(List<Transaction> transactions) {
        // Khởi tạo các giá trị mặc định
        double totalIncome = 0;
        double totalExpenses = 0;
        List<Transaction> recent = new ArrayList<>();
        Map<String, Double> spentByCategory = new HashMap<>();

        // Khởi tạo map với tất cả các danh mục
        List<String> expenseCategories = CategoryManager.getInstance().getExpenseCategories();
        for (String category : expenseCategories) {
            spentByCategory.put(category, 0.0);
        }

        if (transactions != null && !transactions.isEmpty()) {
            // 1. Cập nhật giao dịch gần đây (lấy 3 giao dịch mới nhất)
            recent = transactions.stream()
                    .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
                    .limit(3)
                    .collect(Collectors.toList());

            // 2. Tính toán tổng quan tài chính
            for (Transaction transaction : transactions) {
                double amount = Math.abs(transaction.getAmount());
                boolean isIncome = transaction.isIncome();

                if (isIncome) {
                    totalIncome += amount;
                } else {
                    totalExpenses += amount;

                    // 3. Cập nhật chi tiêu theo danh mục cho biểu đồ
                    String category = transaction.getCategory();
                    double currentAmount = spentByCategory.getOrDefault(category, 0.0);
                    spentByCategory.put(category, currentAmount + amount);
                }
            }
        }

        // Cập nhật tất cả LiveData cùng một lúc để tránh nhấp nháy
        income.setValue(totalIncome);
        expenses.setValue(totalExpenses);
        balance.setValue(totalIncome - totalExpenses);
        recentTransactions.setValue(recent);
        categoryExpensesData.setValue(spentByCategory);
    }

    /**
     * Tải dữ liệu ngân sách
     */
    private void loadBudgetData() {
        // Hủy đăng ký listener cũ nếu có
        if (budgetObserver != null) {
            budgetRepository.getActiveBudgets().removeObserver(budgetObserver);
        }

        // Lắng nghe thay đổi ngân sách
        budgetObserver = budgets -> {
            // Khởi tạo map với tất cả các danh mục
            List<String> expenseCategories = CategoryManager.getInstance().getExpenseCategories();
            Map<String, Double> budgetMap = new HashMap<>();
            for (String category : expenseCategories) {
                budgetMap.put(category, 0.0);
            }

            if (budgets != null) {
                // Cập nhật ngân sách cho từng danh mục
                for (Budget budget : budgets) {
                    budgetMap.put(budget.getCategory(), budget.getAmount());
                }
            }

            // Cập nhật LiveData
            categoryBudgetsData.setValue(budgetMap);
        };

        budgetRepository.getActiveBudgets().observeForever(budgetObserver);
    }

    // Getter cho trạng thái tải
    public LiveData<LoadingState> getLoadingState() {
        return loadingState;
    }

    // Getters cho các LiveData
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

    public LiveData<Map<String, Double>> getCategoryExpensesData() {
        return categoryExpensesData;
    }

    public LiveData<Map<String, Double>> getCategoryBudgetsData() {
        return categoryBudgetsData;
    }

    // Phương thức kiểm tra xem dữ liệu đã được tải chưa
    public boolean isDataLoaded() {
        return isInitialDataLoaded;
    }

    // Phương thức làm mới dữ liệu
    public void refreshData() {
        loadAllDashboardData();
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // Hủy đăng ký tất cả listener khi ViewModel bị hủy
        if (transactionsListener != null) {
            transactionsListener.remove();
        }

        if (budgetObserver != null) {
            budgetRepository.getActiveBudgets().removeObserver(budgetObserver);
        }
    }
}
