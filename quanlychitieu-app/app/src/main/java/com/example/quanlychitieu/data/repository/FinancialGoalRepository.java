package com.example.quanlychitieu.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.quanlychitieu.data.model.FinancialGoal;
import com.example.quanlychitieu.data.model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FinancialGoalRepository {
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_GOALS = "goals";
    private static final String TAG = "FinancialGoalRepo";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final MutableLiveData<List<FinancialGoal>> goalsLiveData;
    // Map để lưu trữ LiveData của từng mục tiêu riêng biệt
    private final Map<String, MutableLiveData<FinancialGoal>> goalLiveDataMap;

    private static FinancialGoalRepository instance;

    public static synchronized FinancialGoalRepository getInstance() {
        if (instance == null) {
            instance = new FinancialGoalRepository();
        }
        return instance;
    }

    private FinancialGoalRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        goalsLiveData = new MutableLiveData<>(new ArrayList<>());
        goalLiveDataMap = new HashMap<>();

        // Tải danh sách mục tiêu khi khởi tạo
        loadGoals();
    }

    public LiveData<List<FinancialGoal>> getGoals() {
        loadGoals();
        return goalsLiveData;
    }

    private void loadGoals() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            goalsLiveData.setValue(new ArrayList<>());
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_GOALS)
                .orderBy("endDate", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading goals", error);
                        return;
                    }

                    if (value != null) {
                        List<FinancialGoal> goals = new ArrayList<>();
                        for (QueryDocumentSnapshot document : value) {
                            FinancialGoal goal = documentToGoal(document);
                            goals.add(goal);

                            // Cập nhật LiveData riêng cho từng mục tiêu
                            updateGoalLiveData(goal);
                        }
                        goalsLiveData.setValue(goals);
                    }
                });
    }

    // Cập nhật LiveData riêng lẻ cho từng mục tiêu
    private void updateGoalLiveData(FinancialGoal goal) {
        if (goal == null || goal.getFirebaseId() == null) return;

        String goalId = goal.getFirebaseId();
        if (!goalLiveDataMap.containsKey(goalId)) {
            goalLiveDataMap.put(goalId, new MutableLiveData<>());
        }

        goalLiveDataMap.get(goalId).setValue(goal);
    }

    public LiveData<FinancialGoal> getGoalById(String goalId) {
        if (!goalLiveDataMap.containsKey(goalId)) {
            goalLiveDataMap.put(goalId, new MutableLiveData<>());

            fetchGoalById(goalId);
        } else {
            // Vẫn refresh để đảm bảo dữ liệu mới nhất
            fetchGoalById(goalId);
        }

        return goalLiveDataMap.get(goalId);
    }

    private void fetchGoalById(String goalId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_GOALS)
                .document(goalId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error fetching goal", error);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        FinancialGoal goal = documentSnapshotToGoal(documentSnapshot);
                        updateGoalLiveData(goal);
                    }
                });
    }

    public void addGoal(FinancialGoal goal) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Đặt userId cho mục tiêu
        goal.setUserId(currentUser.getUid());

        // Chuyển đổi thành Map
        Map<String, Object> goalMap = goalToMap(goal);

        // Thêm vào Firestore
        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_GOALS)
                .add(goalMap)
                .addOnSuccessListener(documentReference -> {
                    goal.setFirebaseId(documentReference.getId());
                    // Cập nhật LiveData cho mục tiêu mới
                    updateGoalLiveData(goal);
                });
    }

    public void updateGoal(FinancialGoal goal) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Đảm bảo có userId
        if (goal.getUserId() == null) {
            goal.setUserId(currentUser.getUid());
        }

        Map<String, Object> goalMap = goalToMap(goal);

        // Cập nhật LocalLiveData ngay lập tức để UI phản ứng nhanh hơn
        updateGoalLiveData(goal);

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_GOALS)
                .document(goal.getFirebaseId())
                .set(goalMap);
    }

    public void deleteGoal(String goalId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_GOALS)
                .document(goalId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Xóa khỏi map nếu tồn tại
                    goalLiveDataMap.remove(goalId);
                    loadGoals();
                });
    }

    /**
     * Thêm tiền vào mục tiêu
     */
    public void contributeToGoal(String goalId, double amount) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Lấy mục tiêu hiện tại từ Firestore
        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_GOALS)
                .document(goalId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        FinancialGoal goal = documentSnapshotToGoal(documentSnapshot);

                        // Cập nhật số tiền hiện tại
                        double newAmount = goal.getCurrentAmount() + amount;
                        goal.setCurrentAmount(newAmount);

                        // Kiểm tra nếu đã đạt mục tiêu
                        if (newAmount >= goal.getTargetAmount()) {
                            goal.setCompleted(true);
                        }

                        // Cập nhật UI ngay lập tức
                        updateGoalLiveData(goal);

                        // Lưu lại mục tiêu đã cập nhật lên Firestore
                        updateGoal(goal);

                        // Log để debug
                        Log.d(TAG, "Goal " + goal.getName() + " updated with new amount: " + newAmount);
                    }
                });
    }

    /**
     * Tạo giao dịch đóng góp vào mục tiêu và đồng thời cập nhật mục tiêu
     */
    public void createContributionTransaction(String goalId, double amount) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Lấy thông tin mục tiêu để tạo giao dịch
        if (goalLiveDataMap.containsKey(goalId) && goalLiveDataMap.get(goalId).getValue() != null) {
            FinancialGoal goal = goalLiveDataMap.get(goalId).getValue();
            createTransactionForGoal(goal, amount);
            // Không gọi contributeToGoal ở đây nữa
        } else {
            // Nếu không có trong cache, truy vấn từ Firestore
            db.collection(COLLECTION_USERS)
                    .document(currentUser.getUid())
                    .collection(COLLECTION_GOALS)
                    .document(goalId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            FinancialGoal goal = documentSnapshotToGoal(documentSnapshot);
                            createTransactionForGoal(goal, amount);
                            // Không gọi contributeToGoal ở đây nữa
                        }
                    });
        }
    }
    // Phương thức hỗ trợ tạo giao dịch
    private void createTransactionForGoal(FinancialGoal goal, double amount) {
        if (goal == null) return;

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        // Tạo giao dịch chi tiêu mới
        Transaction transaction = new Transaction();
        transaction.setFirebaseId(UUID.randomUUID().toString());
        transaction.setId(System.currentTimeMillis());
        transaction.setDescription("Đóng góp: " + goal.getName());
        transaction.setAmount(-Math.abs(amount)); // Luôn là số âm vì đây là chi tiêu
        transaction.setCategory("Tiết kiệm");
        transaction.setDate(new java.util.Date());
        transaction.setIncome(false);
        transaction.setNote("Đóng góp vào mục tiêu: " + goal.getName());
        transaction.setRepeat(false);
        transaction.setUserId(currentUser.getUid());
        transaction.setGoalContribution(true);
        transaction.setGoalId(goal.getFirebaseId());

        // Thêm giao dịch vào repository
        TransactionRepository.getInstance().addTransaction(transaction);

    }

    // Chuyển đổi từ document sang đối tượng FinancialGoal
    private FinancialGoal documentToGoal(QueryDocumentSnapshot document) {
        String firebaseId = document.getId();
        return extractGoalFromDocument(document, firebaseId);
    }

    private FinancialGoal documentSnapshotToGoal(DocumentSnapshot document) {
        String firebaseId = document.getId();
        return extractGoalFromDocument(document, firebaseId);
    }

    private FinancialGoal extractGoalFromDocument(DocumentSnapshot document, String firebaseId) {
        // Lấy các giá trị từ document, xử lý null an toàn
        long id = document.getLong("id") != null ? document.getLong("id") : System.currentTimeMillis();
        String userId = document.getString("userId");
        String name = document.getString("name") != null ? document.getString("name") : "";
        String description = document.getString("description") != null ? document.getString("description") : "";
        double targetAmount = document.getDouble("targetAmount") != null ? document.getDouble("targetAmount") : 0.0;
        double currentAmount = document.getDouble("currentAmount") != null ? document.getDouble("currentAmount") : 0.0;
        java.util.Date startDate = document.getDate("startDate") != null ? document.getDate("startDate") : new java.util.Date();
        java.util.Date endDate = document.getDate("endDate") != null ? document.getDate("endDate") : new java.util.Date();
        boolean completed = document.getBoolean("completed") != null ? document.getBoolean("completed") : false;
        String category = document.getString("category") != null ? document.getString("category") : "";

        // Tạo đối tượng FinancialGoal
        FinancialGoal goal = new FinancialGoal(id, userId, name, description, targetAmount,
                currentAmount, startDate, endDate, completed, category);
        goal.setFirebaseId(firebaseId);

        return goal;
    }

    private Map<String, Object> goalToMap(FinancialGoal goal) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", goal.getId());
        map.put("userId", goal.getUserId());
        map.put("name", goal.getName());
        map.put("description", goal.getDescription());
        map.put("targetAmount", goal.getTargetAmount());
        map.put("currentAmount", goal.getCurrentAmount());
        map.put("startDate", goal.getStartDate());
        map.put("endDate", goal.getEndDate());
        map.put("completed", goal.isCompleted());
        map.put("category", goal.getCategory());

        return map;
    }
}