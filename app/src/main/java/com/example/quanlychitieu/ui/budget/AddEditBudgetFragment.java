package com.example.quanlychitieu.ui.budget;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.databinding.FragmentAddEditBudgetBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditBudgetFragment extends Fragment {

    private FragmentAddEditBudgetBinding binding;
    private BudgetViewModel viewModel;
    private String budgetId;
    private boolean isEditMode = false;

    private final Calendar startDateCalendar = Calendar.getInstance();
    private final Calendar endDateCalendar = Calendar.getInstance();
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditBudgetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        // Check if we're editing an existing budget
        if (getArguments() != null) {
            budgetId = getArguments().getString("budget_id");
            isEditMode = budgetId != null && !budgetId.isEmpty();

            if (isEditMode) {
                binding.toolbarTitle.setText("Sửa ngân sách");
                loadExistingBudget();
            } else {
                setupDefaultDates();
            }
        } else {
            setupDefaultDates();
        }

        setupMonthPicker();
        setupCategoryDropdown();
        setupAmountInputFormatting();
        setupNotificationOptions();
        setupSaveButton();
        setupToolbar();
    }

    private void setupDefaultDates() {
        // Set to first day of current month
        startDateCalendar.set(Calendar.DAY_OF_MONTH, 1);

        // Set to last day of current month
        endDateCalendar.set(Calendar.DAY_OF_MONTH, endDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));

        // Display the month and year
        binding.monthInput.setText(monthYearFormat.format(startDateCalendar.getTime()));
    }

    private void loadExistingBudget() {
        viewModel.getBudgetById(budgetId).observe(getViewLifecycleOwner(), budget -> {
            if (budget != null) {
                populateFormWithBudgetData(budget);
            }
        });
    }

    private void populateFormWithBudgetData(Budget budget) {
        // Set dates
        startDateCalendar.setTime(budget.getStartDate());
        endDateCalendar.setTime(budget.getEndDate());
        binding.monthInput.setText(monthYearFormat.format(startDateCalendar.getTime()));

        // Set category
        binding.categoryInput.setText(budget.getCategory(), false);

        // Set amount
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("vi", "VN"));
        formatter.applyPattern("#,###");
        String formattedAmount = formatter.format(budget.getAmount()).replace(",", ".");
        binding.budgetAmountInput.setText(formattedAmount);

        // Set notification options
        binding.notificationSwitch.setChecked(budget.isNotificationsEnabled());
        binding.notificationOptions.setVisibility(budget.isNotificationsEnabled() ? View.VISIBLE : View.GONE);

        // Set notification threshold
        int threshold = budget.getNotificationThreshold();
        if (threshold == 80) {
            binding.threshold80.setChecked(true);
        } else if (threshold == 90) {
            binding.threshold90.setChecked(true);
        } else if (threshold == 100) {
            binding.threshold100.setChecked(true);
        }

        // Set note
        if (budget.getNote() != null) {
            binding.noteInput.setText(budget.getNote());
        }
    }

    private void setupMonthPicker() {
        binding.monthInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        // Set to first day of selected month
                        startDateCalendar.set(year, month, 1);

                        // Set to last day of selected month
                        endDateCalendar.set(year, month, 1);
                        endDateCalendar.set(Calendar.DAY_OF_MONTH, endDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));

                        binding.monthInput.setText(monthYearFormat.format(startDateCalendar.getTime()));
                    },
                    startDateCalendar.get(Calendar.YEAR),
                    startDateCalendar.get(Calendar.MONTH),
                    startDateCalendar.get(Calendar.DAY_OF_MONTH)
            );

            // Hide day selection since we only care about month and year
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 2)); // 2 years in future
            datePickerDialog.show();
        });
    }

    private void setupCategoryDropdown() {
        // Setup with expense categories since budgets are typically for expenses
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                CategoryManager.getInstance().getExpenseCategories()
        );

        AutoCompleteTextView categoryInput = binding.categoryInput;
        categoryInput.setAdapter(categoryAdapter);

        // Set default selection if adapter has items
        if (categoryAdapter.getCount() > 0 && !isEditMode) {
            categoryInput.setText(categoryAdapter.getItem(0), false);
        }
    }

    private void setupAmountInputFormatting() {
        binding.budgetAmountInput.addTextChangedListener(new TextWatcher() {
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
                    binding.budgetAmountInput.removeTextChangedListener(this);

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

                    binding.budgetAmountInput.addTextChangedListener(this);
                }
            }
        });
    }

    private void setupNotificationOptions() {
        binding.notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.notificationOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void setupSaveButton() {
        binding.saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveBudget();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Validate category
        String category = binding.categoryInput.getText().toString().trim();
        if (TextUtils.isEmpty(category)) {
            binding.categoryLayout.setError("Vui lòng chọn danh mục");
            isValid = false;
        } else {
            binding.categoryLayout.setError(null);
        }

        // Validate amount
        String amountText = binding.budgetAmountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountText)) {
            binding.budgetAmountLayout.setError("Vui lòng nhập số tiền ngân sách");
            isValid = false;
        } else {
            try {
                // Remove formatting before parsing
                String cleanAmount = amountText.replace(".", "");
                double amount = Double.parseDouble(cleanAmount);
                if (amount <= 0) {
                    binding.budgetAmountLayout.setError("Số tiền phải lớn hơn 0");
                    isValid = false;
                } else {
                    binding.budgetAmountLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.budgetAmountLayout.setError("Số tiền không hợp lệ");
                isValid = false;
            }
        }

        return isValid;
    }

    private void saveBudget() {
        // Get current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get form values
        String category = binding.categoryInput.getText().toString().trim();

        // Parse amount
        String amountStr = binding.budgetAmountInput.getText().toString().trim();
        double amount;
        try {
            amountStr = amountStr.replace(".", "");
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            binding.budgetAmountLayout.setError("Số tiền không hợp lệ");
            return;
        }

        // Get dates
        Date startDate = startDateCalendar.getTime();
        Date endDate = endDateCalendar.getTime();

        // Get notification settings
        boolean notificationsEnabled = binding.notificationSwitch.isChecked();

        // Determine notification threshold
        final int notificationThreshold;
        if (binding.threshold90.isChecked()) {
            notificationThreshold = 90;
        } else if (binding.threshold100.isChecked()) {
            notificationThreshold = 100;
        } else {
            notificationThreshold = 80; // Default
        }

        // Get note
        String note = binding.noteInput.getText().toString().trim();

        // Create final copies of variables for use in lambda
        final double finalAmount = amount;
        final Date finalStartDate = startDate;
        final Date finalEndDate = endDate;
        final boolean finalNotificationsEnabled = notificationsEnabled;
        final String finalNote = note;
        final String finalCategory = category;

        // Create or update budget
        if (isEditMode) {
            // Update existing budget
            viewModel.getBudgetById(budgetId).observe(getViewLifecycleOwner(), existingBudget -> {
                if (existingBudget != null) {
                    // Preserve existing spent amount and notification status
                    double spent = existingBudget.getSpent();
                    boolean notificationSent = existingBudget.isNotificationSent();

                    Budget updatedBudget = new Budget(
                            existingBudget.getId(),
                            userId,
                            finalCategory,
                            finalAmount,
                            spent,
                            finalStartDate,
                            finalEndDate,
                            finalNote,
                            finalNotificationsEnabled,
                            notificationThreshold,
                            notificationSent
                    );
                    updatedBudget.setFirebaseId(budgetId);
                    updatedBudget.setRecurringExpenseNotifications(existingBudget.getRecurringExpenseNotifications());

                    viewModel.updateBudget(updatedBudget);
                    Toast.makeText(requireContext(), "Ngân sách đã được cập nhật", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                }
            });
        } else {
            // Create new budget
            Budget budget = new Budget(
                    System.currentTimeMillis(),
                    userId,
                    finalCategory,
                    finalAmount,
                    0, // Initial spent amount is 0
                    finalStartDate,
                    finalEndDate,
                    finalNote,
                    finalNotificationsEnabled,
                    notificationThreshold,
                    false // notification not sent yet
            );

            viewModel.addBudget(budget);
            Toast.makeText(requireContext(), "Ngân sách đã được tạo", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
        }
    }


    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
