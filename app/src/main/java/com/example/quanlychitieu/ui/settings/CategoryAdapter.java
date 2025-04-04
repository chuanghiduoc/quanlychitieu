package com.example.quanlychitieu.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.CategoryManager;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<String> categories;
    private final CategoryActionListener actionListener;
    private final boolean isExpenseCategory; // Thêm trường này để phân biệt loại danh mục

    public interface CategoryActionListener {
        void onEditCategory(String category, boolean isExpense);
        void onDeleteCategory(String category, boolean isExpense);
    }

    public CategoryAdapter(List<String> categories, CategoryActionListener actionListener, boolean isExpenseCategory) {
        this.categories = categories;
        this.actionListener = actionListener;
        this.isExpenseCategory = isExpenseCategory;
    }

    public void updateCategories(List<String> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        String category = categories.get(position);
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView categoryNameTextView;
        private final ImageButton editButton;
        private final ImageButton deleteButton;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameTextView = itemView.findViewById(R.id.category_name);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        public void bind(String category) {
            categoryNameTextView.setText(category);

            // Kiểm tra xem đây có phải là danh mục mặc định không
            boolean isDefault = isExpenseCategory ?
                    CategoryManager.getInstance().isDefaultExpenseCategory(category) :
                    CategoryManager.getInstance().isDefaultIncomeCategory(category);

            // Ẩn/hiện nút sửa và xóa tùy thuộc vào loại danh mục
            if (isDefault) {
                // Danh mục mặc định không thể sửa hoặc xóa
                editButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);
            } else {
                // Danh mục tùy chỉnh có thể sửa và xóa
                editButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);

                editButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onEditCategory(category, isExpenseCategory);
                    }
                });

                deleteButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onDeleteCategory(category, isExpenseCategory);
                    }
                });
            }
        }
    }
}
