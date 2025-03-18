package com.example.quanlychitieu.data.repository;

import static android.content.ContentValues.TAG;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.data.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetRepository {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_BUDGETS = "budgets";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<Budget>> activeBudgetsLiveData;
    private final MutableLiveData<Double> totalBudgetLiveData;
    private final MutableLiveData<Double> totalSpentLiveData;
    private final MutableLiveData<Map<String, Double>> categorySpentAmountsLiveData;

    private static BudgetRepository instance;

    public static synchronized BudgetRepository getInstance() {
        if (instance == null) {
            instance = new BudgetRepository();
        }
        return instance;
    }

    private BudgetRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        activeBudgetsLiveData = new MutableLiveData<>(new ArrayList<>());
        totalBudgetLiveData = new MutableLiveData<>(0.0);
        totalSpentLiveData = new MutableLiveData<>(0.0);
        categorySpentAmountsLiveData = new MutableLiveData<>(new HashMap<>());

        // Lấy thông tin chi tiêu theo danh mục từ TransactionRepository
        TransactionRepository transactionRepository = TransactionRepository.getInstance();
        categorySpentAmountsLiveData.setValue(transactionRepository.getCategorySpentAmounts().getValue());

        // Theo dõi thay đổi chi tiêu theo danh mục
        transactionRepository.getCategorySpentAmounts().observeForever(categorySpentAmounts -> {
            categorySpentAmountsLiveData.setValue(categorySpentAmounts);
            updateBudgetsWithSpentAmounts();
            calculateTotalSpent();
        });

        // Tải ngân sách hiện tại
        loadActiveBudgets();
    }

    // Cập nhật số tiền đã chi tiêu cho mỗi ngân sách dựa trên dữ liệu giao dịch
    private void updateBudgetsWithSpentAmounts() {
        List<Budget> currentBudgets = activeBudgetsLiveData.getValue();
        Map<String, Double> categorySpentAmounts = categorySpentAmountsLiveData.getValue();

        if (currentBudgets == null || categorySpentAmounts == null) {
            return;
        }

        List<Budget> updatedBudgets = new ArrayList<>();

        for (Budget budget : currentBudgets) {
            String category = budget.getCategory();
            Double spentAmount = categorySpentAmounts.getOrDefault(category, 0.0);

            // Cập nhật số tiền đã chi trong ngân sách
            budget.setSpent(spentAmount);
            updatedBudgets.add(budget);
        }

        activeBudgetsLiveData.setValue(updatedBudgets);
    }

    // Tính tổng số tiền đã chi tiêu
    private void calculateTotalSpent() {
        Map<String, Double> categorySpentAmounts = categorySpentAmountsLiveData.getValue();
        if (categorySpentAmounts == null) {
            totalSpentLiveData.setValue(0.0);
            return;
        }

        double totalSpent = 0.0;
        for (Double amount : categorySpentAmounts.values()) {
            totalSpent += amount;
        }

        totalSpentLiveData.setValue(totalSpent);
    }

    public LiveData<List<Budget>> getActiveBudgets() {
        loadActiveBudgets();
        return activeBudgetsLiveData;
    }

    public LiveData<Double> getTotalBudget() {
        return totalBudgetLiveData;
    }

    public LiveData<Double> getTotalSpent() {
        return totalSpentLiveData;
    }

    public LiveData<Map<String, Double>> getCategorySpentAmounts() {
        return categorySpentAmountsLiveData;
    }

    private void loadActiveBudgets() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            activeBudgetsLiveData.setValue(new ArrayList<>());
            totalBudgetLiveData.setValue(0.0);
            return;
        }

        // Lấy tháng hiện tại
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfMonth = calendar.getTime();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        Date endOfMonth = calendar.getTime();

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .whereGreaterThanOrEqualTo("startDate", startOfMonth)
                .whereLessThanOrEqualTo("endDate", endOfMonth)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("BudgetRepo", "Error loading active budgets", error);
                        return;
                    }

                    if (value != null) {
                        List<Budget> budgets = new ArrayList<>();
                        double totalBudgetAmount = 0.0;

                        for (QueryDocumentSnapshot document : value) {
                            Budget budget = documentToBudget(document);
                            budgets.add(budget);
                            totalBudgetAmount += budget.getAmount();
                        }

                        // Cập nhật danh sách ngân sách
                        activeBudgetsLiveData.setValue(budgets);
                        totalBudgetLiveData.setValue(totalBudgetAmount);

                        // Cập nhật chi tiêu cho mỗi ngân sách
                        updateBudgetsWithSpentAmounts();
                    }
                });
    }

    public LiveData<Budget> getBudgetById(String budgetId) {
        MutableLiveData<Budget> budgetLiveData = new MutableLiveData<>();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return budgetLiveData;

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .document(budgetId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Budget budget = documentSnapshotToBudget(documentSnapshot);

                        // Cập nhật số tiền đã chi tiêu từ dữ liệu giao dịch
                        Map<String, Double> categorySpentAmounts = categorySpentAmountsLiveData.getValue();
                        if (categorySpentAmounts != null) {
                            Double spentAmount = categorySpentAmounts.getOrDefault(budget.getCategory(), 0.0);
                            budget.setSpent(spentAmount);
                        }

                        budgetLiveData.setValue(budget);
                    }
                });

        return budgetLiveData;
    }

    public void addBudget(Budget budget) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Đặt userId cho ngân sách
        budget.setUserId(currentUser.getUid());

        // Cập nhật số tiền đã chi tiêu từ dữ liệu giao dịch
        Map<String, Double> categorySpentAmounts = categorySpentAmountsLiveData.getValue();
        if (categorySpentAmounts != null) {
            Double spentAmount = categorySpentAmounts.getOrDefault(budget.getCategory(), 0.0);
            budget.setSpent(spentAmount);
        }

        // Chuyển đổi thành Map
        Map<String, Object> budgetMap = budgetToMap(budget);

        // Thêm vào Firestore
        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .add(budgetMap)
                .addOnSuccessListener(documentReference -> {
                    budget.setFirebaseId(documentReference.getId());
                    loadActiveBudgets();
                });
    }

    public void updateBudget(Budget budget) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Đảm bảo có userId
        if (budget.getUserId() == null) {
            budget.setUserId(currentUser.getUid());
        }

        // Cập nhật số tiền đã chi tiêu từ dữ liệu giao dịch
        Map<String, Double> categorySpentAmounts = categorySpentAmountsLiveData.getValue();
        if (categorySpentAmounts != null) {
            Double spentAmount = categorySpentAmounts.getOrDefault(budget.getCategory(), 0.0);
            budget.setSpent(spentAmount);
        }

        Map<String, Object> budgetMap = budgetToMap(budget);

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .document(budget.getFirebaseId())
                .set(budgetMap)
                .addOnSuccessListener(aVoid -> loadActiveBudgets());
    }

    public void deleteBudget(String budgetId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .document(budgetId)
                .delete()
                .addOnSuccessListener(aVoid -> loadActiveBudgets());
    }

    // Chuyển đổi từ document sang đối tượng Budget
    private Budget documentToBudget(QueryDocumentSnapshot document) {
        String firebaseId = document.getId();
        return extractBudgetFromDocument(document, firebaseId);
    }

    private Budget documentSnapshotToBudget(DocumentSnapshot document) {
        String firebaseId = document.getId();
        return extractBudgetFromDocument(document, firebaseId);
    }


    public LiveData<Budget> getBudgetByCategory(String category) {
        MutableLiveData<Budget> budgetLiveData = new MutableLiveData<>();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return budgetLiveData;

        // Lấy tháng hiện tại
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date startOfMonth = calendar.getTime();

        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        Date endOfMonth = calendar.getTime();

        // Đơn giản hóa truy vấn, chỉ lọc theo category
        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Lọc thủ công các budget thuộc tháng hiện tại
                        for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                            Date docStartDate = document.getDate("startDate");
                            Date docEndDate = document.getDate("endDate");

                            // Kiểm tra xem budget có thuộc tháng hiện tại không
                            if (docStartDate != null && docEndDate != null &&
                                    !docStartDate.after(endOfMonth) && !docEndDate.before(startOfMonth)) {

                                Budget budget = documentSnapshotToBudget(document);

                                // Cập nhật số tiền đã chi tiêu từ dữ liệu giao dịch
                                Map<String, Double> categorySpentAmounts = categorySpentAmountsLiveData.getValue();
                                if (categorySpentAmounts != null) {
                                    Double spentAmount = categorySpentAmounts.getOrDefault(category, 0.0);
                                    budget.setSpent(spentAmount);
                                }

                                budgetLiveData.setValue(budget);
                                return;
                            }
                        }
                    }
                    budgetLiveData.setValue(null);
                })
                .addOnFailureListener(e -> {
                    Log.e("BudgetRepository", "Error getting budget for category: " + category, e);
                    budgetLiveData.setValue(null);
                });

        return budgetLiveData;
    }

    /**
     * Cập nhật số tiền đã chi tiêu và reset trạng thái thông báo nếu cần
     * @param category Danh mục cần cập nhật
     * @param newSpentAmount Số tiền đã chi tiêu mới
     */
    public void updateBudgetSpentAmount(String category, double newSpentAmount) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        new Handler(Looper.getMainLooper()).post(() -> {
            getBudgetByCategory(category).observeForever(new Observer<Budget>() {
                @Override
                public void onChanged(Budget budget) {
                    // Hủy đăng ký observer sau khi nhận dữ liệu
                    getBudgetByCategory(category).removeObserver(this);

                    if (budget != null) {
                        Log.d(TAG, "Updating budget for " + category +
                                ": current spent=" + budget.getSpent() +
                                ", new spent=" + newSpentAmount);

                        // Luôn reset trạng thái thông báo khi chi tiêu thay đổi
                        budget.resetNotificationStatus();

                        // Cập nhật số tiền đã chi
                        budget.setSpent(newSpentAmount);

                        // Cập nhật ngân sách trong Firebase
                        updateBudget(budget);

                        Log.d(TAG, "Budget updated and notification status reset for " + category);
                    }
                }
            });
        });
    }
    private Budget extractBudgetFromDocument(DocumentSnapshot document, String firebaseId) {
        // Lấy các giá trị từ document, xử lý null an toàn
        long id = document.getLong("id") != null ? document.getLong("id") : System.currentTimeMillis();
        String userId = document.getString("userId");
        String category = document.getString("category") != null ? document.getString("category") : "";
        double amount = document.getDouble("amount") != null ? document.getDouble("amount") : 0.0;
        double spent = document.getDouble("spent") != null ? document.getDouble("spent") : 0.0;
        Date startDate = document.getDate("startDate") != null ? document.getDate("startDate") : new Date();
        Date endDate = document.getDate("endDate") != null ? document.getDate("endDate") : new Date();
        String note = document.getString("note");

        boolean notificationsEnabled = document.getBoolean("notificationsEnabled") != null ?
                document.getBoolean("notificationsEnabled") : true;
        int notificationThreshold = document.getLong("notificationThreshold") != null ?
                document.getLong("notificationThreshold").intValue() : 80;
        boolean notificationSent = document.getBoolean("notificationSent") != null ?
                document.getBoolean("notificationSent") : false;

        // Tạo đối tượng Budget
        Budget budget = new Budget(id, userId, category, amount, spent, startDate, endDate,
                note, notificationsEnabled, notificationThreshold, notificationSent);
        budget.setFirebaseId(firebaseId);

        // Lấy thông báo chi tiêu định kỳ nếu có
        Map<String, Boolean> recurringExpenseNotifications = (Map<String, Boolean>) document.get("recurringExpenseNotifications");
        if (recurringExpenseNotifications != null) {
            budget.setRecurringExpenseNotifications(recurringExpenseNotifications);
        }

        return budget;
    }
    private Map<String, Object> budgetToMap(Budget budget) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", budget.getId());
        map.put("userId", budget.getUserId());
        map.put("category", budget.getCategory());
        map.put("amount", budget.getAmount());
        map.put("spent", budget.getSpent());
        map.put("startDate", budget.getStartDate());
        map.put("endDate", budget.getEndDate());
        map.put("note", budget.getNote());
        map.put("notificationsEnabled", budget.isNotificationsEnabled());
        map.put("notificationThreshold", budget.getNotificationThreshold());
        map.put("notificationSent", budget.isNotificationSent());
        map.put("recurringExpenseNotifications", budget.getRecurringExpenseNotifications());

        return map;
    }
}
