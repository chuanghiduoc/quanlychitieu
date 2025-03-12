package com.example.quanlychitieu.ui.budget;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.BudgetAdapter;
import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.databinding.FragmentBudgetBinding;

import java.text.NumberFormat;
import java.util.Locale;

public class BudgetFragment extends Fragment {

    private FragmentBudgetBinding binding;
    private BudgetViewModel viewModel;
    private BudgetAdapter adapter;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

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

        // Observe total budget data
        observeTotalBudget();

        // Observe category budgets
        observeCategoryBudgets();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.budgetsRecyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Initialize adapter with click listener
        adapter = new BudgetAdapter(budget -> {
            // Handle budget item click - navigate to edit budget
            Toast.makeText(requireContext(), "Edit budget: " + budget.getCategory(), Toast.LENGTH_SHORT).show();

            // Example navigation (uncomment when you have the navigation set up)
            // Bundle args = new Bundle();
            // args.putLong("budgetId", budget.getId());
            // Navigation.findNavController(requireView()).navigate(R.id.action_budget_to_add_edit_budget, args);
        });

        recyclerView.setAdapter(adapter);
    }

    private void setupAddBudgetButton() {
        binding.fabAddBudget.setOnClickListener(v -> {
            // Navigate to add budget screen
            Toast.makeText(requireContext(), "Add new budget", Toast.LENGTH_SHORT).show();

            // Example navigation (uncomment when you have the navigation set up)
            // Navigation.findNavController(v).navigate(R.id.action_budget_to_add_budget);
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

            // Show/hide empty state
            if (budgets.isEmpty()) {
                binding.emptyState.setVisibility(View.VISIBLE);
                binding.budgetsRecyclerView.setVisibility(View.GONE);
            } else {
                binding.emptyState.setVisibility(View.GONE);
                binding.budgetsRecyclerView.setVisibility(View.VISIBLE);
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
