package com.example.quanlychitieu.ui.budget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.BudgetAdapter;
import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.databinding.FragmentBudgetBinding;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BudgetFragment extends Fragment {

    private FragmentBudgetBinding binding;
    private BudgetViewModel viewModel;
    private BudgetAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBudgetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(BudgetViewModel.class);

        // Setup RecyclerView
        setupRecyclerView();

        // Setup FAB
        setupAddBudgetButton();

        // Update title with current month/year
        updateTitleWithCurrentMonth();

        // Observe total budget data
        observeTotalBudget();

        // Observe category budgets
        observeCategoryBudgets();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when returning to this screen
        viewModel.refreshBudgets();
    }

    private void updateTitleWithCurrentMonth() {
        // Get current month and year
        Calendar calendar = Calendar.getInstance();
        String monthYear = monthYearFormat.format(calendar.getTime());

        // Update the title text to include current month/year
        TextView titleTextView = binding.totalBudgetCard.findViewById(android.R.id.text1);
        if (titleTextView != null) {
            titleTextView.setText("Tổng ngân sách tháng " + monthYear);
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.budgetsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize adapter with click listener
        adapter = new BudgetAdapter(budget -> {
            // Navigate to edit budget screen
            Bundle args = new Bundle();
            args.putString("budget_id", budget.getFirebaseId());

            // Check if navigation action exists
            if (Navigation.findNavController(requireView()).getCurrentDestination().getAction(R.id.action_budget_to_add_budget) != null) {
                Navigation.findNavController(requireView()).navigate(
                        R.id.action_budget_to_add_budget, args);
            } else {
                Toast.makeText(requireContext(), "Chỉnh sửa ngân sách: " + budget.getCategory(), Toast.LENGTH_SHORT).show();
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void setupAddBudgetButton() {
        binding.fabAddBudget.setOnClickListener(v -> {
            // Navigate to add budget screen
            if (Navigation.findNavController(v).getCurrentDestination().getAction(R.id.action_budget_to_add_budget) != null) {
                Navigation.findNavController(v).navigate(R.id.action_budget_to_add_budget);
            } else {
                Toast.makeText(requireContext(), "Thêm ngân sách mới", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void observeTotalBudget() {
        // Observe total budget amount
        viewModel.getTotalBudget().observe(getViewLifecycleOwner(), totalBudget -> {
            binding.totalBudget.setText(formatCurrency(totalBudget));
        });

        // Observe total spent amount
        viewModel.getTotalSpent().observe(getViewLifecycleOwner(), totalSpent -> {
            binding.totalSpent.setText(formatCurrency(totalSpent));
        });

        // Observe remaining amount
        viewModel.getRemainingAmount().observe(getViewLifecycleOwner(), remaining -> {
            double totalBudget = viewModel.getTotalBudget().getValue() != null ?
                    viewModel.getTotalBudget().getValue() : 0;

            int percentage = totalBudget > 0 ?
                    (int) (remaining / totalBudget * 100) : 0;

            binding.totalBudgetRemaining.setText(
                    String.format("Còn lại: %s (%d%%)", formatCurrency(remaining), percentage));
        });

        // Observe progress percentage
        viewModel.getProgressPercentage().observe(getViewLifecycleOwner(), progress -> {
            binding.totalBudgetProgress.setProgress(progress);
        });
    }

    private void observeCategoryBudgets() {
        viewModel.getCategoryBudgets().observe(getViewLifecycleOwner(), budgets -> {
            adapter.submitList(budgets);

            // Show loading state
            boolean isLoading = viewModel.getIsLoading().getValue() != null &&
                    viewModel.getIsLoading().getValue();

            // Show/hide empty state
            if (!isLoading && (budgets == null || budgets.isEmpty())) {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.budgetsRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyState.setVisibility(View.GONE);
                binding.budgetsRecyclerView.setVisibility(View.VISIBLE);
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // You can add a progress indicator here if needed
            if (isLoading) {
                // Show loading indicator
                binding.budgetsRecyclerView.setVisibility(View.GONE);
            } else {
                // Check if we have data to show
                List<Budget> budgets = viewModel.getCategoryBudgets().getValue();
                if (budgets != null && !budgets.isEmpty()) {
                    binding.budgetsRecyclerView.setVisibility(View.VISIBLE);
                    binding.emptyState.setVisibility(View.GONE);
                } else {
                    binding.budgetsRecyclerView.setVisibility(View.GONE);
                    binding.emptyState.setVisibility(View.VISIBLE);
                }
            }
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
