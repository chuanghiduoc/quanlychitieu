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

    private final BudgetRepository repository; // Kho lưu trữ ngân sách
    private final TransactionRepository transactionRepository; // Kho lưu trữ giao dịch
    private final MediatorLiveData<List<Budget>> displayBudgets = new MediatorLiveData<>(); // LiveData trung gian để hiển thị danh sách ngân sách
    private final MediatorLiveData<Double> totalBudget = new MediatorLiveData<>(); // LiveData trung gian cho tổng ngân sách
    private final MediatorLiveData<Double> totalSpent = new MediatorLiveData<>(); // LiveData trung gian cho tổng chi tiêu
    private final MutableLiveData<Double> remainingAmount = new MutableLiveData<>(0.0); // LiveData có thể thay đổi cho số tiền còn lại
    private final MutableLiveData<Integer> progressPercentage = new MutableLiveData<>(0); // LiveData có thể thay đổi cho phần trăm tiến độ
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true); // LiveData có thể thay đổi cho trạng thái đang tải

    // Theo dõi các nguồn dữ liệu hiện đang hoạt động
    private LiveData<List<Budget>> currentBudgetsSource = null; // Nguồn LiveData hiện tại cho danh sách ngân sách
    private LiveData<Double> currentTotalBudgetSource = null; // Nguồn LiveData hiện tại cho tổng ngân sách
    private LiveData<Double> currentTotalSpentSource = null; // Nguồn LiveData hiện tại cho tổng chi tiêu
    private LiveData<Map<String, Double>> currentCategorySpentSource = null; // Nguồn LiveData hiện tại cho số tiền đã chi theo danh mục

    public BudgetViewModel() {
        repository = BudgetRepository.getInstance(); // Lấy instance của BudgetRepository
        transactionRepository = TransactionRepository.getInstance(); // Lấy instance của TransactionRepository

        // Khởi tạo với các giá trị mặc định
        totalBudget.setValue(0.0);
        totalSpent.setValue(0.0);

        // Tải ngân sách đang hoạt động và kết hợp với tất cả các danh mục
        loadBudgetsWithAllCategories();
    }

    private void updateRemainingAndProgress() {
        Double budget = totalBudget.getValue(); // Lấy giá trị tổng ngân sách
        Double spent = totalSpent.getValue(); // Lấy giá trị tổng chi tiêu

        if (budget != null && spent != null) {
            double remaining = budget - spent; // Tính số tiền còn lại
            remainingAmount.setValue(remaining);

            int progress = budget > 0 ? (int) ((spent / budget) * 100) : 0; // Tính phần trăm tiến độ
            progressPercentage.setValue(progress);
        }
    }

    private void loadBudgetsWithAllCategories() {
        isLoading.setValue(true); // Đặt trạng thái đang tải là true

        // Lấy ngân sách đang hoạt động từ repository
        LiveData<List<Budget>> activeBudgets = repository.getActiveBudgets();

        // Xóa nguồn dữ liệu trước đó nếu có
        if (currentBudgetsSource != null) {
            displayBudgets.removeSource(currentBudgetsSource);
        }

        // Thêm nguồn dữ liệu mới
        displayBudgets.addSource(activeBudgets, budgets -> {
            // Kết hợp với tất cả các danh mục chi tiêu
            createCompleteBudgetsList(budgets);
            isLoading.setValue(false); // Đặt trạng thái đang tải là false sau khi dữ liệu được tải
        });

        // Cập nhật tham chiếu nguồn dữ liệu hiện tại
        currentBudgetsSource = activeBudgets;

        // Theo dõi số tiền đã chi theo danh mục
        observeCategorySpentAmounts();

        // Theo dõi tổng ngân sách và tổng số tiền đã chi
        observeTotals();
    }

    private void createCompleteBudgetsList(List<Budget> activeBudgets) {
        // Lấy tất cả các danh mục chi tiêu
        List<String> allCategories = CategoryManager.getInstance().getExpenseCategories();

        // Tạo một map chứa các ngân sách hiện có theo danh mục
        Map<String, Budget> budgetMap = new HashMap<>();
        if (activeBudgets != null) {
            for (Budget budget : activeBudgets) {
                budgetMap.put(budget.getCategory(), budget);
            }
        }

        // Tạo một danh sách hoàn chỉnh với tất cả các danh mục
        List<Budget> completeBudgetsList = new ArrayList<>();

        // Lấy số tiền đã chi theo danh mục
        Map<String, Double> categorySpentAmounts = repository.getCategorySpentAmounts().getValue();
        if (categorySpentAmounts == null) {
            categorySpentAmounts = new HashMap<>();
        }

        // Thêm tất cả các danh mục, sử dụng ngân sách hiện có nếu có
        for (String category : allCategories) {
            Budget budget = budgetMap.get(category);

            if (budget == null) {
                // Tạo một ngân sách giữ chỗ với số tiền là 0
                budget = new Budget();
                budget.setCategory(category);
                budget.setAmount(0);

                // Đặt số tiền đã chi từ các giao dịch
                Double spentAmount = categorySpentAmounts.getOrDefault(category, 0.0);
                budget.setSpent(spentAmount);
            }

            completeBudgetsList.add(budget);
        }

        // Cập nhật danh sách ngân sách hiển thị
        displayBudgets.setValue(completeBudgetsList);
    }

    private void observeCategorySpentAmounts() {
        // Theo dõi số tiền đã chi theo danh mục từ repository
        LiveData<Map<String, Double>> categorySpentAmounts = repository.getCategorySpentAmounts();

        // Xóa nguồn dữ liệu trước đó nếu có
        if (currentCategorySpentSource != null) {
            displayBudgets.removeSource(currentCategorySpentSource);
        }

        // Thêm nguồn dữ liệu mới
        displayBudgets.addSource(categorySpentAmounts, spentAmounts -> {
            // Cập nhật danh sách ngân sách với số tiền đã chi mới
            List<Budget> currentBudgets = displayBudgets.getValue();
            if (currentBudgets != null) {
                for (Budget budget : currentBudgets) {
                    Double spentAmount = spentAmounts.getOrDefault(budget.getCategory(), 0.0);
                    budget.setSpent(spentAmount);
                }
                displayBudgets.setValue(currentBudgets);
            }
        });

        // Cập nhật tham chiếu nguồn dữ liệu hiện tại
        currentCategorySpentSource = categorySpentAmounts;
    }

    private void observeTotals() {
        // Theo dõi tổng số tiền ngân sách từ repository
        LiveData<Double> repositoryTotalBudget = repository.getTotalBudget();

        // Xóa nguồn dữ liệu trước đó nếu có
        if (currentTotalBudgetSource != null) {
            totalBudget.removeSource(currentTotalBudgetSource);
        }

        // Thêm nguồn dữ liệu mới
        totalBudget.addSource(repositoryTotalBudget, value -> {
            totalBudget.setValue(value);
            updateRemainingAndProgress();
        });

        // Cập nhật tham chiếu nguồn dữ liệu hiện tại
        currentTotalBudgetSource = repositoryTotalBudget;

        // Theo dõi tổng số tiền đã chi từ repository
        LiveData<Double> repositoryTotalSpent = repository.getTotalSpent();

        // Xóa nguồn dữ liệu trước đó nếu có
        if (currentTotalSpentSource != null) {
            totalSpent.removeSource(currentTotalSpentSource);
        }

        // Thêm nguồn dữ liệu mới
        totalSpent.addSource(repositoryTotalSpent, value -> {
            totalSpent.setValue(value);
            updateRemainingAndProgress();
        });

        // Cập nhật tham chiếu nguồn dữ liệu hiện tại
        currentTotalSpentSource = repositoryTotalSpent;
    }

    // Phương thức để lấy một ngân sách cụ thể theo ID
    public LiveData<Budget> getBudgetById(String budgetId) {
        return repository.getBudgetById(budgetId);
    }

    // Phương thức để thêm một ngân sách mới
    public void addBudget(Budget budget) {
        repository.addBudget(budget);
    }

    // Phương thức để cập nhật một ngân sách
    public void updateBudget(Budget budget) {
        repository.updateBudget(budget);
    }

    // Phương thức để xóa một ngân sách
    public void deleteBudget(String budgetId) {
        repository.deleteBudget(budgetId);
    }

    // Phương thức để làm mới dữ liệu
    public void refreshBudgets() {
        // Tải lại ngân sách đang hoạt động
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