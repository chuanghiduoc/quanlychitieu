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
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", new Locale("vi", "VN"));

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

        // Kiểm tra xem chúng ta đang chỉnh sửa ngân sách hiện có hay không
        if (getArguments() != null) {
            budgetId = getArguments().getString("budget_id");
            String selectedCategory = getArguments().getString("selected_category");
            isEditMode = budgetId != null && !budgetId.isEmpty();

            if (isEditMode) {
                // Sửa ngân sách hiện có
                binding.toolbarTitle.setText("Sửa ngân sách");
                loadExistingBudget();
                setupDeleteButton();
            } else {
                // Thêm ngân sách mới
                binding.toolbarTitle.setText("Thêm ngân sách");
                setupDefaultDates();

                // Nếu có danh mục được chọn sẵn
                if (selectedCategory != null && !selectedCategory.isEmpty()) {
                    setupCategoryDropdownWithPreselection(selectedCategory);
                } else {
                    setupCategoryDropdown();
                }

                // Ẩn nút xóa khi thêm mới
                binding.deleteButton.setVisibility(View.GONE);
            }
        } else {
            // Thêm ngân sách mới (không có tham số)
            binding.toolbarTitle.setText("Thêm ngân sách");
            setupDefaultDates();
            setupCategoryDropdown();

            // Ẩn nút xóa khi thêm mới
            binding.deleteButton.setVisibility(View.GONE);
        }

        setupMonthPicker();
        setupAmountInputFormatting();
        setupNotificationOptions();
        setupSaveButton();
        setupToolbar();
    }

    // Thêm phương thức mới để thiết lập dropdown với danh mục được chọn sẵn
    private void setupCategoryDropdownWithPreselection(String selectedCategory) {
        // Thiết lập với các danh mục chi tiêu
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                CategoryManager.getInstance().getExpenseCategories()
        );

        AutoCompleteTextView categoryInput = binding.categoryInput;
        categoryInput.setAdapter(categoryAdapter);

        // Thiết lập danh mục được chọn sẵn
        categoryInput.setText(selectedCategory, false);
    }

    // Thêm phương thức mới để thiết lập nút xóa ngân sách
    private void setupDeleteButton() {
        binding.deleteButton.setVisibility(View.VISIBLE);
        binding.deleteButton.setOnClickListener(v -> {
            if (budgetId != null) {
                viewModel.deleteBudget(budgetId);
                Toast.makeText(requireContext(), "Đã xóa ngân sách", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });
    }


    private void setupDefaultDates() {
        // Đặt thành ngày đầu tiên của tháng hiện tại
        startDateCalendar.set(Calendar.DAY_OF_MONTH, 1);

        // Đặt thành ngày cuối cùng của tháng hiện tại
        endDateCalendar.set(Calendar.DAY_OF_MONTH, endDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));

        // Hiển thị tháng và năm
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
        // Đặt ngày tháng
        startDateCalendar.setTime(budget.getStartDate());
        endDateCalendar.setTime(budget.getEndDate());
        binding.monthInput.setText(monthYearFormat.format(startDateCalendar.getTime()));

        // Đặt danh mục
        binding.categoryInput.setText(budget.getCategory(), false);

        // Đặt số tiền
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("vi", "VN"));
        formatter.applyPattern("#,###");
        String formattedAmount = formatter.format(budget.getAmount()).replace(",", ".");
        binding.budgetAmountInput.setText(formattedAmount);

        // Đặt tùy chọn thông báo
        binding.notificationSwitch.setChecked(budget.isNotificationsEnabled());
        binding.notificationOptions.setVisibility(budget.isNotificationsEnabled() ? View.VISIBLE : View.GONE);

        // Đặt ngưỡng thông báo
        int threshold = budget.getNotificationThreshold();
        if (threshold == 80) {
            binding.threshold80.setChecked(true);
        } else if (threshold == 90) {
            binding.threshold90.setChecked(true);
        } else if (threshold == 100) {
            binding.threshold100.setChecked(true);
        }

        // Đặt ghi chú
        if (budget.getNote() != null) {
            binding.noteInput.setText(budget.getNote());
        }
    }

    private void setupMonthPicker() {
        binding.monthInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        // Đặt thành ngày đầu tiên của tháng được chọn
                        startDateCalendar.set(year, month, 1);

                        // Đặt thành ngày cuối cùng của tháng được chọn
                        endDateCalendar.set(year, month, 1);
                        endDateCalendar.set(Calendar.DAY_OF_MONTH, endDateCalendar.getActualMaximum(Calendar.DAY_OF_MONTH));

                        binding.monthInput.setText(monthYearFormat.format(startDateCalendar.getTime()));
                    },
                    startDateCalendar.get(Calendar.YEAR),
                    startDateCalendar.get(Calendar.MONTH),
                    startDateCalendar.get(Calendar.DAY_OF_MONTH)
            );

            // Ẩn phần chọn ngày vì chúng ta chỉ quan tâm đến tháng và năm
            // Giới hạn ngày tối đa là 2 năm trong tương lai
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 2)); // 2 năm trong tương lai
            datePickerDialog.show();
        });
    }

    private void setupCategoryDropdown() {
        // Thiết lập với các danh mục chi tiêu vì ngân sách thường dành cho chi tiêu
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                CategoryManager.getInstance().getExpenseCategories()
        );

        AutoCompleteTextView categoryInput = binding.categoryInput;
        categoryInput.setAdapter(categoryAdapter);

        // Đặt lựa chọn mặc định nếu adapter có mục và không phải chế độ sửa
        if (categoryAdapter.getCount() > 0 && !isEditMode) {
            categoryInput.setText(categoryAdapter.getItem(0), false);
        }
    }

    private void setupAmountInputFormatting() {
        binding.budgetAmountInput.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Không cần thiết
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Không cần thiết
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    binding.budgetAmountInput.removeTextChangedListener(this);

                    // Xóa tất cả các ký tự không phải là số
                    String cleanString = s.toString().replaceAll("[^\\d]", "");

                    if (!cleanString.isEmpty()) {
                        try {
                            // Phân tích thành kiểu long (không có số thập phân cho VND)
                            long parsed = Long.parseLong(cleanString);

                            // Định dạng với ngôn ngữ Việt Nam
                            DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("vi", "VN"));
                            formatter.applyPattern("#,###");
                            formatter.setDecimalSeparatorAlwaysShown(false);
                            formatter.setGroupingUsed(true);
                            formatter.setGroupingSize(3);

                            String formatted = formatter.format(parsed);
                            // Thay thế dấu phẩy bằng dấu chấm để hiển thị kiểu Việt Nam
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
        // dùng & thay vì && để cả hai hàm đều chạy (trả về lỗi nếu cả hai đều sai)
        return validateCategory() & validateAmount();
    }

    private boolean validateCategory() {
        String category = binding.categoryInput.getText().toString().trim();
        if (TextUtils.isEmpty(category)) {
            binding.categoryLayout.setError("Vui lòng chọn danh mục");
            return false;
        } else {
            binding.categoryLayout.setError(null);
            return true;
        }
    }

    private boolean validateAmount() {
        String amountText = binding.budgetAmountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountText)) {
            binding.budgetAmountLayout.setError("Vui lòng nhập số tiền ngân sách");
            return false;
        }

        try {
            // Xóa dấu chấm trước khi phân tích
            double amount = Double.parseDouble(amountText.replace(".", ""));
            if (amount <= 0) {
                binding.budgetAmountLayout.setError("Số tiền phải lớn hơn 0");
                return false;
            }
            binding.budgetAmountLayout.setError(null);
            return true;
        } catch (NumberFormatException e) {
            binding.budgetAmountLayout.setError("Số tiền không hợp lệ");
            return false;
        }
    }

    private void saveBudget() {
        // Lấy ID người dùng hiện tại
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Lấy giá trị từ biểu mẫu
        String category = binding.categoryInput.getText().toString().trim();

        // Phân tích số tiền
        String amountStr = binding.budgetAmountInput.getText().toString().trim();
        double amount;
        try {
            // Xóa dấu chấm trước khi phân tích
            amountStr = amountStr.replace(".", "");
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            binding.budgetAmountLayout.setError("Số tiền không hợp lệ");
            return;
        }

        // Lấy ngày tháng
        Date startDate = startDateCalendar.getTime();
        Date endDate = endDateCalendar.getTime();

        // Lấy cài đặt thông báo
        boolean notificationsEnabled = binding.notificationSwitch.isChecked();

        // Xác định ngưỡng thông báo
        final int notificationThreshold;
        if (binding.threshold90.isChecked()) {
            notificationThreshold = 90;
        } else if (binding.threshold100.isChecked()) {
            notificationThreshold = 100;
        } else {
            notificationThreshold = 80; // Mặc định
        }

        // Lấy ghi chú
        String note = binding.noteInput.getText().toString().trim();

        // Tạo bản sao cuối cùng của các biến để sử dụng trong lambda
        final double finalAmount = amount;
        final Date finalStartDate = startDate;
        final Date finalEndDate = endDate;
        final boolean finalNotificationsEnabled = notificationsEnabled;
        final String finalNote = note;
        final String finalCategory = category;

        // Tạo hoặc cập nhật ngân sách
        if (isEditMode) {
            // Cập nhật ngân sách hiện có
            viewModel.getBudgetById(budgetId).observe(getViewLifecycleOwner(), existingBudget -> {
                if (existingBudget != null) {
                    // Giữ lại số tiền đã chi tiêu và trạng thái thông báo hiện có
                    double spent = existingBudget.getSpent();
                    boolean notificationSent = existingBudget.isNotificationSent();

                    Budget updatedBudget = new Budget(
                            existingBudget.getId(),
                            userId,
                            finalCategory,
                            finalAmount,
                            spent, // Giữ lại số tiền đã chi
                            finalStartDate,
                            finalEndDate,
                            finalNote,
                            finalNotificationsEnabled,
                            notificationThreshold,
                            notificationSent // Giữ lại trạng thái đã gửi thông báo
                    );
                    updatedBudget.setFirebaseId(budgetId); // Đặt ID Firebase để cập nhật đúng bản ghi
                    // Giữ lại cài đặt thông báo chi tiêu định kỳ (nếu có)
                    updatedBudget.setRecurringExpenseNotifications(existingBudget.getRecurringExpenseNotifications());

                    viewModel.updateBudget(updatedBudget);
                    Toast.makeText(requireContext(), "Ngân sách đã được cập nhật", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).popBackStack();
                }
            });
        } else {
            // Tạo ngân sách mới
            Budget budget = new Budget(
                    System.currentTimeMillis(), // ID tạm thời, sẽ được thay bằng ID Firebase khi lưu
                    userId,
                    finalCategory,
                    finalAmount,
                    0, // Số tiền chi tiêu ban đầu là 0
                    finalStartDate,
                    finalEndDate,
                    finalNote,
                    finalNotificationsEnabled,
                    notificationThreshold,
                    false // thông báo chưa được gửi
            );

            viewModel.addBudget(budget);
            Toast.makeText(requireContext(), "Ngân sách đã được tạo", Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();
        }
    }


    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Quay lại màn hình trước đó
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Giải phóng binding để tránh rò rỉ bộ nhớ
        binding = null;
    }
}