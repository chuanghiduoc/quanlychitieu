package com.example.quanlychitieu.ui.goals;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.FinancialGoal;
import com.example.quanlychitieu.databinding.FragmentAddEditGoalBinding;
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

public class AddEditGoalFragment extends Fragment {

    private FragmentAddEditGoalBinding binding;
    private GoalsViewModel viewModel;
    private FirebaseAuth auth;

    private final Calendar startDateCalendar = Calendar.getInstance();
    private final Calendar endDateCalendar = Calendar.getInstance();
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private String goalId;
    private boolean isEditMode = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditGoalBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo ViewModel và Auth
        viewModel = new ViewModelProvider(this).get(GoalsViewModel.class);
        auth = FirebaseAuth.getInstance();

        // Kiểm tra nếu đang sửa mục tiêu hiện có
        if (getArguments() != null) {
            goalId = getArguments().getString("goal_id");
            isEditMode = goalId != null && !goalId.isEmpty();

            if (isEditMode) {
                // Tải dữ liệu mục tiêu hiện có
                loadExistingGoal(goalId);
                binding.toolbarTitle.setText("Sửa mục tiêu");
            } else {
                // Thiết lập mặc định cho ngày bắt đầu và kết thúc
                endDateCalendar.add(Calendar.YEAR, 1); // Mặc định kết thúc sau 1 năm
            }
        }

