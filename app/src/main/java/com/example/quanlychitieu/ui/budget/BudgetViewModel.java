package com.example.quanlychitieu.ui.budget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.CategoryManager;
import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.data.repository.BudgetRepository;
import com.example.quanlychitieu.data.repository.TransactionRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BudgetViewModel extends ViewModel {

    private final BudgetRepository repository;
    private final TransactionRepository transactionRepository;
    private final MediatorLiveData<List<Budget>> displayBudgets = new MediatorLiveData<>();
    private final MediatorLiveData<Double> totalBudget = new MediatorLiveData<>();
    private final MediatorLiveData<Double> totalSpent = new MediatorLiveData<>();
    private final MutableLiveData<Double> remainingAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> progressPercentage = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    // Keep track of current active sources
    private LiveData<List<Budget>> currentBudgetsSource = null;
    private LiveData<Double> currentTotalBudgetSource = null;
    private LiveData<Double> currentTotalSpentSource = null;
    private LiveData<Map<String, Double>> currentCategorySpentSource = null;

    public BudgetViewModel() {
        repository = BudgetRepository.getInstance();
        transactionRepository = TransactionRepository.getInstance();

        // Initialize with default values
        totalBudget.setValue(0.0);
        totalSpent.setValue(0.0);

        // Load active budgets and combine with all categories
        loadBudgetsWithAllCategories();
    }

    private void updateRemainingAndProgress() {
        Double budget = totalBudget.getValue();
        Double spent = totalSpent.getValue();

        if (budget != null && spent != null) {
            double remaining = budget - spent;
            remainingAmount.setValue(remaining);

            int progress = budget > 0 ? (int) ((spent / budget) * 100) : 0;
            progressPercentage.setValue(progress);
        }
    }

    private void loadBudgetsWithAllCategories() {
        isLoading.setValue(true);

        // Get active budgets from repository
        LiveData<List<Budget>> activeBudgets = repository.getActiveBudgets();

        // Remove previous source if it exists
        if (currentBudgetsSource != null) {
            displayBudgets.removeSource(currentBudgetsSource);
        }

        // Add new source
        displayBudgets.addSource(activeBudgets, budgets -> {
            // Combine with all expense categories
            createCompleteBudgetsList(budgets);
            isLoading.setValue(false);
        });

        // Update current source reference
        currentBudgetsSource = activeBudgets;

        // Observe category spent amounts
        observeCategorySpentAmounts();

        // Observe total budget and spent amounts
        observeTotals();
    }

    private void createCompleteBudgetsList(List<Budget> activeBudgets) {
        // Get all expense categories
        List<String> allCategories = CategoryManager.getInstance().getExpenseCategories();

        // Create a map of existing budgets by category
        Map<String, Budget> budgetMap = new HashMap<>();
        if (activeBudgets != null) {
            for (Budget budget : activeBudgets) {
                budgetMap.put(budget.getCategory(), budget);
            }
        }

        // Create a complete list with all categories
        List<Budget> completeBudgetsList = new ArrayList<>();

        // Get category spent amounts
        Map<String, Double> categorySpentAmounts = repository.getCategorySpentAmounts().getValue();
        if (categorySpentAmounts == null) {
            categorySpentAmounts = new HashMap<>();
        }

        // Add all categories, using existing budgets when available
        for (String category : allCategories) {
            Budget budget = budgetMap.get(category);

            if (budget == null) {
                // Create a placeholder budget with 0 amount
                budget = new Budget();
                budget.setCategory(category);
                budget.setAmount(0);

                // Set spent amount from transactions
                Double spentAmount = categorySpentAmounts.getOrDefault(category, 0.0);
                budget.setSpent(spentAmount);
            }

            completeBudgetsList.add(budget);
        }

        // Update the display budgets
        displayBudgets.setValue(completeBudgetsList);
    }

    private void observeCategorySpentAmounts() {
        // Observe category spent amounts from repository
        LiveData<Map<String, Double>> categorySpentAmounts = repository.getCategorySpentAmounts();

        // Remove previous source if it exists
        if (currentCategorySpentSource != null) {
            displayBudgets.removeSource(currentCategorySpentSource);
        }

        // Add new source
        displayBudgets.addSource(categorySpentAmounts, spentAmounts -> {
            // Update the budgets list with new spent amounts
            List<Budget> currentBudgets = displayBudgets.getValue();
            if (currentBudgets != null) {
                for (Budget budget : currentBudgets) {
                    Double spentAmount = spentAmounts.getOrDefault(budget.getCategory(), 0.0);
                    budget.setSpent(spentAmount);
                }
                displayBudgets.setValue(currentBudgets);
            }
        });

        // Update current source reference
        currentCategorySpentSource = categorySpentAmounts;
    }

    private void observeTotals() {
        // Observe the total budget amount from repository
        LiveData<Double> repositoryTotalBudget = repository.getTotalBudget();

        // Remove previous source if it exists
        if (currentTotalBudgetSource != null) {
            totalBudget.removeSource(currentTotalBudgetSource);
        }

        // Add new source
        totalBudget.addSource(repositoryTotalBudget, value -> {
            totalBudget.setValue(value);
            updateRemainingAndProgress();
        });

        // Update current source reference
        currentTotalBudgetSource = repositoryTotalBudget;

        // Observe the total spent amount from repository
        LiveData<Double> repositoryTotalSpent = repository.getTotalSpent();

        // Remove previous source if it exists
        if (currentTotalSpentSource != null) {
            totalSpent.removeSource(currentTotalSpentSource);
        }

        // Add new source
        totalSpent.addSource(repositoryTotalSpent, value -> {
            totalSpent.setValue(value);
            updateRemainingAndProgress();
        });

        // Update current source reference
        currentTotalSpentSource = repositoryTotalSpent;
    }

    // Method to get a specific budget by ID
    public LiveData<Budget> getBudgetById(String budgetId) {
        return repository.getBudgetById(budgetId);
    }

    // Method to add a new budget
    public void addBudget(Budget budget) {
        repository.addBudget(budget);
    }

    // Method to update a budget
    public void updateBudget(Budget budget) {
        repository.updateBudget(budget);
    }

    // Method to delete a budget
    public void deleteBudget(String budgetId) {
        repository.deleteBudget(budgetId);
    }

    // Method to refresh data
    public void refreshBudgets() {
        // Reload active budgets
        loadBudgetsWithAllCategories();
    }

    public LiveData<Double> getTotalBudget() {
        return totalBudget;
    }

    public LiveData<Double> getTotalSpent() {
        return totalSpent;
    }

    public LiveData<Double> getRemainingAmount() {
        return remainingAmount;
    }

    public LiveData<Integer> getProgressPercentage() {
        return progressPercentage;
    }

    public LiveData<List<Budget>> getCategoryBudgets() {
        return displayBudgets;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}