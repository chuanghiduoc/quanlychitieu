package com.example.quanlychitieu.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.TransactionAdapter;
import com.example.quanlychitieu.auth.ProfileActivity;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.databinding.FragmentDashboardBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class DashboardFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private TransactionAdapter recentTransactionsAdapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private static final String TAG = "DashboardFragment";
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo NavController
        navController = Navigation.findNavController(view);
        // Khởi tạo ViewModel một lần
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Thiết lập RecyclerView cho các giao dịch gần đây
        setupRecentTransactionsRecyclerView();

        // Thiết lập click listener cho "View All" giao dịch
        binding.viewAllTransactions.setOnClickListener(v -> {
            // Chuyển đến Transactions không xoá back stack
            navController.navigate(R.id.action_dashboard_to_transactions, null);
        });

        // Thiết lập click listener cho hình ảnh profile
        binding.profileImage.setOnClickListener(v -> {
            try {
                // Start ProfileActivity instead of navigating to a fragment
                Intent intent = new Intent(requireActivity(), ProfileActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Navigation to profile failed: " + e.getMessage(), e);
            }
        });

        // Quan sát dữ liệu tài chính
        observeFinancialData();

        // Quan sát các giao dịch gần đây
        observeRecentTransactions();
    }

    private void setupRecentTransactionsRecyclerView() {
        RecyclerView recyclerView = binding.recentTransactionsRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Khởi tạo adapter với this làm OnTransactionClickListener
        recentTransactionsAdapter = new TransactionAdapter(this);
        recyclerView.setAdapter(recentTransactionsAdapter);
    }

    private void observeFinancialData() {
        dashboardViewModel.getIncome().observe(getViewLifecycleOwner(), income -> {
            String formattedIncome = formatCurrency(income);
            binding.incomeAmount.setText(formattedIncome);
        });

        dashboardViewModel.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            String formattedExpenses = formatCurrency(expenses);
            binding.expenseAmount.setText(formattedExpenses);
        });

        dashboardViewModel.getBalance().observe(getViewLifecycleOwner(), balance -> {
            String formattedBalance = formatCurrency(balance);
            binding.balanceAmount.setText(formattedBalance);
        });
    }

    private void observeRecentTransactions() {
        dashboardViewModel.getRecentTransactions().observe(getViewLifecycleOwner(), transactions -> {
            recentTransactionsAdapter.submitList(transactions);
        });
    }

    private String formatCurrency(double amount) {
        return currencyFormat.format(amount)
                .replace("₫", "đ")
                .replace(",", ".");
    }

    // Implement OnTransactionClickListener methods
    @Override
    public void onTransactionClick(Transaction transaction) {
        // Navigate to transaction details or edit screen
        Bundle args = new Bundle();
        args.putString("transaction_id", transaction.getFirebaseId());
        navController.navigate(R.id.action_dashboard_to_add_transaction, args);
    }

    @Override
    public void onEditClick(Transaction transaction) {
        // Navigate to edit transaction screen
        Bundle args = new Bundle();
        args.putString("transaction_id", transaction.getFirebaseId());
        navController.navigate(R.id.action_dashboard_to_add_transaction, args);
    }

    @Override
    public void onDeleteClick(Transaction transaction) {
        // Handle delete action if needed in dashboard
        // For dashboard, you might want to just show a toast and not allow deletion
        Toast.makeText(requireContext(), "Vui lòng vào trang Giao dịch để xóa", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
