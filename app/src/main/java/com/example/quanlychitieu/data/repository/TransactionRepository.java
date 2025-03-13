package com.example.quanlychitieu.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.quanlychitieu.data.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
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
        loadTransactions();
        return transactionsLiveData;
    }

    public LiveData<List<Transaction>> getFilteredTransactions(Date fromDate, Date toDate, String category, String type) {
        MutableLiveData<List<Transaction>> filteredData = new MutableLiveData<>();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            filteredData.setValue(new ArrayList<>());
            return filteredData;
        }

        Query query = db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .whereGreaterThanOrEqualTo("date", fromDate)
                .whereLessThanOrEqualTo("date", toDate);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<Transaction> transactions = new ArrayList<>();

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                Transaction transaction = documentToTransaction(document);

                // Áp dụng bộ lọc loại giao dịch
                boolean typeMatch = false;
                if (type.equals("Tất cả giao dịch")) {
                    typeMatch = true;
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


    public LiveData<Transaction> getTransactionById(String transactionId) {
        MutableLiveData<Transaction> transactionLiveData = new MutableLiveData<>();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return transactionLiveData;
        }

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .document(transactionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Use the generic documentSnapshotToTransaction method instead
                        Transaction transaction = documentSnapshotToTransaction(documentSnapshot);
                        transactionLiveData.setValue(transaction);
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
                    // Reload transactions
                    loadTransactions();
                });
    }

    public void updateTransaction(Transaction transaction) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        // Đảm bảo tính nhất quán của dữ liệu
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

        // Update in Firestore
        String documentId = transaction.getFirebaseId() != null ?
                transaction.getFirebaseId() :
                String.valueOf(transaction.getId());

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .document(documentId)
                .set(transactionMap)
                .addOnSuccessListener(aVoid -> {
                    // Reload transactions
                    loadTransactions();
                });
    }


    public void deleteTransaction(String transactionId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_TRANSACTIONS)
                .document(transactionId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Reload transactions
                    loadTransactions();
                });
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

        Transaction transaction = new Transaction(id, description, amount, category, date, isIncome, note, repeat);
        transaction.setFirebaseId(firebaseId);
        transaction.setUserId(userId);

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
