package com.example.quanlychitieu.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CategoryManager {

    private static CategoryManager instance;
    private final List<String> expenseCategories;
    private final List<String> incomeCategories;
    private final List<String> allCategories;

    private CategoryManager() {
        // Danh sách cố định các danh mục chi tiêu
        expenseCategories = new ArrayList<>(Arrays.asList(
                "Ăn uống",
                "Di chuyển",
                "Mua sắm",
                "Hóa đơn",
                "Khác"
        ));

        // Danh sách cố định các danh mục thu nhập
        incomeCategories = new ArrayList<>(Arrays.asList(
                "Lương",
                "Thưởng",
                "Quà tặng",
                "Khác"
        ));

        // Create combined list of all categories
        allCategories = new ArrayList<>();
        allCategories.addAll(expenseCategories);

        // Add income categories that aren't already in the list (to avoid duplicates like "Khác")
        for (String category : incomeCategories) {
            if (!allCategories.contains(category)) {
                allCategories.add(category);
            }
        }

    }

    public static synchronized CategoryManager getInstance() {
        if (instance == null) {
            instance = new CategoryManager();
        }
        return instance;
    }

    public List<String> getExpenseCategories() {
        return new ArrayList<>(expenseCategories); // Return a copy to prevent modification
    }

    public List<String> getIncomeCategories() {
        return new ArrayList<>(incomeCategories); // Return a copy to prevent modification
    }

    public List<String> getAllCategories() {
        return new ArrayList<>(allCategories); // Return a copy to prevent modification
    }

    public boolean isExpenseCategory(String category) {
        return expenseCategories.contains(category);
    }

    public boolean isIncomeCategory(String category) {
        return incomeCategories.contains(category);
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
}
