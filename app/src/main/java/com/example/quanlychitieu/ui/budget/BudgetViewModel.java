package com.example.quanlychitieu.ui.budget;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.data.repository.BudgetRepository;

import java.util.List;

public class BudgetViewModel extends ViewModel {

    private final BudgetRepository repository;
    private final MediatorLiveData<List<Budget>> categoryBudgets = new MediatorLiveData<>();
    private final MediatorLiveData<Double> totalBudget = new MediatorLiveData<>();
    private final MediatorLiveData<Double> totalSpent = new MediatorLiveData<>();
    private final MutableLiveData<Double> remainingAmount = new MutableLiveData<>(0.0);
    private final MutableLiveData<Integer> progressPercentage = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    // Keep track of current active sources
    private LiveData<List<Budget>> currentBudgetsSource = null;
    private LiveData<Double> currentTotalBudgetSource = null;
    private LiveData<Double> currentTotalSpentSource = null;

    public BudgetViewModel() {
        repository = BudgetRepository.getInstance();

        // Initialize with default values
        totalBudget.setValue(0.0);
        totalSpent.setValue(0.0);

        // Load active budgets for the current month
        loadActiveBudgets();
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

    private void loadActiveBudgets() {
        isLoading.setValue(true);

        // Get active budgets from repository
        LiveData<List<Budget>> activeBudgets = repository.getActiveBudgets();

        // Remove previous source if it exists
        if (currentBudgetsSource != null) {
            categoryBudgets.removeSource(currentBudgetsSource);
        }

        // Add new source
        categoryBudgets.addSource(activeBudgets, budgets -> {
            categoryBudgets.setValue(budgets);
            isLoading.setValue(false);
        });

        // Update current source reference
        currentBudgetsSource = activeBudgets;

        // Observe total budget and spent amounts
        observeTotals();
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
        loadActiveBudgets();
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
        return categoryBudgets;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
}
