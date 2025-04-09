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
        // Lấy các lựa chọn hiện tại
        String currentTransactionType = binding.transactionTypeInput.getText().toString();

        // Khởi tạo lại các adapter
        setupTransactionTypeFilter();

        // Cập nhật bộ lọc danh mục dựa trên loại giao dịch hiện tại
        updateCategoryFilterBasedOnTransactionType(currentTransactionType);

        // Khôi phục các lựa chọn
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

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);

        setupToolbar();

        // Thêm dòng này để thiết lập bộ chọn ngày
        setupDatePickers();

        // Khởi tạo adapter danh mục chỉ một lần
        if (allCategoriesAdapter == null) {
            initCategoryAdapters();
        }

        // Thiết lập các thành phần UI
        setupTransactionTypeFilter();
        setupCategoryFilter();
        setupRecyclerView();
        setupAddTransactionButton();
        observeLoadingState();
        observeTransactions();

        // Áp dụng bộ lọc mặc định tự động
        applyDefaultFilters();
    }

    private void applyDefaultFilters() {
        // Đặt giá trị mặc định cho dropdown
        binding.transactionTypeInput.setText("Tất cả giao dịch", false);
        binding.categoryFilterInput.setText("Tất cả danh mục", false);

        // Áp dụng bộ lọc với các giá trị mặc định
        applyFilters();
    }


    private void observeLoadingState() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoading();
            } else {
                hideLoading();

                // Buộc cập nhật UI khi quá trình tải hoàn tất
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

        // Lấy danh sách giao dịch hiện tại để xác định nội dung hiển thị
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
        // Đặt ngày bắt đầu là ngày đầu tiên của tháng hiện tại
        fromDateCalendar.set(Calendar.DAY_OF_MONTH, 1);

        // Đặt ngày kết thúc là ngày hiện tại
        toDateCalendar.setTime(new Date()); // Hôm nay

        // Cập nhật giao diện người dùng
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

                        // Áp dụng bộ lọc với ngày mới
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

                        // Áp dụng bộ lọc với ngày mới
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

        // Không đặt văn bản mặc định ở đây, vì nó sẽ được khôi phục trong restoreDropdownSelections

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
        // Hiển thị trạng thái tải trước khi áp dụng bộ lọc
        showLoading();

        Date fromDate = fromDateCalendar.getTime();
        Date toDate = toDateCalendar.getTime();

        // Kiểm tra tính hợp lệ của ngày
        if (fromDate.after(toDate)) {
            Toast.makeText(requireContext(), "Ngày bắt đầu phải trước ngày kết thúc", Toast.LENGTH_SHORT).show();
            hideLoading();
            return;
        }

        String category = binding.categoryFilterInput.getText().toString();
        String transactionType = binding.transactionTypeInput.getText().toString();

        // Áp dụng bộ lọc
        viewModel.applyFilter(fromDate, toDate, category, transactionType, transactions -> {
            // Cập nhật UI dựa trên kết quả đã lọc
            updateUIBasedOnTransactions(transactions);
        });
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this);
        binding.transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.transactionsRecyclerView.setAdapter(adapter);

        // Thêm chức năng vuốt
        SwipeToDeleteCallback swipeHandler = new SwipeToDeleteCallback(requireContext(), this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeHandler);
        itemTouchHelper.attachToRecyclerView(binding.transactionsRecyclerView);
    }

    private void setupAddTransactionButton() {
        binding.fabAddTransaction.setOnClickListener(v -> {
            // Điều hướng đến AddEditTransactionFragment
            Navigation.findNavController(v).navigate(
                    R.id.action_transactions_to_add_transaction
            );
        });
    }


    private void observeTransactions() {
        viewModel.getTransactions().observe(getViewLifecycleOwner(), transactions -> {

            // Luôn cập nhật adapter với các giao dịch mới, ngay cả khi danh sách trống
            adapter.submitList(transactions);

            // Buộc cập nhật UI bất cứ khi nào nhận được dữ liệu giao dịch mới
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
        // Logic thiết lập Toolbar ở đây
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

    // Triển khai các phương thức của SwipeActionListener
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
                    // Hiển thị trạng thái tải trong khi xóa
                    showLoading();
                    viewModel.deleteTransaction(transaction.getFirebaseId());
                    Toast.makeText(requireContext(), "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", (dialog, which) -> {
                    // Làm mới adapter để đặt lại mục đã vuốt
                    adapter.notifyDataSetChanged();
                })
                .show();
    }
}