package com.example.quanlychitieu.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.Transaction;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
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

public class TransactionRepository {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_TRANSACTIONS = "transactions";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<Transaction>> transactionsLiveData;

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
    }

    public LiveData<List<Transaction>> getAllTransactions() {
        MutableLiveData<List<Transaction>> allTransactionsLiveData = new MutableLiveData<>();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            allTransactionsLiveData.setValue(new ArrayList<>());
            return allTransactionsLiveData;
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

                    // Update both LiveData objects
                    transactionsLiveData.setValue(transactions);
                    allTransactionsLiveData.setValue(transactions);
                })
                .addOnFailureListener(e -> {
                    // Handle failure
                    transactionsLiveData.setValue(new ArrayList<>());
                    allTransactionsLiveData.setValue(new ArrayList<>());
                });
        loadTransactions();
        return allTransactionsLiveData;
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
                    loadTransactions(); // 🛠 Cập nhật danh sách sau khi thêm
                });


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
                });
    }



    public Task<Void> deleteTransaction(String transactionId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return Tasks.forException(new Exception("User not logged in"));

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
                });
    }



    // Method for QueryDocumentSnapshot (from query results)
    private Transaction documentToTransaction(QueryDocumentSnapshot document) {
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

        // Set repeat information if available
        if (repeat) {
            transaction.setRepeatType(repeatType);
            transaction.setEndDate(endDate);
        }

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

        // Add repeat type and end date if transaction is recurring
        if (transaction.isRepeat()) {
            if (transaction.getRepeatType() != null) {
                map.put("repeatType", transaction.getRepeatType());
            }

            if (transaction.getEndDate() != null) {
                map.put("endDate", transaction.getEndDate());
            }
        }

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
