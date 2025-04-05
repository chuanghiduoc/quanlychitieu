package com.example.quanlychitieu.ui.transactions;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.FinancialGoal;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.data.repository.FinancialGoalRepository;
import com.example.quanlychitieu.data.repository.TransactionRepository;
import com.example.quanlychitieu.databinding.FragmentAddEditTransactionBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class AddEditTransactionFragment extends Fragment {

    private FragmentAddEditTransactionBinding binding;
    private final Calendar transactionCalendar = Calendar.getInstance();
    private final Calendar endDateCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private TransactionRepository repository;
    private FirebaseAuth auth;

    private String transactionId;
    private boolean isEditMode = false;
    private Spinner goalSpinner;
    private CheckBox contributeToGoalCheckbox;
    private LinearLayout goalSelectionLayout;
    private List<FinancialGoal> availableGoals;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditTransactionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = TransactionRepository.getInstance();
        auth = FirebaseAuth.getInstance();

        // Check if we're editing an existing transaction
        if (getArguments() != null) {
            transactionId = getArguments().getString("transaction_id");
            isEditMode = transactionId != null && !transactionId.isEmpty();

            if (isEditMode) {
                // Load existing transaction data
                loadExistingTransaction(transactionId);
                binding.toolbarTitle.setText("Sửa giao dịch");
            }
        }

        setupTransactionTypeRadioGroup();
        setupDatePickers();
        setupTimePicker();
        setupCategoryDropdown();
        setupRecurringOptions();
        setupAmountInputFormatting();
        setupSaveButton();
        setupToolbar();
        setupGoalContribution();

    }

    private void setupAmountInputFormatting() {
        int maxLength = 15;
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(maxLength);
        binding.amountInput.setFilters(filters);

        binding.amountInput.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    binding.amountInput.removeTextChangedListener(this);

                    // Remove all non-digit characters
                    String cleanString = s.toString().replaceAll("[^\\d]", "");

                    if (!cleanString.isEmpty()) {
                        try {
                            // Parse to a long (no decimals for VND)
                            long parsed = Long.parseLong(cleanString);

                            // Format with Vietnamese locale
                            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("vi", "VN"));
                            formatter.applyPattern("#,###");
                            formatter.setDecimalSeparatorAlwaysShown(false);
                            formatter.setGroupingUsed(true);
                            formatter.setGroupingSize(3);

                            String formatted = formatter.format(parsed);
                            // Replace comma with dot for Vietnamese display
                            formatted = formatted.replace(",", ".");

                            current = formatted;
                            s.replace(0, s.length(), formatted);
                        } catch (NumberFormatException e) {
                            s.clear();
                        }
                    }

                    binding.amountInput.addTextChangedListener(this);
                }
            }
        });
    }

    private void loadExistingTransaction(String id) {
        repository.getTransactionById(id).observe(getViewLifecycleOwner(), transaction -> {
            if (transaction != null) {
                populateFormWithTransactionData(transaction);
            }
        });
    }

    private void populateFormWithTransactionData(Transaction transaction) {
        // Set transaction type
        binding.expenseRadio.setChecked(!transaction.isIncome());
        binding.incomeRadio.setChecked(transaction.isIncome());

        // Format the amount properly with thousand separators
        double amount = Math.abs(transaction.getAmount());

        // Use DecimalFormat for better control
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("vi", "VN"));
        formatter.applyPattern("#,###");
        formatter.setDecimalSeparatorAlwaysShown(false);

        String formattedAmount = formatter.format(amount);
        // Replace comma with dot for Vietnamese display
        formattedAmount = formattedAmount.replace(",", ".");

        binding.amountInput.setText(formattedAmount);

        // Set category
        binding.categoryInput.setText(transaction.getCategory(), false);

        // Set date and time
        transactionCalendar.setTime(transaction.getDate());
        binding.dateInput.setText(dateFormatter.format(transaction.getDate()));
        binding.timeInput.setText(timeFormatter.format(transaction.getDate()));

        // Set note
        binding.noteInput.setText(transaction.getNote());

        // Set recurring options
        binding.repeatSwitch.setChecked(transaction.isRepeat());
        if (transaction.isRepeat()) {
            binding.repeatOptions.setVisibility(View.VISIBLE);

            // Set repeat type if available
            if (transaction.getRepeatType() != null && !transaction.getRepeatType().isEmpty()) {
                binding.repeatTypeInput.setText(transaction.getRepeatType(), false);
            }

            // Set end date if available
            if (transaction.getEndDate() != null) {
                endDateCalendar.setTime(transaction.getEndDate());
                binding.endDateInput.setText(dateFormatter.format(transaction.getEndDate()));
            }
        }
    }


    private void setupTransactionTypeRadioGroup() {
        binding.transactionTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isIncome = (checkedId == binding.incomeRadio.getId());
            updateCategoryDropdown(isIncome);
        });
    }

    private void setupDatePickers() {
        // Setup transaction date picker
        binding.dateInput.setText(dateFormatter.format(transactionCalendar.getTime()));
        binding.dateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        transactionCalendar.set(Calendar.YEAR, year);
                        transactionCalendar.set(Calendar.MONTH, month);
                        transactionCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        binding.dateInput.setText(dateFormatter.format(transactionCalendar.getTime()));
                    },
                    transactionCalendar.get(Calendar.YEAR),
                    transactionCalendar.get(Calendar.MONTH),
                    transactionCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Setup end date picker for recurring transactions
        endDateCalendar.add(Calendar.MONTH, 1); // Default to one month later
        binding.endDateInput.setText(dateFormatter.format(endDateCalendar.getTime()));
        binding.endDateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        endDateCalendar.set(Calendar.YEAR, year);
                        endDateCalendar.set(Calendar.MONTH, month);
                        endDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        binding.endDateInput.setText(dateFormatter.format(endDateCalendar.getTime()));
                    },
                    endDateCalendar.get(Calendar.YEAR),
                    endDateCalendar.get(Calendar.MONTH),
                    endDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
    }

    private void setupTimePicker() {
        binding.timeInput.setText(timeFormatter.format(transactionCalendar.getTime()));
        binding.timeInput.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        transactionCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        transactionCalendar.set(Calendar.MINUTE, minute);
                        binding.timeInput.setText(timeFormatter.format(transactionCalendar.getTime()));
                    },
                    transactionCalendar.get(Calendar.HOUR_OF_DAY),
                    transactionCalendar.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });
    }

    private void setupCategoryDropdown() {
        // Thiết lập ban đầu với danh mục chi tiêu (lựa chọn mặc định)
        updateCategoryDropdown(false);

        // Thêm nút thêm danh mục mới
        binding.addCategoryButton.setOnClickListener(v -> {
            showAddCategoryDialog();
        });
    }


    private void showAddCategoryDialog() {
        boolean isIncome = binding.incomeRadio.isChecked();
        String dialogTitle = isIncome ? "Thêm danh mục thu nhập" : "Thêm danh mục chi tiêu";

        // Tạo dialog với EditText để nhập tên danh mục mới
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Nhập tên danh mục");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(dialogTitle)
                .setView(input)
                .setPositiveButton("Thêm", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) {
                        // Thêm danh mục mới vào CategoryManager
                        if (isIncome) {
                            CategoryManager.getInstance().addCustomIncomeCategory(newCategory);
                        } else {
                            CategoryManager.getInstance().addCustomExpenseCategory(newCategory);
                        }

                        // Cập nhật dropdown với danh mục mới
                        updateCategoryDropdown(isIncome);

                        // Tự động chọn danh mục vừa thêm
                        binding.categoryInput.setText(newCategory, false);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateCategoryDropdown(boolean isIncome) {
        ArrayAdapter<String> categoryAdapter;
        if (isIncome) {
            categoryAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    CategoryManager.getInstance().getIncomeCategories()
            );
        } else {
            categoryAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    CategoryManager.getInstance().getExpenseCategories()
            );
        }

        AutoCompleteTextView categoryInput = binding.categoryInput;
        categoryInput.setAdapter(categoryAdapter);

        // Set default selection if adapter has items
        if (categoryAdapter.getCount() > 0 && (categoryInput.getText() == null || categoryInput.getText().toString().isEmpty())) {
            categoryInput.setText(categoryAdapter.getItem(0), false);
        }
    }

    private void setupRecurringOptions() {
        // Setup repeat frequency options
        String[] repeatOptions = {"Hàng ngày", "Hàng tuần", "Hàng tháng", "Hàng năm"};
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                repeatOptions
        );
        binding.repeatTypeInput.setAdapter(repeatAdapter);
        binding.repeatTypeInput.setText(repeatOptions[2], false); // Default to monthly

        // Show/hide recurring options based on switch
        binding.repeatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.repeatOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void setupSaveButton() {
        binding.saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveTransaction();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate amount
        String amountText = binding.amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountText)) {
            binding.amountLayout.setError("Vui lòng nhập số tiền");
            isValid = false;
        } else {
            try {
                // Remove formatting before parsing
                String cleanAmount = amountText.replace(".", "");
                double amount = Double.parseDouble(cleanAmount);
                if (amount <= 0) {
                    binding.amountLayout.setError("Số tiền phải lớn hơn 0");
                    isValid = false;
                } else {
                    binding.amountLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.amountLayout.setError("Số tiền không hợp lệ");
                isValid = false;
            }
        }

        // Validate category
        String category = binding.categoryInput.getText().toString().trim();
        if (TextUtils.isEmpty(category)) {
            binding.categoryLayout.setError("Vui lòng chọn danh mục");
            isValid = false;
        } else {
            binding.categoryLayout.setError(null);
        }

        // Check if user is signed in
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu giao dịch", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void saveTransaction() {
        // Get current user
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu giao dịch", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get all form values
        boolean isIncome = binding.incomeRadio.isChecked();

        // Parse the formatted amount
        String amountStr = binding.amountInput.getText().toString().trim();
        double amount;
        try {
            // Remove all dots (thousand separators) before parsing
            amountStr = amountStr.replace(".", "");
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            binding.amountLayout.setError("Số tiền không hợp lệ");
            return;
        }

        String category = binding.categoryInput.getText().toString().trim();
        Date date = transactionCalendar.getTime();
        String note = binding.noteInput.getText().toString().trim();
        boolean isRecurring = binding.repeatSwitch.isChecked();

        // Lấy kiểu lặp lại và ngày kết thúc (nếu có)
        String repeatType = "None";
        Date endDate = null;

        if (isRecurring) {
            repeatType = binding.repeatTypeInput.getText() != null
                    ? binding.repeatTypeInput.getText().toString()
                    : "None";
            endDate = endDateCalendar.getTime();
        }

        // Kiểm tra nếu đóng góp vào mục tiêu
        boolean isContributeToGoal = binding.contributeToGoalCheckbox.isChecked();
        String goalId = null;

        if (isContributeToGoal && availableGoals != null && !availableGoals.isEmpty()) {
            int selectedPosition = binding.goalSpinner.getSelectedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < availableGoals.size()) {
                goalId = availableGoals.get(selectedPosition).getFirebaseId();
            }
        }

        Transaction transaction;

        if (isEditMode) {
            transaction = new Transaction();
            transaction.setFirebaseId(transactionId);
            transaction.setId(System.currentTimeMillis());
            transaction.setDescription(category);
            transaction.setAmount(isIncome ? Math.abs(amount) : -Math.abs(amount)); // Đảm bảo số tiền âm cho chi tiêu
            transaction.setCategory(category);
            transaction.setDate(date);
            transaction.setIncome(isIncome);
            transaction.setNote(note);
            transaction.setRepeat(isRecurring);
            transaction.setRepeatType(repeatType);
            transaction.setEndDate(endDate);
            transaction.setUserId(currentUser.getUid());
            transaction.setGoalContribution(isContributeToGoal);
            transaction.setGoalId(goalId);

            repository.updateTransaction(transaction);
            Toast.makeText(requireContext(), "Giao dịch đã được cập nhật", Toast.LENGTH_SHORT).show();
        } else {
            transaction = new Transaction();
            transaction.setFirebaseId(UUID.randomUUID().toString());
            transaction.setId(System.currentTimeMillis());
            transaction.setDescription(category);
            transaction.setAmount(isIncome ? Math.abs(amount) : -Math.abs(amount)); // Đảm bảo số tiền âm cho chi tiêu
            transaction.setCategory(category);
            transaction.setDate(date);
            transaction.setIncome(isIncome);
            transaction.setNote(note);
            transaction.setRepeat(isRecurring);
            transaction.setRepeatType(repeatType);
            transaction.setEndDate(endDate);
            transaction.setUserId(currentUser.getUid());
            transaction.setGoalContribution(isContributeToGoal);
            transaction.setGoalId(goalId);

            repository.addTransaction(transaction);

            // Kiểm tra nếu có lặp lại
            if (isRecurring && !"None".equals(repeatType)) {
                generateRecurringTransactions(transaction);
            }

            Toast.makeText(requireContext(), "Giao dịch đã được thêm", Toast.LENGTH_SHORT).show();
        }

        Navigation.findNavController(requireView()).popBackStack();
    }


    private void generateRecurringTransactions(Transaction baseTransaction) {
        if (!baseTransaction.isRepeat() || baseTransaction.getRepeatType() == null) {
            return; // Không có lặp lại, thoát
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(baseTransaction.getDate());

        Date endDate = baseTransaction.getEndDate(); // Ngày kết thúc
        if (endDate == null) {
            // Nếu không có ngày kết thúc, mặc định tạo lặp lại trong 1 năm
            Calendar defaultEndCal = Calendar.getInstance();
            defaultEndCal.setTime(baseTransaction.getDate());
            defaultEndCal.add(Calendar.YEAR, 1);
            endDate = defaultEndCal.getTime();
        }

        // Xác định khoảng cách thời gian dựa trên loại lặp lại
        int calendarField;
        int amount = 1;

        switch (baseTransaction.getRepeatType().toLowerCase()) {
            case "hàng ngày":
                calendarField = Calendar.DAY_OF_MONTH;
                break;
            case "hàng tuần":
                calendarField = Calendar.WEEK_OF_YEAR;
                break;
            case "hàng tháng":
                calendarField = Calendar.MONTH;
                break;
            case "hàng năm":
                calendarField = Calendar.YEAR;
                break;
            default:
                return; // Loại lặp lại không hợp lệ
        }

        // Tạo các giao dịch lặp lại
        int maxRecurringTransactions = 100; // Giới hạn số lượng giao dịch lặp lại để tránh lặp vô hạn
        int count = 0;

        while (count < maxRecurringTransactions) {
            // Tăng ngày theo loại lặp lại
            calendar.add(calendarField, amount);
            Date nextDate = calendar.getTime();

            // Kiểm tra nếu đã vượt quá ngày kết thúc
            if (nextDate.after(endDate)) {
                break;
            }

            // Tạo giao dịch mới dựa trên giao dịch gốc
            Transaction newTransaction = new Transaction();
            newTransaction.setFirebaseId(UUID.randomUUID().toString());
            newTransaction.setId(System.currentTimeMillis() + count); // ID duy nhất
            newTransaction.setDescription(baseTransaction.getDescription());
            newTransaction.setAmount(baseTransaction.getAmount());
            newTransaction.setCategory(baseTransaction.getCategory());
            newTransaction.setDate(nextDate); // Ngày mới
            newTransaction.setIncome(baseTransaction.isIncome());
            newTransaction.setNote(baseTransaction.getNote());

            // Giữ lại thông tin lặp lại giống như giao dịch gốc
            newTransaction.setRepeat(baseTransaction.isRepeat());
            newTransaction.setRepeatType(baseTransaction.getRepeatType());
            newTransaction.setEndDate(baseTransaction.getEndDate());

            newTransaction.setUserId(baseTransaction.getUserId());

            // Lưu giao dịch vào cơ sở dữ liệu
            repository.addTransaction(newTransaction);

            count++;
        }
    }

    private void setupGoalContribution() {
        // Ánh xạ các view
        contributeToGoalCheckbox = binding.contributeToGoalCheckbox;
        goalSelectionLayout = binding.goalSelectionLayout;
        goalSpinner = binding.goalSpinner;

        // Mặc định ẩn phần chọn mục tiêu
        goalSelectionLayout.setVisibility(View.GONE);

        // Hiển thị/ẩn phần chọn mục tiêu dựa trên checkbox
        contributeToGoalCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            goalSelectionLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);

            // Nếu đóng góp vào mục tiêu, tự động chọn loại giao dịch là chi tiêu
            if (isChecked) {
                binding.expenseRadio.setChecked(true);
                // Tải danh sách mục tiêu
                loadAvailableGoals();
            }
        });

        // Chỉ cho phép đóng góp vào mục tiêu nếu là giao dịch chi tiêu
        binding.transactionTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isExpense = (checkedId == binding.expenseRadio.getId());
            contributeToGoalCheckbox.setEnabled(isExpense);

            if (!isExpense && contributeToGoalCheckbox.isChecked()) {
                contributeToGoalCheckbox.setChecked(false);
            }
        });
    }

    // Phương thức mới để tải danh sách mục tiêu
    private void loadAvailableGoals() {
        FinancialGoalRepository goalRepository = FinancialGoalRepository.getInstance();
        goalRepository.getGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null && !goals.isEmpty()) {
                // Lọc ra các mục tiêu chưa hoàn thành
                availableGoals = new ArrayList<>();
                for (FinancialGoal goal : goals) {
                    if (!goal.isCompleted()) {
                        availableGoals.add(goal);
                    }
                }

                if (!availableGoals.isEmpty()) {
                    // Tạo adapter cho spinner
                    List<String> goalNames = new ArrayList<>();
                    for (FinancialGoal goal : availableGoals) {
                        goalNames.add(goal.getName());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            goalNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    goalSpinner.setAdapter(adapter);
                } else {
                    // Không có mục tiêu nào khả dụng
                    contributeToGoalCheckbox.setChecked(false);
                    contributeToGoalCheckbox.setEnabled(false);
                    Toast.makeText(requireContext(), "Không có mục tiêu nào để đóng góp", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Không có mục tiêu nào
                contributeToGoalCheckbox.setChecked(false);
                contributeToGoalCheckbox.setEnabled(false);
            }
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Navigate back
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
