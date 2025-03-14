package com.example.quanlychitieu.ui.reminders;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.Reminder;
import com.example.quanlychitieu.service.ReminderNotificationService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.example.quanlychitieu.utils.CurrencyFormatter;
import java.math.BigDecimal;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddEditReminderFragment extends Fragment {
    private RemindersViewModel viewModel;
    private TextInputEditText titleInput, amountInput, dateInput, timeInput, endDateInput, noteInput;
    private AutoCompleteTextView categoryInput, repeatTypeInput;
    private SwitchMaterial repeatSwitch;
    private LinearLayout repeatOptions;
    private MaterialButton saveButton;
    private ReminderNotificationService notificationService;

    private Calendar reminderCalendar = Calendar.getInstance();
    private Calendar endDateCalendar = Calendar.getInstance();
    private boolean isEditMode = false;
    private long reminderId = -1;
    private Reminder currentReminder;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_edit_reminder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo notification service
        notificationService = new ReminderNotificationService(requireContext());

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(RemindersViewModel.class);

        // Khởi tạo views
        initViews(view);

        // Thiết lập các spinner/dropdown
        setupDropdowns();

        // Thiết lập các date/time pickers
        setupDateTimePickers();

        // Thiết lập switch lặp lại
        setupRepeatSwitch();

        // Thiết lập nút lưu
        setupSaveButton();

        // Lấy arguments để xác định chế độ thêm mới hay chỉnh sửa
        Bundle args = getArguments();
        if (args != null) {
            reminderId = args.getLong("reminderId", -1);
            String documentId = args.getString("documentId");

            Log.d("AddEditReminderFragment", "Received reminderId: " + reminderId + ", documentId: " + documentId);

            if (reminderId != -1) {
                isEditMode = true;
                ((TextView) view.findViewById(R.id.toolbar_title)).setText("Chỉnh sửa nhắc nhở");

                // Ưu tiên tải dữ liệu bằng documentId nếu có
                if (documentId != null && !documentId.isEmpty()) {
                    loadReminderDataByDocumentId(documentId);
                } else {
                    // Nếu không có documentId, tải theo numericId
                    loadReminderData(reminderId);
                }
            }
        }
    }
    private void loadReminderDataByDocumentId(String documentId) {
        Log.d("AddEditReminderFragment", "Loading reminder by documentId: " + documentId);
        viewModel.getReminderByDocumentId(documentId).observe(getViewLifecycleOwner(), reminder -> {
            if (reminder != null) {
                currentReminder = reminder;
                fillFormWithData();
                Log.d("AddEditReminderFragment", "Loaded reminder: " + reminder.getTitle());
            } else {
                Toast.makeText(requireContext(), "Không tìm thấy nhắc nhở", Toast.LENGTH_SHORT).show();
                Log.e("AddEditReminderFragment", "Failed to load reminder with documentId: " + documentId);
                Navigation.findNavController(requireView()).popBackStack();
            }
        });
    }
    private void loadReminderData(long reminderId) {
        viewModel.getReminderByNumericId(reminderId).observe(getViewLifecycleOwner(), reminder -> {
            if (reminder != null) {
                currentReminder = reminder;
                fillFormWithData();
            } else {
                currentReminder = null;
                isEditMode = false;
            }
        });
    }



    private void initViews(View view) {
        titleInput = view.findViewById(R.id.title_input);
        amountInput = view.findViewById(R.id.amount_input);
        categoryInput = view.findViewById(R.id.category_input);
        dateInput = view.findViewById(R.id.date_input);
        timeInput = view.findViewById(R.id.time_input);
        repeatSwitch = view.findViewById(R.id.repeat_switch);
        repeatOptions = view.findViewById(R.id.repeat_options);
        repeatTypeInput = view.findViewById(R.id.repeat_type_input);
        endDateInput = view.findViewById(R.id.end_date_input);
        noteInput = view.findViewById(R.id.note_input);
        saveButton = view.findViewById(R.id.save_button);

        // Thiết lập tiêu đề toolbar
        if (isEditMode) {
            ((TextView) view.findViewById(R.id.toolbar_title)).setText("Chỉnh sửa nhắc nhở");
        }

        // Thiết lập nút back
        ((MaterialToolbar) view.findViewById(R.id.toolbar)).setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack()
        );

        amountInput.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Không cần xử lý
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Không cần xử lý
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    amountInput.removeTextChangedListener(this);

                    String cleanString = s.toString().replace(".", "").replace("đ", "");

                    if (!cleanString.isEmpty()) {
                        try {
                            double parsed = Double.parseDouble(cleanString);
                            String formatted = CurrencyFormatter.formatNumber(parsed);
                            current = formatted;
                            s.replace(0, s.length(), formatted);
                        } catch (NumberFormatException e) {
                            s.replace(0, s.length(), "");
                        }
                    } else {
                        current = "";
                    }

                    amountInput.addTextChangedListener(this);
                }
            }
        });
    }


    private void setupDropdowns() {
        // Thiết lập danh mục
        String[] categories = {"Chi tiêu cá nhân", "Tiền thuê nhà", "Hóa đơn", "Tiền điện",
                "Tiền nước", "Internet", "Bảo hiểm", "Trả góp", "Khác"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        categoryInput.setAdapter(categoryAdapter);

        // Thiết lập loại lặp lại
        String[] repeatTypes = {"Hàng ngày", "Hàng tuần", "Hàng tháng", "Hàng năm"};
        ArrayAdapter<String> repeatTypeAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, repeatTypes);
        repeatTypeInput.setAdapter(repeatTypeAdapter);
    }

    private void setupDateTimePickers() {
        // Thiết lập date picker cho ngày nhắc nhở
        dateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        reminderCalendar.set(Calendar.YEAR, year);
                        reminderCalendar.set(Calendar.MONTH, month);
                        reminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateInput.setText(dateFormat.format(reminderCalendar.getTime()));
                    },
                    reminderCalendar.get(Calendar.YEAR),
                    reminderCalendar.get(Calendar.MONTH),
                    reminderCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        // Thiết lập time picker cho giờ nhắc nhở
        timeInput.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        reminderCalendar.set(Calendar.MINUTE, minute);
                        timeInput.setText(timeFormat.format(reminderCalendar.getTime()));
                    },
                    reminderCalendar.get(Calendar.HOUR_OF_DAY),
                    reminderCalendar.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });

        // Thiết lập date picker cho ngày kết thúc
        endDateInput.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        endDateCalendar.set(Calendar.YEAR, year);
                        endDateCalendar.set(Calendar.MONTH, month);
                        endDateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        endDateInput.setText(dateFormat.format(endDateCalendar.getTime()));
                    },
                    endDateCalendar.get(Calendar.YEAR),
                    endDateCalendar.get(Calendar.MONTH),
                    endDateCalendar.get(Calendar.DAY_OF_MONTH)
            );
            // Ngày kết thúc phải sau ngày nhắc nhở
            datePickerDialog.getDatePicker().setMinDate(reminderCalendar.getTimeInMillis());
            datePickerDialog.show();
        });

        // Thiết lập giá trị mặc định
        dateInput.setText(dateFormat.format(reminderCalendar.getTime()));
        timeInput.setText(timeFormat.format(reminderCalendar.getTime()));
    }

    private void setupRepeatSwitch() {
        repeatSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            repeatOptions.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveReminder();
            }
        });
    }

    private boolean validateForm() {
        boolean valid = true;

        // Kiểm tra tiêu đề
        if (TextUtils.isEmpty(titleInput.getText())) {
            ((TextInputLayout) titleInput.getParent().getParent()).setError("Vui lòng nhập tiêu đề");
            valid = false;
        } else {
            ((TextInputLayout) titleInput.getParent().getParent()).setError(null);
        }

        // Kiểm tra số tiền
        if (TextUtils.isEmpty(amountInput.getText())) {
            ((TextInputLayout) amountInput.getParent().getParent()).setError("Vui lòng nhập số tiền");
            valid = false;
        } else {
            try {
                double amount = CurrencyFormatter.parseVietnamCurrency(amountInput.getText().toString());
                if (amount <= 0) {
                    ((TextInputLayout) amountInput.getParent().getParent()).setError("Số tiền phải lớn hơn 0");
                    valid = false;
                } else {
                    ((TextInputLayout) amountInput.getParent().getParent()).setError(null);
                }
            } catch (Exception e) {
                ((TextInputLayout) amountInput.getParent().getParent()).setError("Số tiền không hợp lệ");
                valid = false;
            }
        }

        // Kiểm tra danh mục
        if (TextUtils.isEmpty(categoryInput.getText())) {
            ((TextInputLayout) categoryInput.getParent().getParent()).setError("Vui lòng chọn danh mục");
            valid = false;
        } else {
            ((TextInputLayout) categoryInput.getParent().getParent()).setError(null);
        }

        // Kiểm tra ngày và giờ
        if (TextUtils.isEmpty(dateInput.getText())) {
            ((TextInputLayout) dateInput.getParent().getParent()).setError("Vui lòng chọn ngày");
            valid = false;
        } else {
            ((TextInputLayout) dateInput.getParent().getParent()).setError(null);
        }

        if (TextUtils.isEmpty(timeInput.getText())) {
            ((TextInputLayout) timeInput.getParent().getParent()).setError("Vui lòng chọn giờ");
            valid = false;
        } else {
            ((TextInputLayout) timeInput.getParent().getParent()).setError(null);
        }

        // Nếu bật lặp lại, kiểm tra loại lặp lại
        if (repeatSwitch.isChecked() && TextUtils.isEmpty(repeatTypeInput.getText())) {
            ((TextInputLayout) repeatTypeInput.getParent().getParent()).setError("Vui lòng chọn tần suất lặp lại");
            valid = false;
        } else {
            ((TextInputLayout) repeatTypeInput.getParent().getParent()).setError(null);
        }

        return valid;
    }

    private void saveReminder() {
        try {
            // Lấy dữ liệu từ form
            String title = titleInput.getText().toString().trim();
            double amount = CurrencyFormatter.parseVietnamCurrency(amountInput.getText().toString().trim());
            String category = categoryInput.getText().toString().trim();
            Date dateTime = reminderCalendar.getTime();
            boolean isRepeating = repeatSwitch.isChecked();

            // Log thông tin để debug
            Log.d("AddEditReminderFragment", "Saving reminder: " + title);
            Log.d("AddEditReminderFragment", "DateTime: " + dateTime);

            String repeatType = null;
            Date endDate = null;

            if (isRepeating) {
                String selectedRepeatType = repeatTypeInput.getText().toString().trim();
                // Chuyển đổi text sang mã lưu trữ
                if (selectedRepeatType.equals("Hàng ngày")) {
                    repeatType = "DAILY";
                } else if (selectedRepeatType.equals("Hàng tuần")) {
                    repeatType = "WEEKLY";
                } else if (selectedRepeatType.equals("Hàng tháng")) {
                    repeatType = "MONTHLY";
                } else if (selectedRepeatType.equals("Hàng năm")) {
                    repeatType = "YEARLY";
                }

                // Lấy ngày kết thúc nếu có
                if (!TextUtils.isEmpty(endDateInput.getText())) {
                    endDate = endDateCalendar.getTime();
                }
            }

            String note = noteInput.getText().toString().trim();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            // Tạo đối tượng Reminder
            Reminder reminder = new Reminder(
                    reminderId > 0 ? reminderId : System.currentTimeMillis(),
                    title,
                    dateTime,
                    false, // isCompleted
                    amount,
                    category,
                    note,
                    isRepeating,
                    repeatType,
                    endDate,
                    userId
            );

            // Lưu nhắc nhở
            if (isEditMode && currentReminder != null) {
                viewModel.updateReminder(currentReminder.getDocumentId(), reminder, notificationService);

                Log.d("AddEditReminderFragment", "Updating reminder with ID: " + currentReminder.getDocumentId());
            } else {
                viewModel.addReminder(reminder, notificationService);
                Log.d("AddEditReminderFragment", "Adding new reminder");
            }

            // Quay lại màn hình danh sách
            Toast.makeText(requireContext(),
                    isEditMode ? "Đã cập nhật nhắc nhở" : "Đã thêm nhắc nhở mới",
                    Toast.LENGTH_SHORT).show();
            Navigation.findNavController(requireView()).popBackStack();

        } catch (Exception e) {
            Log.e("AddEditReminderFragment", "Error saving reminder", e);
            Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }



    private void fillFormWithData() {
        // Điền dữ liệu vào form khi ở chế độ chỉnh sửa
        titleInput.setText(currentReminder.getTitle());
        amountInput.setText(CurrencyFormatter.formatNumber(currentReminder.getAmount()));

        categoryInput.setText(currentReminder.getCategory());

        // Thiết lập ngày và giờ
        Date reminderDate = currentReminder.getDateTime();
        if (reminderDate != null) {
            reminderCalendar.setTime(reminderDate);
            dateInput.setText(dateFormat.format(reminderDate));
            timeInput.setText(timeFormat.format(reminderDate));
        }

        // Thiết lập lặp lại
        repeatSwitch.setChecked(currentReminder.isRepeating());
        if (currentReminder.isRepeating()) {
            repeatOptions.setVisibility(View.VISIBLE);

            // Chuyển đổi mã sang text hiển thị
            String repeatTypeText = "";
            if (currentReminder.getRepeatType() != null) {
                switch (currentReminder.getRepeatType()) {
                    case "DAILY":
                        repeatTypeText = "Hàng ngày";
                        break;
                    case "WEEKLY":
                        repeatTypeText = "Hàng tuần";
                        break;
                    case "MONTHLY":
                        repeatTypeText = "Hàng tháng";
                        break;
                    case "YEARLY":
                        repeatTypeText = "Hàng năm";
                        break;
                }
            }
            repeatTypeInput.setText(repeatTypeText);

            // Thiết lập ngày kết thúc
            if (currentReminder.getEndDate() != null) {
                endDateCalendar.setTime(currentReminder.getEndDate());
                endDateInput.setText(dateFormat.format(currentReminder.getEndDate()));
            }
        }

        // Thiết lập ghi chú
        noteInput.setText(currentReminder.getNote());
    }
}