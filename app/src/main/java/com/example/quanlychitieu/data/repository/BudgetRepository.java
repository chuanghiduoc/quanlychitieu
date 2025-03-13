package com.example.quanlychitieu.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.data.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetRepository {
    private static final String TAG = "BudgetRepository";
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_BUDGETS = "budgets";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<Budget>> budgetsLiveData;
    private final MutableLiveData<Double> totalBudgetLiveData;
    private final MutableLiveData<Double> totalSpentLiveData;

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
        budgetsLiveData = new MutableLiveData<>(new ArrayList<>());
        totalBudgetLiveData = new MutableLiveData<>(0.0);
        totalSpentLiveData = new MutableLiveData<>(0.0);
    }

    /**
     * Load all budgets for the current user
     */
    public LiveData<List<Budget>> getAllBudgets() {
        loadBudgets();
        return budgetsLiveData;
    }

    /**
     * Load active budgets for the current user (current month)
     */
    public LiveData<List<Budget>> getActiveBudgets() {
        loadActiveBudgets();
        return budgetsLiveData;
    }

    /**
     * Get a specific budget by ID
     */
    public LiveData<Budget> getBudgetById(String budgetId) {
        MutableLiveData<Budget> budgetLiveData = new MutableLiveData<>();

        // First check if we already have this budget in memory
        List<Budget> currentBudgets = budgetsLiveData.getValue();
        if (currentBudgets != null) {
            for (Budget budget : currentBudgets) {
                if (budget.getFirebaseId().equals(budgetId)) {
                    budgetLiveData.setValue(budget);
                    return budgetLiveData;
                }
            }
        }

        // If not found in memory, query Firestore
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return budgetLiveData;

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .document(budgetId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Budget budget = documentToBudget(documentSnapshot);
                        budgetLiveData.setValue(budget);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting budget", e);
                });

        return budgetLiveData;
    }

    /**
     * Get budgets for a specific category
     */
    public LiveData<List<Budget>> getBudgetsByCategory(String category) {
        MutableLiveData<List<Budget>> categoryBudgetsLiveData = new MutableLiveData<>();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            categoryBudgetsLiveData.setValue(new ArrayList<>());
            return categoryBudgetsLiveData;
        }

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Budget> budgets = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Budget budget = documentToBudget(document);
                        budgets.add(budget);
                    }
                    categoryBudgetsLiveData.setValue(budgets);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting budgets by category", e);
                    categoryBudgetsLiveData.setValue(new ArrayList<>());
                });

        return categoryBudgetsLiveData;
    }

    /**
     * Add a new budget
     */
    public void addBudget(Budget budget) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Ensure userId is set
        budget.setUserId(currentUser.getUid());

        // Convert budget to Map
        Map<String, Object> budgetMap = budgetToMap(budget);

        // Add to Firestore
        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .add(budgetMap)
                .addOnSuccessListener(documentReference -> {
                    budget.setFirebaseId(documentReference.getId());
                    loadBudgets(); // Refresh the budgets list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding budget", e);
                });
    }

    /**
     * Update an existing budget
     */
    public void updateBudget(Budget budget) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Ensure userId is set
        budget.setUserId(currentUser.getUid());

        // Convert budget to Map
        Map<String, Object> budgetMap = budgetToMap(budget);

        // Update in Firestore
        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .document(budget.getFirebaseId())
                .set(budgetMap)
                .addOnSuccessListener(aVoid -> {
                    loadBudgets(); // Refresh the budgets list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating budget", e);
                });
    }

    /**
     * Delete a budget
     */
    public void deleteBudget(String budgetId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .document(budgetId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    loadBudgets(); // Refresh the budgets list
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting budget", e);
                });
    }

    /**
     * Update a budget's spent amount when a transaction is added
     */
    public void updateBudgetSpentAmount(Transaction transaction) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || transaction.isIncome()) return; // Only update for expense transactions

        // Get the transaction category and amount
        String category = transaction.getCategory();
        double amount = Math.abs(transaction.getAmount());
        Date transactionDate = transaction.getDate();

        // Find active budgets for this category
        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = db.batch();
                    boolean budgetUpdated = false;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Budget budget = documentToBudget(document);

                        // Check if transaction date is within budget period
                        if (transactionDate.after(budget.getStartDate()) &&
                                transactionDate.before(budget.getEndDate())) {

                            // Update the spent amount
                            double newSpent = budget.getSpent() + amount;
                            document.getReference().update("spent", newSpent);

                            // Check if we need to update notification status
                            int newPercentage = (int)((newSpent / budget.getAmount()) * 100);
                            if (budget.isNotificationsEnabled() && !budget.isNotificationSent() &&
                                    newPercentage >= budget.getNotificationThreshold()) {
                                document.getReference().update("notificationSent", true);
                            }

                            budgetUpdated = true;
                        }
                    }

                    if (budgetUpdated) {
                        loadBudgets(); // Refresh budgets after update
                    }
                });
    }

    /**
     * Check for budgets that need notifications
     */
    public LiveData<List<Budget>> getBudgetsNeedingNotification() {
        MutableLiveData<List<Budget>> notificationBudgetsLiveData = new MutableLiveData<>();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            notificationBudgetsLiveData.setValue(new ArrayList<>());
            return notificationBudgetsLiveData;
        }

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .whereEqualTo("notificationsEnabled", true)
                .whereEqualTo("notificationSent", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Budget> budgetsToNotify = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Budget budget = documentToBudget(document);

                        // Check if budget threshold has been reached
                        if (budget.shouldSendNotification()) {
                            budgetsToNotify.add(budget);
                        }
                    }

                    notificationBudgetsLiveData.setValue(budgetsToNotify);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for budgets needing notification", e);
                    notificationBudgetsLiveData.setValue(new ArrayList<>());
                });

        return notificationBudgetsLiveData;
    }

    /**
     * Load all budgets for the current user
     */
    private void loadBudgets() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            budgetsLiveData.setValue(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .orderBy("startDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Budget> budgets = new ArrayList<>();
                    double totalBudget = 0;
                    double totalSpent = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Budget budget = documentToBudget(document);
                        budgets.add(budget);

                        // Update totals
                        totalBudget += budget.getAmount();
                        totalSpent += budget.getSpent();
                    }

                    budgetsLiveData.setValue(budgets);
                    totalBudgetLiveData.setValue(totalBudget);
                    totalSpentLiveData.setValue(totalSpent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading budgets", e);
                    budgetsLiveData.setValue(new ArrayList<>());
                });
    }

    /**
     * Load active budgets for the current month
     */
    private void loadActiveBudgets() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            budgetsLiveData.setValue(new ArrayList<>());
            return;
        }

        // Get current date
        Date now = new Date();

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_BUDGETS)
                .whereGreaterThanOrEqualTo("endDate", now) // End date in the future
                .whereLessThanOrEqualTo("startDate", now)  // Start date in the past
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Budget> budgets = new ArrayList<>();
                    double totalBudget = 0;
                    double totalSpent = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Budget budget = documentToBudget(document);
                        budgets.add(budget);

                        // Update totals
                        totalBudget += budget.getAmount();
                        totalSpent += budget.getSpent();
                    }

                    budgetsLiveData.setValue(budgets);
                    totalBudgetLiveData.setValue(totalBudget);
                    totalSpentLiveData.setValue(totalSpent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading active budgets", e);
                    budgetsLiveData.setValue(new ArrayList<>());
                });
    }

    /**
     * Get total budget amount
     */
    public LiveData<Double> getTotalBudget() {
        return totalBudgetLiveData;
    }

    /**
     * Get total spent amount
     */
    public LiveData<Double> getTotalSpent() {
        return totalSpentLiveData;
    }

    /**
     * Convert document to Budget object
     */
    private Budget documentToBudget(DocumentSnapshot document) {
        String firebaseId = document.getId();

        // Extract all fields with null safety
        long id = document.getLong("id") != null ? document.getLong("id") : System.currentTimeMillis();
        String userId = document.getString("userId");
        String category = document.getString("category");
        double amount = document.getDouble("amount") != null ? document.getDouble("amount") : 0.0;
        double spent = document.getDouble("spent") != null ? document.getDouble("spent") : 0.0;
        Date startDate = document.getDate("startDate");
        Date endDate = document.getDate("endDate");
        String note = document.getString("note");

        boolean notificationsEnabled = document.getBoolean("notificationsEnabled") != null ?
                document.getBoolean("notificationsEnabled") : true;
        int notificationThreshold = document.getLong("notificationThreshold") != null ?
                document.getLong("notificationThreshold").intValue() : 80;
        boolean notificationSent = document.getBoolean("notificationSent") != null ?
                document.getBoolean("notificationSent") : false;

        // Get recurring expense notifications map with null safety
        Map<String, Boolean> recurringExpenseNotifications = new HashMap<>();
        Map<String, Object> notificationsMap = document.get("recurringExpenseNotifications") != null ?
                (Map<String, Object>) document.get("recurringExpenseNotifications") : new HashMap<>();

        for (Map.Entry<String, Object> entry : notificationsMap.entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                recurringExpenseNotifications.put(entry.getKey(), (Boolean) entry.getValue());
            }
        }

        // Create budget object
        Budget budget = new Budget(
                id,
                userId,
                category,
                amount,
                spent,
                startDate,
                endDate,
                note,
                notificationsEnabled,
                notificationThreshold,
                notificationSent
        );
        budget.setFirebaseId(firebaseId);
        budget.setRecurringExpenseNotifications(recurringExpenseNotifications);

        return budget;
    }

    /**
     * Convert Budget to Map for Firestore
     */
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
