package com.example.quanlychitieu.ui.transactions;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.TransactionAdapter;
import com.example.quanlychitieu.adapter.helper.SwipeToDeleteCallback;
import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.databinding.FragmentTransactionsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener,
        SwipeToDeleteCallback.SwipeActionListener {

    private FragmentTransactionsBinding binding;
    private TransactionsViewModel viewModel;
    private TransactionAdapter adapter;
    private final Calendar fromDateCalendar = Calendar.getInstance();
    private final Calendar toDateCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private ArrayAdapter<String> allCategoriesAdapter;
    private ArrayAdapter<String> expenseCategoriesAdapter;
    private ArrayAdapter<String> incomeCategoriesAdapter;

    private void refreshCategoryAdapters() {
        initCategoryAdapters();

        // Cập nhật adapter cho dropdown danh mục hiện tại
        String currentTransactionType = binding.transactionTypeInput.getText().toString();
        updateCategoryFilterBasedOnTransactionType(currentTransactionType);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshTransactions();
        // Làm mới adapter danh mục khi quay lại fragment
        refreshCategoryAdapters();
        // Khôi phục lựa chọn dropdown
        restoreDropdownSelections();
    }
    private void restoreDropdownSelections() {
        // Get the current selections
        String currentTransactionType = binding.transactionTypeInput.getText().toString();

        // Reinitialize the adapters
        setupTransactionTypeFilter();

        // Update category filter based on current transaction type
        updateCategoryFilterBasedOnTransactionType(currentTransactionType);

        // Restore the selections
        binding.transactionTypeInput.setText("Tất cả giao dịch", false);
        binding.categoryFilterInput.setText("Tất cả danh mục", false);
        binding.dateToInput.setText(dateFormatter.format(toDateCalendar.getTime()));
    }

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

        // Add this line to set up the date pickers
        setupDatePickers();

        // Initialize category adapters only once
        if (allCategoriesAdapter == null) {
            initCategoryAdapters();
        }

        // Set up UI components
        setupTransactionTypeFilter();
        setupCategoryFilter();
        setupRecyclerView();
        setupAddTransactionButton();
        observeLoadingState();
        observeTransactions();

        // Apply default filters automatically
        applyDefaultFilters();
    }

    private void applyDefaultFilters() {
        // Set default dropdown values
        binding.transactionTypeInput.setText("Tất cả giao dịch", false);
        binding.categoryFilterInput.setText("Tất cả danh mục", false);

        // Apply filters with default values
        applyFilters();
    }


    private void observeLoadingState() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoading();
            } else {
                hideLoading();

                // Force UI update when loading completes
                List<Transaction> currentTransactions = adapter.getCurrentList();
                updateUIBasedOnTransactions(currentTransactions);
            }
        });
    }

    private void showLoading() {
        binding.loadingContainer.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
        binding.transactionsRecyclerView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        binding.loadingContainer.setVisibility(View.GONE);

        // Get current transactions to determine what to show
        List<Transaction> currentTransactions = adapter.getCurrentList();
        updateUIBasedOnTransactions(currentTransactions);
    }

    private void initCategoryAdapters() {
        // Tạo adapter cho tất cả danh mục
        List<String> allCategories = new ArrayList<>();
        allCategories.add("Tất cả danh mục");
        allCategories.addAll(CategoryManager.getInstance().getAllCategories());
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
        // Set from date to first day of current month
        fromDateCalendar.set(Calendar.DAY_OF_MONTH, 1);

        // Set to date to current day
        toDateCalendar.setTime(new Date()); // Today

        // Update the UI
        binding.dateFromInput.setText(dateFormatter.format(fromDateCalendar.getTime()));
        binding.dateToInput.setText(dateFormatter.format(toDateCalendar.getTime()));

        binding.dateFromInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        fromDateCalendar.set(Calendar.YEAR, year);
                        fromDateCalendar.set(Calendar.MONTH, month);
                        fromDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        binding.dateFromInput.setText(dateFormatter.format(fromDateCalendar.getTime()));

                        // Apply filters with the new date
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
                        toDateCalendar.set(Calendar.YEAR, year);
                        toDateCalendar.set(Calendar.MONTH, month);
                        toDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        binding.dateToInput.setText(dateFormatter.format(toDateCalendar.getTime()));

                        // Apply filters with the new date
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


        // Lắng nghe sự kiện khi người dùng chọn danh mục
        categoryInput.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = (String) parent.getItemAtPosition(position);
            if (!selectedCategory.equals("Tất cả danh mục")) {
                boolean inExpense = CategoryManager.getInstance().getExpenseCategories().contains(selectedCategory);
                boolean inIncome = CategoryManager.getInstance().getIncomeCategories().contains(selectedCategory);

                if (inExpense && !inIncome) {
                    binding.transactionTypeInput.setText("Chi tiêu", false);
                } else if (inIncome && !inExpense) {
                    binding.transactionTypeInput.setText("Thu nhập", false);
                }
            }
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

        // Don't set default text here, as it will be restored in restoreDropdownSelections

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
        // Show loading before applying filters
        showLoading();

        Date fromDate = fromDateCalendar.getTime();
        Date toDate = toDateCalendar.getTime();

        // Validate dates
        if (fromDate.after(toDate)) {
            Toast.makeText(requireContext(), "Ngày bắt đầu phải trước ngày kết thúc", Toast.LENGTH_SHORT).show();
            hideLoading();
            return;
        }

        String category = binding.categoryFilterInput.getText().toString();
        String transactionType = binding.transactionTypeInput.getText().toString();

        // Apply filters
        viewModel.applyFilter(fromDate, toDate, category, transactionType, transactions -> {
            // Update UI based on filtered results
            updateUIBasedOnTransactions(transactions);
        });
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this);
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRecyclerView.setAdapter(adapter);

        // Add swipe functionality
        SwipeToDeleteCallback swipeHandler = new SwipeToDeleteCallback(requireContext(), this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHandler);
        itemTouchHelper.attachToRecyclerView(binding.transactionsRecyclerView);
    }

    private void setupAddTransactionButton() {
        binding.fabAddTransaction.setOnClickListener(v -> {
            // Navigate to AddEditTransactionFragment
            Navigation.findNavController(v).navigate(
                    R.id.action_transactions_to_add_transaction
            );
        });
    }


    private void observeTransactions() {
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {

            // Always update the adapter with the new transactions, even if empty
            adapter.submitList(transactions);

            // Force UI update whenever we get new transaction data
            updateUIBasedOnTransactions(transactions);
        });
    }

    private void updateUIBasedOnTransactions(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.transactionsRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.transactionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupToolbar() {
        // Toolbar setup logic here
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        Bundle args = new Bundle();
        args.putString("transaction_id", transaction.getFirebaseId());
        Navigation.findNavController(requireView()).navigate(
                R.id.action_transactions_to_transaction_detail, args);
    }
    @Override
    public void onEditClick(Transaction transaction) {
        navigateToEditTransaction(transaction);
    }

    @Override
    public void onDeleteClick(Transaction transaction) {
        showDeleteConfirmationDialog(transaction);
    }

    // Implement SwipeActionListener methods
    @Override
    public void onDelete(int position) {
        Transaction transaction = adapter.getCurrentList().get(position);
        showDeleteConfirmationDialog(transaction);
    }

    @Override
    public void onEdit(int position) {
        Transaction transaction = adapter.getCurrentList().get(position);
        navigateToEditTransaction(transaction);
    }


    private void navigateToEditTransaction(Transaction transaction) {
        Bundle args = new Bundle();
        args.putString("transaction_id", transaction.getFirebaseId());
        Navigation.findNavController(requireView()).navigate(
                R.id.action_transactions_to_add_transaction, args);
    }

    private void showDeleteConfirmationDialog(Transaction transaction) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc chắn muốn xóa giao dịch này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Show loading while deleting
                    showLoading();
                    viewModel.deleteTransaction(transaction.getFirebaseId());
                    Toast.makeText(requireContext(), "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    // Refresh the adapter to reset the swiped item
                    adapter.notifyDataSetChanged();
                })
                .show();
    }
}