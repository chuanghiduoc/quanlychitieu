package com.example.quanlychitieu.ui.transactions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.TransactionAdapter;
import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.databinding.FragmentTransactionsBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionAdapter adapter;
    private final Calendar fromDateCalendar = Calendar.getInstance();
    private final Calendar toDateCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Khai báo các adapter cho danh mục
    private ArrayAdapter<String> allCategoriesAdapter;
    private ArrayAdapter<String> expenseCategoriesAdapter;
    private ArrayAdapter<String> incomeCategoriesAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);

        setupToolbar();
        setupDatePickers();

        // Khởi tạo các adapter danh mục
        initCategoryAdapters();

        // Thiết lập bộ lọc loại giao dịch trước, vì danh mục phụ thuộc vào loại
        setupTransactionTypeFilter();

        // Thiết lập bộ lọc danh mục (ban đầu hiển thị tất cả danh mục)
        setupCategoryFilter();

        setupRecyclerView();
        setupAddTransactionButton();

        // Observe transactions data
        observeTransactions();
    }

    private void initCategoryAdapters() {
        // Tạo adapter cho tất cả danh mục
        List<String> allCategories = new ArrayList<>();
        allCategories.add("Tất cả danh mục");
        allCategories.addAll(CategoryManager.getInstance().getExpenseCategories());
        allCategories.addAll(CategoryManager.getInstance().getIncomeCategories());
        allCategoriesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                allCategories
        );

        // Tạo adapter cho danh mục chi tiêu
        List<String> expenseCategories = new ArrayList<>();
        expenseCategories.add("Tất cả danh mục");
        expenseCategories.addAll(CategoryManager.getInstance().getExpenseCategories());
        expenseCategoriesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                expenseCategories
        );

        // Tạo adapter cho danh mục thu nhập
        List<String> incomeCategories = new ArrayList<>();
        incomeCategories.add("Tất cả danh mục");
        incomeCategories.addAll(CategoryManager.getInstance().getIncomeCategories());
        incomeCategoriesAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                incomeCategories
        );
    }

    private void setupDatePickers() {
        // Giữ nguyên code
        fromDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
        binding.dateFromInput.setText(dateFormatter.format(fromDateCalendar.getTime()));
        binding.dateToInput.setText(dateFormatter.format(toDateCalendar.getTime()));

        binding.dateFromInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        fromDateCalendar.set(year, month, dayOfMonth);
                        binding.dateFromInput.setText(dateFormatter.format(fromDateCalendar.getTime()));
                        applyFilters();
                    },
                    fromDateCalendar.get(Calendar.YEAR),
                    fromDateCalendar.get(Calendar.MONTH),
                    fromDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        binding.dateToInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        toDateCalendar.set(year, month, dayOfMonth);
                        binding.dateToInput.setText(dateFormatter.format(toDateCalendar.getTime()));
                        applyFilters();
                    },
                    toDateCalendar.get(Calendar.YEAR),
                    toDateCalendar.get(Calendar.MONTH),
                    toDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupCategoryFilter() {
        // Ban đầu, thiết lập adapter mặc định (tất cả danh mục)
        AutoCompleteTextView categoryInput = binding.categoryFilterInput;
        categoryInput.setAdapter(allCategoriesAdapter);
        categoryInput.setText("Tất cả danh mục", false);

        // Lắng nghe sự kiện khi người dùng chọn danh mục
        categoryInput.setOnItemClickListener((parent, view, position, id) -> {
            // Áp dụng bộ lọc ngay lập tức sau khi chọn danh mục
            applyFilters();
        });
    }



    private void setupTransactionTypeFilter() {
        String[] transactionTypes = {"Tất cả giao dịch", "Chi tiêu", "Thu nhập"};

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                transactionTypes
        );

        binding.transactionTypeInput.setAdapter(typeAdapter);
        binding.transactionTypeInput.setText(transactionTypes[0], false);

        binding.transactionTypeInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedType = (String) parent.getItemAtPosition(position);

            // Cập nhật danh mục dựa trên loại giao dịch đã chọn
            updateCategoryFilterBasedOnTransactionType(selectedType);

            // Áp dụng bộ lọc ngay lập tức sau khi cập nhật danh mục
            applyFilters();
        });
    }



    // Phương thức mới để cập nhật bộ lọc danh mục dựa trên loại giao dịch
    private void updateCategoryFilterBasedOnTransactionType(String transactionType) {
        AutoCompleteTextView categoryInput = binding.categoryFilterInput;
        String currentCategory = categoryInput.getText().toString();

        // Tạm thời tắt sự kiện lắng nghe để tránh gọi applyFilters() nhiều lần
        AdapterView.OnItemClickListener originalListener = categoryInput.getOnItemClickListener();
        categoryInput.setOnItemClickListener(null);

        switch (transactionType) {
            case "Chi tiêu":
                // Đặt adapter cho danh mục chi tiêu
                categoryInput.setAdapter(expenseCategoriesAdapter);

                // Kiểm tra xem danh mục hiện tại có phù hợp với loại giao dịch không
                if (!containsCategory(CategoryManager.getInstance().getExpenseCategories(), currentCategory)
                        && !currentCategory.equals("Tất cả danh mục")) {
                    categoryInput.setText("Tất cả danh mục", false);
                } else {
                    // Giữ nguyên giá trị nếu hợp lệ
                    categoryInput.setText(currentCategory, false);
                }
                break;

            case "Thu nhập":
                // Đặt adapter cho danh mục thu nhập
                categoryInput.setAdapter(incomeCategoriesAdapter);

                // Kiểm tra xem danh mục hiện tại có phù hợp với loại giao dịch không
                if (!containsCategory(CategoryManager.getInstance().getIncomeCategories(), currentCategory)
                        && !currentCategory.equals("Tất cả danh mục")) {
                    categoryInput.setText("Tất cả danh mục", false);
                } else {
                    // Giữ nguyên giá trị nếu hợp lệ
                    categoryInput.setText(currentCategory, false);
                }
                break;

            default: // "Tất cả giao dịch"
                // Đặt adapter cho tất cả danh mục
                categoryInput.setAdapter(allCategoriesAdapter);
                categoryInput.setText(currentCategory, false); // Giữ nguyên giá trị đã chọn
                break;
        }

        // Khôi phục sự kiện lắng nghe
        categoryInput.setOnItemClickListener(originalListener);
    }


    // Phương thức hỗ trợ kiểm tra xem danh mục có tồn tại trong danh sách không
    private boolean containsCategory(List<String> categories, String category) {
        for (String cat : categories) {
            if (cat.equals(category)) {
                return true;
            }
        }
        return false;
    }


    private void applyFilters() {
        Date fromDate = fromDateCalendar.getTime();
        Date toDate = toDateCalendar.getTime();
        String category = binding.categoryFilterInput.getText().toString();
        String transactionType = binding.transactionTypeInput.getText().toString();

        Log.d("TransactionsFragment", "Applying filters - Type: " + transactionType +
                ", Category: " + category +
                ", From: " + dateFormatter.format(fromDate) +
                ", To: " + dateFormatter.format(toDate));

        if (fromDate.after(toDate)) {
            Toast.makeText(requireContext(), "Ngày bắt đầu phải trước ngày kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra tính hợp lệ của bộ lọc danh mục và loại giao dịch
        if (!transactionType.equals("Tất cả giao dịch")) {
            if (transactionType.equals("Chi tiêu") &&
                    !CategoryManager.getInstance().getExpenseCategories().contains(category) &&
                    !category.equals("Tất cả danh mục")) {
                // Nếu đang lọc chi tiêu nhưng chọn danh mục thu nhập
                Toast.makeText(requireContext(), "Danh mục không phù hợp với loại giao dịch", Toast.LENGTH_SHORT).show();
                binding.categoryFilterInput.setText("Tất cả danh mục", false);
                category = "Tất cả danh mục";
            } else if (transactionType.equals("Thu nhập") &&
                    !CategoryManager.getInstance().getIncomeCategories().contains(category) &&
                    !category.equals("Tất cả danh mục")) {
                // Nếu đang lọc thu nhập nhưng chọn danh mục chi tiêu
                Toast.makeText(requireContext(), "Danh mục không phù hợp với loại giao dịch", Toast.LENGTH_SHORT).show();
                binding.categoryFilterInput.setText("Tất cả danh mục", false);
                category = "Tất cả danh mục";
            }
        }

        // Áp dụng bộ lọc trong ViewModel
        viewModel.applyFilter(fromDate, toDate, category, transactionType);
    }



    // Các phương thức còn lại giữ nguyên
    private void setupRecyclerView() {
        adapter = new TransactionAdapter();
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRecyclerView.setAdapter(adapter);
    }

    private void setupAddTransactionButton() {
        binding.fabAddTransaction.setOnClickListener(v -> {
            // Navigate to AddEditTransactionFragment
            Navigation.findNavController(v).navigate(
                    R.id.action_transactions_to_add_transaction
            );
        });
    }


    // Trong TransactionsFragment
    private void observeTransactions() {
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {

            adapter.submitList(transactions);

            // Show/hide empty state
            if (transactions.isEmpty()) {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.transactionsRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyState.setVisibility(View.GONE);
                binding.transactionsRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }


    private void setupToolbar() {
        // Toolbar setup logic here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}