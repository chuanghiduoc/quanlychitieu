package com.example.quanlychitieu.ui.settings;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.databinding.FragmentCategoryManagementBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class CategoryManagementFragment extends Fragment implements CategoryAdapter.CategoryActionListener {

    private FragmentCategoryManagementBinding binding;
    private CategoryAdapter expenseAdapter;
    private CategoryAdapter incomeAdapter;
    private boolean isShowingExpenseCategories = true;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoryManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo ProgressDialog
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Đang xử lý...");
        progressDialog.setCancelable(false);

        setupToolbar();
        setupTabLayout();
        setupRecyclerView();
        setupAddButton();

        // Tải lại danh mục từ Firebase
        CategoryManager.getInstance().refresh();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    private void setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isShowingExpenseCategories = tab.getPosition() == 0;
                updateCategoryList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        expenseAdapter = new CategoryAdapter(new ArrayList<>(), this, true);
        incomeAdapter = new CategoryAdapter(new ArrayList<>(), this, false);

        binding.categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.categoriesRecyclerView.addItemDecoration(
                new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        // Mặc định hiển thị danh mục chi tiêu
        binding.categoriesRecyclerView.setAdapter(expenseAdapter);
        updateCategoryList();
    }

    private void updateCategoryList() {
        if (isShowingExpenseCategories) {
            List<String> expenseCategories = CategoryManager.getInstance().getExpenseCategories();
            expenseAdapter.updateCategories(expenseCategories);
            binding.categoriesRecyclerView.setAdapter(expenseAdapter);
        } else {
            List<String> incomeCategories = CategoryManager.getInstance().getIncomeCategories();
            incomeAdapter.updateCategories(incomeCategories);
            binding.categoriesRecyclerView.setAdapter(incomeAdapter);
        }
    }

    private void setupAddButton() {
        binding.fabAddCategory.setOnClickListener(v -> {
            showAddCategoryDialog();
        });
    }

    private void showAddCategoryDialog() {
        String dialogTitle = isShowingExpenseCategories ?
                "Thêm danh mục chi tiêu" : "Thêm danh mục thu nhập";

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nhập tên danh mục");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(dialogTitle)
                .setView(input)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) {
                        addCategory(newCategory);
                    } else {
                        Toast.makeText(requireContext(), "Tên danh mục không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addCategory(String newCategory) {
        progressDialog.show();

        if (isShowingExpenseCategories) {
            CategoryManager.getInstance().addCustomExpenseCategory(newCategory)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        updateCategoryList();
                        Toast.makeText(requireContext(), "Đã thêm danh mục mới", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            CategoryManager.getInstance().addCustomIncomeCategory(newCategory)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        updateCategoryList();
                        Toast.makeText(requireContext(), "Đã thêm danh mục mới", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onEditCategory(String category, boolean isExpense) {
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(category);
        input.setSelectAllOnFocus(true);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sửa danh mục")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) {
                        updateCategory(category, newCategory, isExpense);
                    } else {
                        Toast.makeText(requireContext(), "Tên danh mục không được để trống", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateCategory(String oldCategory, String newCategory, boolean isExpense) {
        if (oldCategory.equals(newCategory)) {
            // Không có thay đổi
            return;
        }

        progressDialog.show();

        if (isExpense) {
            CategoryManager.getInstance().updateCustomExpenseCategory(oldCategory, newCategory)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        updateCategoryList();
                        Toast.makeText(requireContext(), "Đã cập nhật danh mục", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            CategoryManager.getInstance().updateCustomIncomeCategory(oldCategory, newCategory)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        updateCategoryList();
                        Toast.makeText(requireContext(), "Đã cập nhật danh mục", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onDeleteCategory(String category, boolean isExpense) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa danh mục")
                .setMessage("Bạn có chắc chắn muốn xóa danh mục này? Các giao dịch đã sử dụng danh mục này sẽ không bị ảnh hưởng.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteCategory(category, isExpense);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteCategory(String category, boolean isExpense) {
        progressDialog.show();

        if (isExpense) {
            CategoryManager.getInstance().removeCustomExpenseCategory(category)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        updateCategoryList();
                        Toast.makeText(requireContext(), "Đã xóa danh mục", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            CategoryManager.getInstance().removeCustomIncomeCategory(category)
                    .addOnSuccessListener(aVoid -> {
                        progressDialog.dismiss();
                        updateCategoryList();
                        Toast.makeText(requireContext(), "Đã xóa danh mục", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        binding = null;
    }
}
