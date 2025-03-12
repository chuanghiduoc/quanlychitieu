package com.example.quanlychitieu.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.TransactionAdapter;
import com.example.quanlychitieu.databinding.FragmentDashboardBinding;
import com.example.quanlychitieu.data.model.Transaction;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private TransactionAdapter recentTransactionsAdapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Setup recent transactions RecyclerView
        setupRecentTransactionsRecyclerView();

        // Setup click listener for "View All" transactions
        binding.viewAllTransactions.setOnClickListener(v -> {
            NavOptions navOptions = new NavOptions.Builder()
                    .setPopUpTo(R.id.navigation_dashboard, false)
                    .build();

            Navigation.findNavController(view).navigate(R.id.navigation_transactions, null, navOptions);
        });

        // Setup click listener for profile image
        binding.profileImage.setOnClickListener(v -> {
            Navigation.findNavController(view).navigate(R.id.profileFragment);
        });

        // Observe financial data
        observeFinancialData();

        // Observe recent transactions
        observeRecentTransactions();
    }

    private void setupRecentTransactionsRecyclerView() {
        RecyclerView recyclerView = binding.recentTransactionsRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize adapter
        recentTransactionsAdapter = new TransactionAdapter();
        recyclerView.setAdapter(recentTransactionsAdapter);
    }

    private void observeFinancialData() {
        // Observe income
        dashboardViewModel.getIncome().observe(getViewLifecycleOwner(), income -> {
            String formattedIncome = formatCurrency(income);
            binding.incomeAmount.setText(formattedIncome);
        });

        // Observe expenses
        dashboardViewModel.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            String formattedExpenses = formatCurrency(expenses);
            binding.expenseAmount.setText(formattedExpenses);
        });

        // Observe balance
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
