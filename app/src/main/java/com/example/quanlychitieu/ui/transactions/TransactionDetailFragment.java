package com.example.quanlychitieu.ui.transactions;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.databinding.FragmentTransactionDetailBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransactionDetailFragment extends Fragment {

    private FragmentTransactionDetailBinding binding;
    private TransactionsViewModel viewModel;
    private String transactionId;
    private Transaction currentTransaction;

    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(TransactionsViewModel.class);

        // Get transaction ID from arguments
        if (getArguments() != null) {
            transactionId = getArguments().getString("transaction_id");
            if (transactionId != null && !transactionId.isEmpty()) {
                loadTransactionDetails(transactionId);
            } else {
                showError("Không tìm thấy thông tin giao dịch");
            }
        } else {
            showError("Không tìm thấy thông tin giao dịch");
        }

        setupToolbar();
        setupActionButtons();
    }

    private void loadTransactionDetails(String id) {
        // Show loading state
        showLoading(true);

        viewModel.getTransactionById(id).observe(getViewLifecycleOwner(), transaction -> {
            // Hide loading state
            showLoading(false);

            if (transaction != null) {
                currentTransaction = transaction;
                displayTransactionDetails(transaction);
            } else {
                showError("Không tìm thấy thông tin giao dịch");
            }
        });
    }

    private void displayTransactionDetails(Transaction transaction) {
        // Set transaction type indicator and color
        boolean isIncome = transaction.isIncome();
        binding.transactionTypeIndicator.setBackgroundResource(
                isIncome ? R.drawable.bg_income_indicator : R.drawable.bg_expense_indicator);
        binding.transactionTypeText.setText(isIncome ? "Thu nhập" : "Chi tiêu");

        // Set category and icon
        binding.categoryName.setText(transaction.getCategory());
        setCategoryIcon(transaction.getCategory(), isIncome);

        // Set amount with formatting
        double amount = Math.abs(transaction.getAmount());
        DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(new Locale("vi", "VN"));
        formatter.applyPattern("#,###");
        String formattedAmount = formatter.format(amount).replace(",", ".");
        binding.amountValue.setText(formattedAmount + " đ");
        binding.amountValue.setTextColor(getResources().getColor(
                isIncome ? R.color.income_green : R.color.expense_red, null));

        // Set date and time
        binding.dateTimeValue.setText(dateTimeFormatter.format(transaction.getDate()));

        // Set description/note
        String note = transaction.getNote();
        if (note != null && !note.isEmpty()) {
            binding.noteContainer.setVisibility(View.VISIBLE);
            binding.noteValue.setText(note);
        } else {
            binding.noteContainer.setVisibility(View.GONE);
        }

        // Set recurring info if applicable
        if (transaction.isRepeat()) {
            binding.recurringContainer.setVisibility(View.VISIBLE);
            binding.recurringValue.setText("Có (Hàng tháng)"); // You may need to adjust based on your data model
        } else {
            binding.recurringContainer.setVisibility(View.GONE);
        }
    }

    private void setCategoryIcon(String category, boolean isIncome) {
        // Set the appropriate icon based on the category
        int iconResId;

        switch (category.toLowerCase()) {
            case "ăn uống":
                iconResId = R.drawable.ic_food;
                break;
            case "di chuyển":
                iconResId = R.drawable.ic_move;
                break;
            case "mua sắm":
                iconResId = R.drawable.ic_shopping;
                break;
            case "hóa đơn":
                iconResId = R.drawable.ic_invoice;
                break;
            case "lương":
                iconResId = R.drawable.ic_salary;
                break;
            case "thưởng":
                iconResId = R.drawable.ic_bonus;
                break;
            case "quà tặng":
                iconResId = R.drawable.ic_gift;
                break;
            case "khác":
                iconResId = R.drawable.ic_more;
                break;
            default:
                iconResId = isIncome ? R.drawable.ic_income : R.drawable.ic_expense;
                break;
        }
        binding.categoryIcon.setImageResource(iconResId);
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> {
            // Navigate back
            Navigation.findNavController(requireView()).popBackStack();
        });
    }

    private void setupActionButtons() {
        // Edit button
        binding.editButton.setOnClickListener(v -> {
            if (currentTransaction != null) {
                Bundle args = new Bundle();
                args.putString("transaction_id", currentTransaction.getFirebaseId());
                Navigation.findNavController(requireView()).navigate(
                        R.id.action_transaction_detail_to_edit_transaction, args);
            }
        });

        // Delete button
        binding.deleteButton.setOnClickListener(v -> {
            if (currentTransaction != null) {
                showDeleteConfirmationDialog(currentTransaction);
            }
        });
    }

    private void showDeleteConfirmationDialog(Transaction transaction) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Xóa giao dịch")
                .setMessage("Bạn có chắc chắn muốn xóa giao dịch này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Show loading while deleting
                    showLoading(true);
                    viewModel.deleteTransaction(transaction.getFirebaseId());
                    Toast.makeText(requireContext(), "Đã xóa giao dịch", Toast.LENGTH_SHORT).show();
                    // Navigate back after deletion
                    Navigation.findNavController(requireView()).popBackStack();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.contentContainer.setVisibility(isLoading ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.contentContainer.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.errorMessage.setText(message);

        binding.retryButton.setOnClickListener(v -> {
            if (transactionId != null && !transactionId.isEmpty()) {
                binding.errorContainer.setVisibility(View.GONE);
                loadTransactionDetails(transactionId);
            } else {
                Navigation.findNavController(requireView()).popBackStack();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
