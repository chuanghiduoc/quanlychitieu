package com.example.quanlychitieu.data;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryManager {

    private static final String TAG = "CategoryManager";
    private static CategoryManager instance;
    private final List<String> defaultExpenseCategories;
    private final List<String> defaultIncomeCategories;
    private final List<String> customExpenseCategories;
    private final List<String> customIncomeCategories;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final String COLLECTION_USERS = "users";
    private static final String COLLECTION_CATEGORIES = "categories";
    private static final String DOCUMENT_USER_CATEGORIES = "user_categories";

    private CategoryManager() {
        // Danh sách mặc định các danh mục chi tiêu
        defaultExpenseCategories = new ArrayList<>(Arrays.asList(
                "Ăn uống",
                "Di chuyển",
                "Mua sắm",
                "Hóa đơn",
                "Khác"
        ));

        // Danh sách mặc định các danh mục thu nhập
        defaultIncomeCategories = new ArrayList<>(Arrays.asList(
                "Lương",
                "Thưởng",
                "Quà tặng",
                "Khác"
        ));

        // Khởi tạo danh sách danh mục tùy chỉnh
        customExpenseCategories = new ArrayList<>();
        customIncomeCategories = new ArrayList<>();

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Tải danh mục tùy chỉnh từ Firebase
        loadCustomCategories();
    }

    public static synchronized CategoryManager getInstance() {
        if (instance == null) {
            instance = new CategoryManager();
        }
        return instance;
    }

    /**
     * Tải danh mục tùy chỉnh từ Firebase
     */
    private void loadCustomCategories() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_CATEGORIES)
                .document(DOCUMENT_USER_CATEGORIES)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Lấy danh sách danh mục chi tiêu tùy chỉnh
                        List<String> expenseList = (List<String>) documentSnapshot.get("expense");
                        if (expenseList != null) {
                            customExpenseCategories.clear();
                            customExpenseCategories.addAll(expenseList);
                            Log.d(TAG, "Loaded custom expense categories: " + customExpenseCategories.size());
                        }

                        // Lấy danh sách danh mục thu nhập tùy chỉnh
                        List<String> incomeList = (List<String>) documentSnapshot.get("income");
                        if (incomeList != null) {
                            customIncomeCategories.clear();
                            customIncomeCategories.addAll(incomeList);
                            Log.d(TAG, "Loaded custom income categories: " + customIncomeCategories.size());
                        }
                    } else {
                        Log.d(TAG, "No custom categories found, creating empty document");
                        // Tạo document mới với danh sách rỗng
                        saveCustomCategories();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading custom categories", e);
                });
    }

    /**
     * Lưu danh mục tùy chỉnh vào Firebase
     */
    public Task<Void> saveCustomCategories() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Cannot save categories: User not logged in");
            return null;
        }

        Map<String, Object> categories = new HashMap<>();
        categories.put("expense", customExpenseCategories);
        categories.put("income", customIncomeCategories);

        return db.collection(COLLECTION_USERS)
                .document(currentUser.getUid())
                .collection(COLLECTION_CATEGORIES)
                .document(DOCUMENT_USER_CATEGORIES)
                .set(categories)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Custom categories saved successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving custom categories", e);
                });
    }

    /**
     * Thêm danh mục chi tiêu tùy chỉnh mới
     */
    public Task<Void> addCustomExpenseCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            Log.e(TAG, "Cannot add empty category");
            return null;
        }

        category = category.trim();

        if (!customExpenseCategories.contains(category) &&
                !defaultExpenseCategories.contains(category)) {
            customExpenseCategories.add(category);
            return saveCustomCategories();
        }

        Log.d(TAG, "Category already exists: " + category);
        return null;
    }

    /**
     * Thêm danh mục thu nhập tùy chỉnh mới
     */
    public Task<Void> addCustomIncomeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            Log.e(TAG, "Cannot add empty category");
            return null;
        }

        category = category.trim();

        if (!customIncomeCategories.contains(category) &&
                !defaultIncomeCategories.contains(category)) {
            customIncomeCategories.add(category);
            return saveCustomCategories();
        }

        Log.d(TAG, "Category already exists: " + category);
        return null;
    }

    /**
     * Cập nhật danh mục chi tiêu tùy chỉnh
     */
    public Task<Void> updateCustomExpenseCategory(String oldCategory, String newCategory) {
        if (newCategory == null || newCategory.trim().isEmpty()) {
            Log.e(TAG, "Cannot update to empty category");
            return null;
        }

        newCategory = newCategory.trim();

        if (defaultExpenseCategories.contains(oldCategory)) {
            Log.e(TAG, "Cannot update default category: " + oldCategory);
            return null;
        }

        if (defaultExpenseCategories.contains(newCategory) ||
                customExpenseCategories.contains(newCategory)) {
            Log.e(TAG, "Category already exists: " + newCategory);
            return null;
        }

        int index = customExpenseCategories.indexOf(oldCategory);
        if (index != -1) {
            customExpenseCategories.set(index, newCategory);
            return saveCustomCategories();
        }

        Log.e(TAG, "Category not found: " + oldCategory);
        return null;
    }

    /**
     * Cập nhật danh mục thu nhập tùy chỉnh
     */
    public Task<Void> updateCustomIncomeCategory(String oldCategory, String newCategory) {
        if (newCategory == null || newCategory.trim().isEmpty()) {
            Log.e(TAG, "Cannot update to empty category");
            return null;
        }

        newCategory = newCategory.trim();

        if (defaultIncomeCategories.contains(oldCategory)) {
            Log.e(TAG, "Cannot update default category: " + oldCategory);
            return null;
        }

        if (defaultIncomeCategories.contains(newCategory) ||
                customIncomeCategories.contains(newCategory)) {
            Log.e(TAG, "Category already exists: " + newCategory);
            return null;
        }

        int index = customIncomeCategories.indexOf(oldCategory);
        if (index != -1) {
            customIncomeCategories.set(index, newCategory);
            return saveCustomCategories();
        }

        Log.e(TAG, "Category not found: " + oldCategory);
        return null;
    }

    /**
     * Xóa danh mục chi tiêu tùy chỉnh
     */
    public Task<Void> removeCustomExpenseCategory(String category) {
        if (defaultExpenseCategories.contains(category)) {
            Log.e(TAG, "Cannot remove default category: " + category);
            return null;
        }

        if (customExpenseCategories.remove(category)) {
            return saveCustomCategories();
        }

        Log.e(TAG, "Category not found: " + category);
        return null;
    }

    /**
     * Xóa danh mục thu nhập tùy chỉnh
     */
    public Task<Void> removeCustomIncomeCategory(String category) {
        if (defaultIncomeCategories.contains(category)) {
            Log.e(TAG, "Cannot remove default category: " + category);
            return null;
        }

        if (customIncomeCategories.remove(category)) {
            return saveCustomCategories();
        }

        Log.e(TAG, "Category not found: " + category);
        return null;
    }

    /**
     * Kiểm tra xem danh mục có phải là danh mục chi tiêu mặc định không
     */
    public boolean isDefaultExpenseCategory(String category) {
        return defaultExpenseCategories.contains(category);
    }

    /**
     * Kiểm tra xem danh mục có phải là danh mục thu nhập mặc định không
     */
    public boolean isDefaultIncomeCategory(String category) {
        return defaultIncomeCategories.contains(category);
    }

    /**
     * Kiểm tra xem danh mục có phải là danh mục tùy chỉnh không
     */
    public boolean isCustomCategory(String category) {
        return customExpenseCategories.contains(category) ||
                customIncomeCategories.contains(category);
    }

    public List<String> getExpenseCategories() {
        List<String> allExpenseCategories = new ArrayList<>(defaultExpenseCategories);
        allExpenseCategories.addAll(customExpenseCategories);
        return allExpenseCategories;
    }

    public List<String> getIncomeCategories() {
        List<String> allIncomeCategories = new ArrayList<>(defaultIncomeCategories);
        allIncomeCategories.addAll(customIncomeCategories);
        return allIncomeCategories;
    }

    public List<String> getAllCategories() {
        List<String> allCategories = new ArrayList<>();
        allCategories.addAll(getExpenseCategories());

        // Add income categories that aren't already in the list (to avoid duplicates like "Khác")
        for (String category : getIncomeCategories()) {
            if (!allCategories.contains(category)) {
                allCategories.add(category);
            }
        }

        return allCategories;
    }

    public boolean isExpenseCategory(String category) {
        return defaultExpenseCategories.contains(category) || customExpenseCategories.contains(category);
    }

    public boolean isIncomeCategory(String category) {
        return defaultIncomeCategories.contains(category) || customIncomeCategories.contains(category);
    }

    public String getCategoryType(String category) {
        if (isExpenseCategory(category)) {
            return "Chi tiêu";
        } else if (isIncomeCategory(category)) {
            return "Thu nhập";
        } else {
            return "Tất cả giao dịch";
        }
    }

    // Phương thức để lấy danh sách danh mục chi tiêu mặc định
    public List<String> getDefaultExpenseCategories() {
        return new ArrayList<>(defaultExpenseCategories);
    }

    // Phương thức để lấy danh sách danh mục thu nhập mặc định
    public List<String> getDefaultIncomeCategories() {
        return new ArrayList<>(defaultIncomeCategories);
    }

    // Phương thức để lấy danh sách danh mục chi tiêu tùy chỉnh
    public List<String> getCustomExpenseCategories() {
        return new ArrayList<>(customExpenseCategories);
    }

    // Phương thức để lấy danh sách danh mục thu nhập tùy chỉnh
    public List<String> getCustomIncomeCategories() {
        return new ArrayList<>(customIncomeCategories);
    }

    /**
     * Làm mới dữ liệu từ Firestore
     */
    public void refresh() {
        loadCustomCategories();
    }
}
