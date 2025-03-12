package com.example.quanlychitieu.data;

import android.content.Context;
import com.example.quanlychitieu.R;
import java.util.Arrays;
import java.util.List;

public class CategoryManager {

    private static CategoryManager instance;
    private final List<String> expenseCategories;
    private final List<String> incomeCategories;

    private CategoryManager() {
        // Danh sách cố định các danh mục chi tiêu
        expenseCategories = Arrays.asList(
                "Ăn uống",
                "Di chuyển",
                "Mua sắm",
                "Hóa đơn",
                "Khác"
        );

        // Danh sách cố định các danh mục thu nhập
        incomeCategories = Arrays.asList(
                "Lương",
                "Thưởng",
                "Quà tặng",
                "Khác"
        );
    }

    public static synchronized CategoryManager getInstance() {
        if (instance == null) {
            instance = new CategoryManager();
        }
        return instance;
    }

    public List<String> getExpenseCategories() {
        return expenseCategories;
    }

    public List<String> getIncomeCategories() {
        return incomeCategories;
    }
}
