package com.example.quanlychitieu.ui.transactions;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.data.repository.TransactionRepository;
import com.example.quanlychitieu.databinding.FragmentAddEditTransactionBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
        setupSaveButton();
        setupToolbar();
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

        // Set amount
        binding.amountInput.setText(String.valueOf(transaction.getAmount()));

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
            // Would need to set repeat type and end date if those were in the model
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
        // Initially setup with expense categories (default selection)
        updateCategoryDropdown(false);
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
        if (categoryAdapter.getCount() > 0) {
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
                double amount = Double.parseDouble(amountText);
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
        double amount = Double.parseDouble(binding.amountInput.getText().toString().trim());
        String category = binding.categoryInput.getText().toString().trim();
        Date date = transactionCalendar.getTime();
        String note = binding.noteInput.getText().toString().trim();
        boolean isRecurring = binding.repeatSwitch.isChecked();

        // Create transaction object
        Transaction transaction;
        if (isEditMode) {
            // Update existing transaction
            transaction = new Transaction();
            transaction.setFirebaseId(transactionId);
            transaction.setId(System.currentTimeMillis());
            transaction.setDescription(category); // Using category as description for simplicity
            transaction.setAmount(amount);
            transaction.setCategory(category);
            transaction.setDate(date);
            transaction.setIncome(isIncome);
            transaction.setNote(note);
            transaction.setRepeat(isRecurring);
            transaction.setUserId(currentUser.getUid());

            // Update in Firebase
            repository.updateTransaction(transaction);
            Toast.makeText(requireContext(), "Giao dịch đã được cập nhật", Toast.LENGTH_SHORT).show();
        } else {
            // Create new transaction
            transaction = new Transaction();
            transaction.setFirebaseId(UUID.randomUUID().toString());
            transaction.setId(System.currentTimeMillis());
            transaction.setDescription(category); // Using category as description for simplicity
            transaction.setAmount(amount);
            transaction.setCategory(category);
            transaction.setDate(date);
            transaction.setIncome(isIncome);
            transaction.setNote(note);
            transaction.setRepeat(isRecurring);
            transaction.setUserId(currentUser.getUid());

            // Add to Firebase
            repository.addTransaction(transaction);
            Toast.makeText(requireContext(), "Giao dịch đã được thêm", Toast.LENGTH_SHORT).show();
        }

        // Navigate back
        Navigation.findNavController(requireView()).popBackStack();
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
