package com.example.quanlychitieu.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.BudgetCategoryAdapter;
import com.example.quanlychitieu.adapter.TransactionAdapter;
import com.example.quanlychitieu.auth.ProfileActivity;
import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.databinding.FragmentDashboardBinding;
import com.github.mikephil.charting.charts.PieChart;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

public class DashboardFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private FragmentDashboardBinding binding;
    private DashboardViewModel dashboardViewModel;
    private TransactionAdapter recentTransactionsAdapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private static final String TAG = "DashboardFragment";
    private NavController navController;
    private PieChart expenseChart;
    private PieChart budgetChart;
    private BudgetCategoryAdapter categoryAdapter;
    private RecyclerView categoryRecyclerView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize NavController
        navController = Navigation.findNavController(view);

        // Initialize ViewModel
        dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // Setup RecyclerView for recent transactions
        setupRecentTransactionsRecyclerView();

        // Setup click listener for "View All" transactions
        binding.viewAllTransactions.setOnClickListener(v -> {
            navController.navigate(R.id.action_dashboard_to_transactions, null);
        });

        // Setup click listener for profile image
        binding.profileImage.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(requireActivity(), ProfileActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Navigation to profile failed: " + e.getMessage(), e);
            }
        });

        // Khởi tạo biểu đồ
        setupCharts();

        // Khởi tạo RecyclerView cho danh sách danh mục
        setupCategoryList();

        // Observe financial data
        observeFinancialData();

        // Observe recent transactions
        observeRecentTransactions();

        // Observe chart data
        observeChartData();
    }

    @Override
    public void onResume() {
        super.onResume();
        dashboardViewModel.refreshData();

        // Cập nhật lại biểu đồ nếu cần
        if (expenseChart != null && budgetChart != null) {
            expenseChart.invalidate();
            budgetChart.invalidate();
        }
    }
    private void setupCharts() {
        // Khởi tạo biểu đồ chi tiêu
        expenseChart = new PieChart(requireContext());
        binding.chartContainer.addView(expenseChart);
        ChartHelper.setupPieChart(expenseChart);
        expenseChart.setNoDataText("Đang tải dữ liệu...");

        // Khởi tạo biểu đồ ngân sách
        budgetChart = new PieChart(requireContext());
        binding.budgetChartContainer.addView(budgetChart);
        ChartHelper.setupPieChart(budgetChart);
        budgetChart.setNoDataText("Đang tải dữ liệu...");
    }

    private void setupCategoryList() {
        // Thay thế LinearLayout với RecyclerView
        categoryRecyclerView = new RecyclerView(requireContext());
        categoryRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        binding.categoryList.addView(categoryRecyclerView);

        categoryRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoryAdapter = new BudgetCategoryAdapter(requireContext());
        categoryRecyclerView.setAdapter(categoryAdapter);
    }




    private void observeChartData() {
        // Theo dõi cả dữ liệu chi tiêu và ngân sách
        MediatorLiveData<Pair<Map<String, Double>, Map<String, Double>>> combinedData = new MediatorLiveData<>();

        combinedData.addSource(dashboardViewModel.getCategoryExpensesData(), expenses -> {
            Map<String, Double> budgets = dashboardViewModel.getCategoryBudgetsData().getValue();
            if (budgets != null) {
                combinedData.setValue(new Pair<>(expenses, budgets));
            }
        });

        combinedData.addSource(dashboardViewModel.getCategoryBudgetsData(), budgets -> {
            Map<String, Double> expenses = dashboardViewModel.getCategoryExpensesData().getValue();
            if (expenses != null) {
                combinedData.setValue(new Pair<>(expenses, budgets));
            }
        });

        // Quan sát dữ liệu kết hợp
        combinedData.observe(getViewLifecycleOwner(), data -> {
            if (data != null) {
                Map<String, Double> expenses = data.first;
                Map<String, Double> budgets = data.second;

                // Cập nhật biểu đồ
                ChartHelper.updateExpenseChart(expenseChart, expenses, requireContext());
                ChartHelper.updateBudgetChart(budgetChart, budgets, requireContext());

                // Cập nhật danh sách danh mục
                categoryAdapter.updateData(expenses, budgets);
            }
        });
    }

    private void setupRecentTransactionsRecyclerView() {
        RecyclerView recyclerView = binding.recentTransactionsRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize adapter with this as OnTransactionClickListener
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
            if (transactions != null) {
                recentTransactionsAdapter.submitList(transactions);

                if (transactions.isEmpty()) {
                    // Hiển thị thông báo "Không có giao dịch nào"
                    binding.noTransactionsText.setVisibility(View.VISIBLE);
                    binding.recentTransactionsRecycler.setVisibility(View.GONE);

                    // Ẩn layout tĩnh nếu có
                    if (binding.staticTransactions != null) {
                        binding.staticTransactions.setVisibility(View.GONE);
                    }
                } else {
                    // Hiển thị danh sách giao dịch
                    binding.noTransactionsText.setVisibility(View.GONE);
                    binding.recentTransactionsRecycler.setVisibility(View.VISIBLE);

                    // Ẩn layout tĩnh nếu có
                    if (binding.staticTransactions != null) {
                        binding.staticTransactions.setVisibility(View.GONE);
                    }
                }
            }
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
        // Navigate to transaction details
        Bundle args = new Bundle();
        args.putString("transaction_id", transaction.getFirebaseId());
        navController.navigate(R.id.action_dashboard_to_transaction_detail, args);
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
        // Show a toast message instead of deleting
        Toast.makeText(requireContext(), "Vui lòng vào trang Giao dịch để xóa", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