        // Thiết lập các trường nhập liệu
        setupDatePickers();
        setupAmountInputFormatting();
        setupCategoryDropdown();
        setupSaveButton();
        setupToolbar();
    }

    private void loadExistingGoal(String id) {
        viewModel.getGoalById(id).observe(getViewLifecycleOwner(), goal -> {
            if (goal != null) {
                populateFormWithGoalData(goal);
            }
        });
    }

    private void populateFormWithGoalData(FinancialGoal goal) {
        // Thiết lập tên và mô tả
        binding.nameInput.setText(goal.getName());
        binding.descriptionInput.setText(goal.getDescription());

        // Định dạng số tiền
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("vi", "VN"));
        formatter.applyPattern("#,###");
        formatter.setDecimalSeparatorAlwaysShown(false);

        String formattedTarget = formatter.format(goal.getTargetAmount());
        formattedTarget = formattedTarget.replace(",", ".");
        binding.targetAmountInput.setText(formattedTarget);

        String formattedInitial = formatter.format(goal.getCurrentAmount());
        formattedInitial = formattedInitial.replace(",", ".");
        binding.initialAmountInput.setText(formattedInitial);

        // Thiết lập ngày
        startDateCalendar.setTime(goal.getStartDate());
        binding.startDateInput.setText(dateFormatter.format(goal.getStartDate()));

        endDateCalendar.setTime(goal.getEndDate());
        binding.endDateInput.setText(dateFormatter.format(goal.getEndDate()));

        // Thiết lập danh mục nếu có
        if (goal.getCategory() != null && !goal.getCategory().isEmpty()) {
            binding.categoryInput.setText(goal.getCategory(), false);
        }
    }

    private void setupDatePickers() {
        // Thiết lập ngày bắt đầu
        binding.startDateInput.setText(dateFormatter.format(startDateCalendar.getTime()));
        binding.startDateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        startDateCalendar.set(Calendar.YEAR, year);
                        startDateCalendar.set(Calendar.MONTH, month);
                        startDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        binding.startDateInput.setText(dateFormatter.format(startDateCalendar.getTime()));
                    },
                    startDateCalendar.get(Calendar.YEAR),
                    startDateCalendar.get(Calendar.MONTH),
                    startDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Thiết lập ngày kết thúc
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

    private void setupAmountInputFormatting() {
        // Định dạng số tiền mục tiêu
        setupAmountField(binding.targetAmountInput);

        // Định dạng số tiền ban đầu
        setupAmountField(binding.initialAmountInput);
    }

    private void setupAmountField(com.google.android.material.textfield.TextInputEditText editText) {
        int maxLength = 15;
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(maxLength);
        editText.setFilters(filters);

        editText.addTextChangedListener(new TextWatcher() {
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
                    editText.removeTextChangedListener(this);

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

                    editText.addTextChangedListener(this);
                }
            }
        });
    }

    private void setupCategoryDropdown() {
        // Tạo danh sách danh mục cho mục tiêu
        List<String> categories = new ArrayList<>();
        categories.add("Mua nhà");
        categories.add("Mua xe");
        categories.add("Du lịch");
        categories.add("Học tập");
        categories.add("Đám cưới");
        categories.add("Hưu trí");
        categories.add("Khẩn cấp");
        categories.add("Khác");

        // Thêm các danh mục chi tiêu hiện có
        categories.addAll(CategoryManager.getInstance().getExpenseCategories());

        // Thiết lập adapter cho dropdown
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
        );

        binding.categoryInput.setAdapter(adapter);
    }

    private void setupSaveButton() {
        binding.saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveGoal();
            }
        });
    }

    private boolean validateForm() {
        boolean isValid = true;

        // Kiểm tra tên mục tiêu
        String name = binding.nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            binding.nameLayout.setError("Vui lòng nhập tên mục tiêu");
            isValid = false;
        } else {
            binding.nameLayout.setError(null);
        }

        // Kiểm tra số tiền mục tiêu
        String targetAmountText = binding.targetAmountInput.getText().toString().trim();
        if (targetAmountText.isEmpty()) {
            binding.targetAmountLayout.setError("Vui lòng nhập số tiền mục tiêu");
            isValid = false;
        } else {
            try {
                // Bỏ định dạng trước khi parse
                String cleanAmount = targetAmountText.replace(".", "");
                double amount = Double.parseDouble(cleanAmount);
                if (amount <= 0) {
                    binding.targetAmountLayout.setError("Số tiền phải lớn hơn 0");
                    isValid = false;
                } else {
                    binding.targetAmountLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.targetAmountLayout.setError("Số tiền không hợp lệ");
                isValid = false;
            }
        }

        // Kiểm tra ngày bắt đầu và kết thúc
        if (endDateCalendar.before(startDateCalendar)) {
            binding.endDateLayout.setError("Ngày kết thúc phải sau ngày bắt đầu");
            isValid = false;
        } else {
            binding.endDateLayout.setError(null);
        }

        // Kiểm tra số tiền ban đầu (nếu có)
        String initialAmountText = binding.initialAmountInput.getText().toString().trim();
        if (!initialAmountText.isEmpty()) {
            try {
                // Bỏ định dạng trước khi parse
                String cleanAmount = initialAmountText.replace(".", "");
                double initialAmount = Double.parseDouble(cleanAmount);

                // Kiểm tra số tiền ban đầu có lớn hơn số tiền mục tiêu không
                String cleanTargetAmount = targetAmountText.replace(".", "");
                double targetAmount = Double.parseDouble(cleanTargetAmount);

                if (initialAmount > targetAmount) {
                    binding.initialAmountLayout.setError("Số tiền ban đầu không thể lớn hơn số tiền mục tiêu");
                    isValid = false;
                } else {
                    binding.initialAmountLayout.setError(null);
                }
            } catch (NumberFormatException e) {
                binding.initialAmountLayout.setError("Số tiền không hợp lệ");
                isValid = false;
            }
        }

        // Kiểm tra đăng nhập
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu mục tiêu", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void saveGoal() {
        // Lấy người dùng hiện tại
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu mục tiêu", Toast.LENGTH_SHORT).show();
            return;
        }

        // Lấy giá trị từ form
        String name = binding.nameInput.getText().toString().trim();
        String description = binding.descriptionInput.getText().toString().trim();

        // Parse số tiền mục tiêu
        String targetAmountStr = binding.targetAmountInput.getText().toString().trim();
        targetAmountStr = targetAmountStr.replace(".", "");
        double targetAmount = Double.parseDouble(targetAmountStr);

        // Parse số tiền ban đầu (nếu có)
        double initialAmount = 0.0;
        String initialAmountStr = binding.initialAmountInput.getText().toString().trim();
        if (!initialAmountStr.isEmpty()) {
            initialAmountStr = initialAmountStr.replace(".", "");
            initialAmount = Double.parseDouble(initialAmountStr);
        }

        // Lấy ngày bắt đầu và kết thúc
        Date startDate = startDateCalendar.getTime();
        Date endDate = endDateCalendar.getTime();

        // Lấy danh mục (nếu có)
        String category = binding.categoryInput.getText() != null ?
                binding.categoryInput.getText().toString() : "";

        FinancialGoal goal = new FinancialGoal();
        goal.setId(System.currentTimeMillis());
        goal.setName(name);
        goal.setDescription(description);
        goal.setTargetAmount(targetAmount);
        goal.setCurrentAmount(initialAmount);
        goal.setStartDate(startDate);
        goal.setEndDate(endDate);
        goal.setCompleted(initialAmount >= targetAmount);
        goal.setCategory(category);
        goal.setUserId(currentUser.getUid());

        if (isEditMode && goalId != null && !goalId.isEmpty()) {
            // Cập nhật mục tiêu hiện có
            goal.setFirebaseId(goalId);
            viewModel.updateGoal(goal);
            Toast.makeText(requireContext(), "Mục tiêu đã được cập nhật", Toast.LENGTH_SHORT).show();
        } else {
            // Tạo mục tiêu mới
            viewModel.addGoal(goal);
            Toast.makeText(requireContext(), "Mục tiêu đã được thêm", Toast.LENGTH_SHORT).show();
        }

        // Quay lại màn hình danh sách
        Navigation.findNavController(requireView()).popBackStack();
    }


    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Quay lại màn hình trước
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
