package com.example.quanlychitieu.ui.goals;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.model.FinancialGoal;
import com.example.quanlychitieu.data.repository.FinancialGoalRepository;

import java.util.List;

public class GoalsViewModel extends ViewModel {
    private final FinancialGoalRepository repository;

    public GoalsViewModel() {
        repository = FinancialGoalRepository.getInstance();
    }

    public LiveData<List<FinancialGoal>> getGoals() {
        return repository.getGoals();
    }

    public LiveData<FinancialGoal> getGoalById(String goalId) {
        return repository.getGoalById(goalId);
    }

    public void addGoal(FinancialGoal goal) {
        repository.addGoal(goal);
    }

    public void updateGoal(FinancialGoal goal) {
        repository.updateGoal(goal);
    }

    public void deleteGoal(String goalId) {
        repository.deleteGoal(goalId);
    }

    public void contributeToGoal(String goalId, double amount) {
        // Lấy mục tiêu hiện tại
        FinancialGoal goal = repository.getGoalById(goalId).getValue();
        if (goal != null) {
            // Cập nhật số tiền hiện tại
            double newAmount = goal.getCurrentAmount() + amount;
            goal.setCurrentAmount(newAmount);

            // Kiểm tra xem mục tiêu đã hoàn thành chưa
            if (newAmount >= goal.getTargetAmount()) {
                goal.setCompleted(true);
            }

            // Cập nhật mục tiêu
            repository.updateGoal(goal);
        }

        // Tạo giao dịch đóng góp
        repository.createContributionTransaction(goalId, amount);
    }
}