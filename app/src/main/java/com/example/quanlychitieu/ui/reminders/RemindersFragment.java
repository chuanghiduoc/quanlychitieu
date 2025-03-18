package com.example.quanlychitieu.ui.reminders;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.ReminderAdapter;
import com.example.quanlychitieu.adapter.helper.SwipeToDeleteCallback;
import com.example.quanlychitieu.data.model.Reminder;
import com.example.quanlychitieu.service.ReminderNotificationService;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class RemindersFragment extends Fragment implements ReminderAdapter.OnReminderActionListener {
    private static final String TAG = "RemindersFragment";
    private RemindersViewModel viewModel;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ReminderAdapter adapter;
    private TabLayout tabLayout;
    private ReminderNotificationService notificationService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reminders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo notification service
        notificationService = new ReminderNotificationService(requireContext());

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(RemindersViewModel.class);

        // Khởi tạo views
        recyclerView = view.findViewById(R.id.reminders_recycler_view);
        emptyState = view.findViewById(R.id.empty_state);
        tabLayout = view.findViewById(R.id.tab_layout);

        // Thiết lập RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReminderAdapter(getContext(), this);
        recyclerView.setAdapter(adapter);

        // Thiết lập swipe to delete
        setupSwipeToDelete();

        // Thiết lập TabLayout
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateReminderList(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Không cần xử lý
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Không cần xử lý
            }
        });

        // Quan sát dữ liệu
        observeViewModelData();

        // Bắt đầu lắng nghe thay đổi dữ liệu
        viewModel.startListeningForReminders();

        // Mặc định hiển thị tab đầu tiên (Sắp tới)
        updateReminderList(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        viewModel.stopListeningForReminders();
    }

    private void observeViewModelData() {
        viewModel.getUpcomingReminders().observe(getViewLifecycleOwner(), reminders -> {
            if (tabLayout.getSelectedTabPosition() == 0) {
                updateUI(reminders.isEmpty());
                adapter.setReminders(reminders);

                // Lên lịch thông báo cho tất cả nhắc nhở sắp tới
                scheduleNotificationsForReminders(reminders);
            }
        });

        viewModel.getPastReminders().observe(getViewLifecycleOwner(), reminders -> {
            if (tabLayout.getSelectedTabPosition() == 1) {
                updateUI(reminders.isEmpty());
                adapter.setReminders(reminders);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void scheduleNotificationsForReminders(List<Reminder> reminders) {
        // Lên lịch thông báo cho tất cả nhắc nhở chưa hoàn thành
        for (Reminder reminder : reminders) {
            if (!reminder.isCompleted()) {
                notificationService.scheduleNotification(reminder);
            }
        }
    }

    private void updateReminderList(int tabPosition) {
        Log.d(TAG, "Updating reminder list for tab: " + tabPosition);
        if (tabPosition == 0) {
            // Tab "Sắp tới"
            viewModel.loadUpcomingReminders();
        } else {
            // Tab "Đã qua"
            viewModel.loadPastReminders();
        }
    }

    private void updateUI(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMarkAsPaid(Reminder reminder) {
        if (reminder.getDocumentId() != null) {
            // Hiển thị dialog xác nhận
            new AlertDialog.Builder(requireContext())
                    .setTitle("Đánh dấu đã thanh toán")
                    .setMessage("Bạn có chắc chắn muốn đánh dấu nhắc nhở này là đã thanh toán?")
                    .setPositiveButton("Đồng ý", (dialog, which) -> {
                        viewModel.markReminderAsCompleted(
                                reminder.getDocumentId(),
                                reminder.getId(),
                                notificationService
                        );
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            Log.e(TAG, "Cannot mark as paid: Invalid document ID for reminder: " + reminder.getTitle());
            Toast.makeText(requireContext(), "Không thể đánh dấu: ID không hợp lệ", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onReminderClick(Reminder reminder) {
        // Chỉ cho phép chỉnh sửa nhắc nhở sắp tới (chưa hoàn thành)
        if (reminder.isCompleted() || reminder.isOverdue()) {
            Toast.makeText(requireContext(), "Không thể chỉnh sửa nhắc nhở đã qua", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle args = new Bundle();
        args.putLong("reminderId", reminder.getId());
        args.putString("documentId", reminder.getDocumentId());
        Navigation.findNavController(requireView()).navigate(
                R.id.action_reminders_to_edit_reminder, args);
    }


    @Override
    public void onResume() {
        super.onResume();
        updateReminderList(tabLayout.getSelectedTabPosition());
    }
    private void setupSwipeToDelete() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(requireContext(), new SwipeToDeleteCallback.SwipeActionListener() {
            @Override
            public void onDelete(int position) {
                // Lấy reminder tại vị trí position
                Reminder reminder = adapter.getReminderAt(position);
                if (reminder != null) {
                    // Hiển thị dialog xác nhận xóa
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Xóa nhắc nhở")
                            .setMessage("Bạn có chắc chắn muốn xóa nhắc nhở này?")
                            .setPositiveButton("Xóa", (dialog, which) -> {
                                // Xóa reminder
                                if (reminder.getDocumentId() != null) {
                                    viewModel.deleteReminder(
                                            reminder.getDocumentId(),
                                            reminder.getId(),
                                            notificationService
                                    );
                                    Toast.makeText(requireContext(), "Đã xóa nhắc nhở", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(requireContext(), "Không thể xóa: ID không hợp lệ", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Hủy", (dialog, which) -> {
                                // Hủy xóa, refresh adapter để hiển thị lại item
                                adapter.notifyItemChanged(position);
                            })
                            .setOnCancelListener(dialog -> {
                                // Nếu dialog bị hủy, refresh adapter
                                adapter.notifyItemChanged(position);
                            })
                            .show();
                }
            }

            @Override
            public void onEdit(int position) {
                // Lấy reminder tại vị trí position
                Reminder reminder = adapter.getReminderAt(position);
                if (reminder != null) {
                    // Chỉ cho phép chỉnh sửa nhắc nhở sắp tới (chưa hoàn thành)
                    if (reminder.isCompleted() || reminder.isOverdue()) {
                        Toast.makeText(requireContext(), "Không thể chỉnh sửa nhắc nhở đã qua", Toast.LENGTH_SHORT).show();
                        adapter.notifyItemChanged(position);
                        return;
                    }

                    // Chuyển đến màn hình chỉnh sửa
                    Bundle args = new Bundle();
                    args.putLong("reminderId", reminder.getId());
                    args.putString("documentId", reminder.getDocumentId());
                    Navigation.findNavController(requireView()).navigate(
                            R.id.action_reminders_to_edit_reminder, args);
                }
            }
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }


}
