package com.example.quanlychitieu.data.repository;

import static android.content.ContentValues.TAG;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.quanlychitieu.MainActivity;
import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.data.model.Transaction;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionRepository {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TRANSACTIONS = "transactions";
    private static final String CHANNEL_ID = "budget_notification_channel";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<Transaction>> transactionsLiveData;
    private final MutableLiveData<List<Transaction>> currentMonthTransactionsLiveData;
    private final MutableLiveData<Map<String, Double>> categorySpentAmountsLiveData;

    private Context context;

    private static TransactionRepository instance;

    public static synchronized TransactionRepository getInstance() {
        if (instance == null) {
            instance = new TransactionRepository();
        }
        return instance;
    }

    private TransactionRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        transactionsLiveData = new MutableLiveData<>(new ArrayList<>());
        currentMonthTransactionsLiveData = new MutableLiveData<>(new ArrayList<>());
        categorySpentAmountsLiveData = new MutableLiveData<>(new HashMap<>());

        // Tải giao dịch của tháng hiện tại khi khởi tạo
        loadCurrentMonthTransactions();
    }

    /**
     * Lấy giao dịch của tháng hiện tại và tính toán chi tiêu theo danh mục
     */
    private void loadCurrentMonthTransactions() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            currentMonthTransactionsLiveData.setValue(new ArrayList<>());
            categorySpentAmountsLiveData.setValue(new HashMap<>());
            return;
        }

        // Tính ngày đầu và cuối tháng hiện tại
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

        // Truy vấn giao dịch trong tháng hiện tại
        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .whereGreaterThanOrEqualTo("date", startOfMonth)
                .whereLessThanOrEqualTo("date", endOfMonth)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("TransactionRepo", "Error loading current month transactions", error);
                        return;
                    }

                    if (value != null) {
                        List<Transaction> transactions = new ArrayList<>();
                        for (QueryDocumentSnapshot document : value) {
                            Transaction transaction = documentToTransaction(document);
                            transactions.add(transaction);
                        }

                        // Cập nhật danh sách giao dịch tháng hiện tại
                        currentMonthTransactionsLiveData.setValue(transactions);

                        // Tính toán chi tiêu theo danh mục
                        calculateCategorySpentAmounts(transactions);
                    }
                });
    }
    /**
     * Tính toán số tiền đã chi tiêu theo từng danh mục
     */
    private void calculateCategorySpentAmounts(List<Transaction> transactions) {
        Map<String, Double> spentByCategory = new HashMap<>();

        for (Transaction transaction : transactions) {
            // Chỉ tính các giao dịch chi tiêu (không phải thu nhập)
            if (!transaction.isIncome()) {
                String category = transaction.getCategory();
                // Đảm bảo amount là số dương cho chi tiêu
                double amount = Math.abs(transaction.getAmount());

                // Cộng dồn vào tổng của danh mục
                double currentAmount = spentByCategory.getOrDefault(category, 0.0);
                spentByCategory.put(category, currentAmount + amount);
            }
        }

        // Lấy giá trị trước đó
        Map<String, Double> previousSpentByCategory = categorySpentAmountsLiveData.getValue();

        // Cập nhật LiveData
        categorySpentAmountsLiveData.setValue(spentByCategory);

        // Kiểm tra thay đổi và cập nhật ngân sách
        if (previousSpentByCategory != null) {
            BudgetRepository budgetRepository = BudgetRepository.getInstance();

            for (Map.Entry<String, Double> entry : spentByCategory.entrySet()) {
                String category = entry.getKey();
                double newSpentAmount = entry.getValue();
                double previousSpentAmount = previousSpentByCategory.getOrDefault(category, 0.0);

                // Nếu chi tiêu thay đổi, cập nhật ngân sách
                if (Math.abs(newSpentAmount - previousSpentAmount) > 0.01) {
                    Log.d(TAG, "Spent amount changed for " + category +
                            ": " + previousSpentAmount + " -> " + newSpentAmount);
                    budgetRepository.updateBudgetSpentAmount(category, newSpentAmount);

                    // Kiểm tra ngân sách sau khi cập nhật chi tiêu
                    if (context != null && newSpentAmount > previousSpentAmount) {
                        checkBudgetThresholdAfterTransaction(category);
                    }
                }
            }
        }
    }

    /**
     * Lấy số tiền đã chi tiêu theo danh mục
     */
    public LiveData<Map<String, Double>> getCategorySpentAmounts() {
        return categorySpentAmountsLiveData;
    }



    public LiveData<List<Transaction>> getFilteredTransactions(Date fromDate, Date toDate, String category, String type) {
        MutableLiveData<List<Transaction>> filteredData = new MutableLiveData<>();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            filteredData.setValue(new ArrayList<>());
            return filteredData;
        }

        // Ensure the date range includes the full days
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(fromDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        fromDate = startCal.getTime();

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(toDate);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        toDate = endCal.getTime();

        // Query with date range filter
        Query query = db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .whereGreaterThanOrEqualTo("date", fromDate)
                .whereLessThanOrEqualTo("date", toDate)
                .orderBy("date", Query.Direction.DESCENDING);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Transaction> transactions = new ArrayList<>();

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Transaction transaction = documentToTransaction(document);

                // Áp dụng bộ lọc loại giao dịch
                boolean typeMatch = false;
                if (type.equals("Tất cả giao dịch")) {
                    if (!category.equals("Tất cả danh mục")) {
                        // Check if it's a valid category for this transaction's type
                        if (transaction.isIncome() &&
                                CategoryManager.getInstance().getIncomeCategories().contains(category)) {
                            typeMatch = true;
                        } else if (!transaction.isIncome() &&
                                CategoryManager.getInstance().getExpenseCategories().contains(category)) {
                            typeMatch = true;
                        }
                    } else {
                        // No category filter, show all
                        typeMatch = true;
                    }
                } else if (type.equals("Chi tiêu") && !transaction.isIncome()) {
                    typeMatch = true;
                } else if (type.equals("Thu nhập") && transaction.isIncome()) {
                    typeMatch = true;
                }

                // Áp dụng bộ lọc danh mục
                boolean categoryMatch = category.equals("Tất cả danh mục") ||
                        transaction.getCategory().equals(category);

                // Chỉ thêm giao dịch nếu thỏa mãn cả hai điều kiện
                if (typeMatch && categoryMatch) {
                    transactions.add(transaction);
                }
            }

            filteredData.setValue(transactions);
        }).addOnFailureListener(e -> {
            filteredData.setValue(new ArrayList<>());
        });

        return filteredData;
    }




    private void loadTransactions() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            transactionsLiveData.setValue(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> transactions = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Transaction transaction = documentToTransaction(document);
                        transactions.add(transaction);
                    }
                    transactionsLiveData.setValue(transactions);
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    transactionsLiveData.setValue(new ArrayList<>());
                });
    }


    public LiveData<Transaction> getTransactionById(String transactionId) {
        MutableLiveData<Transaction> transactionLiveData = new MutableLiveData<>();

        List<Transaction> currentTransactions = transactionsLiveData.getValue();
        if (currentTransactions != null) {
            for (Transaction transaction : currentTransactions) {
                if (transaction.getFirebaseId().equals(transactionId)) {
                    transactionLiveData.setValue(transaction);
                    return transactionLiveData;
                }
            }
        }

        // Nếu không tìm thấy, mới truy vấn Firestore
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return transactionLiveData;

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .document(transactionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        transactionLiveData.setValue(documentSnapshotToTransaction(documentSnapshot));
                    }
                });

        return transactionLiveData;
    }


    public void addTransaction(Transaction transaction) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }
        double amount = transaction.getAmount();
        if (!transaction.isIncome() && amount > 0) {
            // Nếu là chi tiêu nhưng số tiền là dương, chuyển thành số âm
            transaction.setAmount(-amount);
        } else if (transaction.isIncome() && amount < 0) {
            // Nếu là thu nhập nhưng số tiền là âm, chuyển thành số dương
            transaction.setAmount(Math.abs(amount));
        }

        // Convert transaction to Map
        Map<String, Object> transactionMap = transactionToMap(transaction);

        // Add to Firestore
        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .add(transactionMap)
                .addOnSuccessListener(documentReference -> {
                    transaction.setFirebaseId(documentReference.getId());
                    loadTransactions(); // Cập nhật danh sách sau khi thêm

                    // Cập nhật giao dịch tháng hiện tại nếu giao dịch thuộc tháng hiện tại
                    if (isCurrentMonth(transaction.getDate())) {
                        loadCurrentMonthTransactions();
                    }
                    // Kiểm tra ngân sách nếu là chi tiêu
                    if (!transaction.isIncome()) {
                        checkBudgetThresholdAfterTransaction(transaction.getCategory());
                    }
                });
    }


    public void setContext(Context context) {
        this.context = context;
        Log.d(TAG, "Context set for TransactionRepository");
    }

    private void checkBudgetThresholdAfterTransaction(String category) {
        // Kiểm tra xem context đã được đặt chưa
        if (context == null) {
            return;
        }

        // Sử dụng Handler để chạy trên main thread
        new Handler(Looper.getMainLooper()).post(() -> {
            // Lấy ngân sách cho danh mục này
            BudgetRepository budgetRepository = BudgetRepository.getInstance();
            budgetRepository.getBudgetByCategory(category).observeForever(new Observer<Budget>() {
                @Override
                public void onChanged(Budget budget) {
                    // Hủy đăng ký observer sau khi nhận dữ liệu
                    budgetRepository.getBudgetByCategory(category).removeObserver(this);

                    if (budget != null) {
                        // Kiểm tra xem thông báo có được bật cho ngân sách này không
                        if (!budget.isNotificationsEnabled()) {
                            return;
                        }

                        // Kiểm tra xem đã đạt ngưỡng thông báo chưa
                        int progressPercentage = budget.getProgressPercentage();
                        if (progressPercentage >= budget.getNotificationThreshold()) {
                            // Gửi thông báo ngay lập tức
                            sendBudgetNotification(budget);
                        } else {
                            Log.d(TAG, "Budget threshold not reached for " + category +
                                    ": " + progressPercentage + "% < " +
                                    budget.getNotificationThreshold() + "%");
                        }
                    } else {
                        Log.d(TAG, "No budget found for category: " + category);
                    }
                }
            });
        });
    }


    // Phương thức gửi thông báo ngân sách
    private void sendBudgetNotification(Budget budget) {
        try {
            // Tạo kênh thông báo nếu cần
            createNotificationChannelIfNeeded();

            // Tạo intent để mở ứng dụng khi nhấn vào thông báo
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Format số tiền
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            String formattedAmount = currencyFormat.format(budget.getAmount())
                    .replace("₫", "đ")
                    .replace(",", ".");
            String formattedSpent = currencyFormat.format(budget.getSpent())
                    .replace("₫", "đ")
                    .replace(",", ".");

            // Tạo thông báo với các cài đặt để hiển thị dạng heads-up
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("Cảnh báo ngân sách")
                    .setContentText("Bạn đã chi tiêu " + budget.getProgressPercentage() + "% ngân sách " + budget.getCategory())
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("Bạn đã chi tiêu " + formattedSpent + " trên tổng ngân sách " +
                                    formattedAmount + " cho danh mục " + budget.getCategory() +
                                    " (" + budget.getProgressPercentage() + "%)"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // Đặt độ ưu tiên cao
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Hiển thị nội dung trên màn hình khóa
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setVibrate(new long[]{0, 250, 250, 250}) // Thêm rung
                    .setLights(Color.RED, 1000, 500); // Thêm đèn LED nháy màu đỏ

            // Hiển thị thông báo
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                // Mỗi danh mục sẽ có ID thông báo riêng để tránh ghi đè
                int notificationId = budget.getCategory().hashCode();
                notificationManager.notify(notificationId, builder.build());
            } else {
                Log.e(TAG, "NotificationManager is null, couldn't send notification");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification for budget: " + budget.getCategory(), e);
        }
    }


    // Tạo kênh thông báo nếu cần
    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context != null) {
            try {
                CharSequence name = "Budget Alerts";
                String description = "Notifications for budget thresholds";
                int importance = NotificationManager.IMPORTANCE_HIGH; // Đặt độ ưu tiên cao

                NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
                channel.setDescription(description);
                channel.enableLights(true);
                channel.setLightColor(Color.RED);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 250, 250, 250});
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); // Hiển thị nội dung trên màn hình khóa

                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created successfully");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel", e);
            }
        }
    }


    public void updateTransaction(Transaction transaction) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        double amount = transaction.getAmount();
        transaction.setAmount(transaction.isIncome() ? Math.abs(amount) : -Math.abs(amount));

        Map<String, Object> transactionMap = transactionToMap(transaction);

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .document(transaction.getFirebaseId())
                .set(transactionMap)
                .addOnSuccessListener(aVoid -> {
                    loadTransactions();

                    // Cập nhật giao dịch tháng hiện tại nếu giao dịch thuộc tháng hiện tại
                    if (isCurrentMonth(transaction.getDate())) {
                        loadCurrentMonthTransactions();
                    }
                    if (!transaction.isIncome()) {
                        checkBudgetThresholdAfterTransaction(transaction.getCategory());
                    }
                });
    }

    public Task<Void> deleteTransaction(String transactionId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return Tasks.forException(new Exception("User not logged in"));

        // Trước khi xóa, kiểm tra xem giao dịch có thuộc tháng hiện tại không
        boolean isCurrentMonthTransaction = false;
        Transaction transactionToDelete = null;
        List<Transaction> currentTransactions = transactionsLiveData.getValue();
        if (currentTransactions != null) {
            for (Transaction t : currentTransactions) {
                if (t.getFirebaseId().equals(transactionId)) {
                    transactionToDelete = t;
                    break;
                }
            }
        }
        final Transaction finalTransactionToDelete = transactionToDelete;

        final boolean needsCurrentMonthUpdate = isCurrentMonthTransaction;

        return db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Update the local cache
                    List<Transaction> currentList = transactionsLiveData.getValue();
                    if (currentList != null) {
                        List<Transaction> updatedList = new ArrayList<>(currentList);
                        updatedList.removeIf(t -> t.getFirebaseId().equals(transactionId));
                        transactionsLiveData.setValue(updatedList);
                    }

                    // Cập nhật giao dịch tháng hiện tại nếu cần
                    if (needsCurrentMonthUpdate) {
                        loadCurrentMonthTransactions();
                        if (finalTransactionToDelete != null && !finalTransactionToDelete.isIncome()) {
                            checkBudgetThresholdAfterTransaction(finalTransactionToDelete.getCategory());
                        }
                    }

                });
    }

    /**
     * Kiểm tra xem một ngày có thuộc tháng hiện tại không
     */
    private boolean isCurrentMonth(Date date) {
        if (date == null) return false;

        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date);

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH);
    }

    // Method for QueryDocumentSnapshot (from query results)
    public Transaction documentToTransaction(QueryDocumentSnapshot document) {
        String firebaseId = document.getId();
        return extractTransactionFromDocument(document, firebaseId);
    }

    // Method for DocumentSnapshot (from direct document retrieval)
    private Transaction documentSnapshotToTransaction(DocumentSnapshot document) {
        String firebaseId = document.getId();
        return extractTransactionFromDocument(document, firebaseId);
    }

    // Helper method to extract transaction data from any document snapshot
    private Transaction extractTransactionFromDocument(DocumentSnapshot document, String firebaseId) {
        // Use getters that handle null values safely
        long id = document.getLong("id") != null ? document.getLong("id") : System.currentTimeMillis();
        String description = document.getString("description") != null ? document.getString("description") : "";
        double amount = document.getDouble("amount") != null ? document.getDouble("amount") : 0.0;
        String category = document.getString("category") != null ? document.getString("category") : "";
        Date date = document.getDate("date") != null ? document.getDate("date") : new Date();
        boolean isIncome = document.getBoolean("isIncome") != null ? document.getBoolean("isIncome") : false;
        String note = document.getString("note") != null ? document.getString("note") : "";
        boolean repeat = document.getBoolean("repeat") != null ? document.getBoolean("repeat") : false;
        String userId = document.getString("userId") != null ? document.getString("userId") : "";

        // Extract repeat type and end date
        String repeatType = document.getString("repeatType");
        Date endDate = document.getDate("endDate");

        String goalId = document.getString("goalId");
        boolean isGoalContribution = document.getBoolean("isGoalContribution") != null ?
                document.getBoolean("isGoalContribution") : false;

        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setDate(date);
        transaction.setIncome(isIncome);
        transaction.setNote(note);
        transaction.setRepeat(repeat);
        transaction.setUserId(userId);
        transaction.setFirebaseId(firebaseId);
        transaction.setRepeatType(repeatType);
        transaction.setEndDate(endDate);
        transaction.setGoalId(goalId);
        transaction.setGoalContribution(isGoalContribution);

        return transaction;
    }

    private Map<String, Object> transactionToMap(Transaction transaction) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", transaction.getId());
        map.put("description", transaction.getDescription());
        map.put("amount", transaction.getAmount());
        map.put("category", transaction.getCategory());
        map.put("date", transaction.getDate());
        map.put("isIncome", transaction.isIncome());
        map.put("note", transaction.getNote());
        map.put("repeat", transaction.isRepeat());
        map.put("repeatType", transaction.getRepeatType());
        map.put("endDate", transaction.getEndDate());
        map.put("goalId", transaction.getGoalId());
        map.put("isGoalContribution", transaction.isGoalContribution());

        // Add user ID if available
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            map.put("userId", currentUser.getUid());
        } else if (transaction.getUserId() != null) {
            map.put("userId", transaction.getUserId());
        }

        return map;
    }

}
