package com.example.quanlychitieu.ui.transactions;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.data.repository.TransactionRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TransactionsViewModel extends ViewModel {
    private final TransactionRepository repository;
    private LiveData<List<Transaction>> transactions;
    private final MutableLiveData<Date> fromDate = new MutableLiveData<>();
    private final MutableLiveData<Date> toDate = new MutableLiveData<>();
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("Tất cả danh mục");
    private final MutableLiveData<String> selectedType = new MutableLiveData<>("Tất cả giao dịch");
    public interface FilterCallback {
        void onFilterComplete(List<Transaction> transactions);
    }
    // Add loading state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true); // Start with loading=true

    public TransactionsViewModel() {
        repository = TransactionRepository.getInstance();

        // Set default date range (current month)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        fromDate.setValue(calendar.getTime());

        calendar = Calendar.getInstance();
        toDate.setValue(calendar.getTime());

        // Initialize with loading state
        loadInitialData();
    }

    private void loadInitialData() {
        isLoading.setValue(true);

        // Get all transactions initially
        transactions = repository.getAllTransactions();

        // Observe transactions to update loading state
        transactions.observeForever(new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactionList) {
                isLoading.setValue(false);
                // Remove observer to avoid memory leaks
                transactions.removeObserver(this);
            }
        });
    }

    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
    }

    // Add getter for loading state
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Transaction> getTransactionById(String id) {
        return repository.getTransactionById(id);
    }

    public void addTransaction(Transaction transaction) {
        repository.addTransaction(transaction);
    }

    public void updateTransaction(Transaction transaction) {
        repository.updateTransaction(transaction);
    }

    public void deleteTransaction(String id) {
        isLoading.setValue(true);
        repository.deleteTransaction(id);
        // The loading state will be updated when the transactions are reloaded
    }

    public void applyFilter(Date fromDate, Date toDate, String category, String type, FilterCallback callback) {
        this.fromDate.setValue(fromDate);
        this.toDate.setValue(toDate);
        this.selectedCategory.setValue(category);
        this.selectedType.setValue(type);



        // Set loading state to true
        isLoading.setValue(true);

        // Lấy dữ liệu đã lọc từ repository
        LiveData<List<Transaction>> filteredTransactions = repository.getFilteredTransactions(fromDate, toDate, category, type);

        // Đảm bảo cập nhật LiveData transactions
        filteredTransactions.observeForever(new Observer<List<Transaction>>() {
            @Override
            public void onChanged(List<Transaction> transactionList) {

                // Always update the LiveData, even with empty list
                if (transactions instanceof MutableLiveData) {
                    ((MutableLiveData<List<Transaction>>) transactions).setValue(transactionList);
                } else {
                    // If not MutableLiveData, create a new one
                    MutableLiveData<List<Transaction>> newTransactions = new MutableLiveData<>();
                    newTransactions.setValue(transactionList);
                    transactions = newTransactions;
                }

                // Set loading state to false
                isLoading.setValue(false);

                // Call the callback with the results
                if (callback != null) {
                    callback.onFilterComplete(transactionList);
                }

                // Hủy quan sát sau khi cập nhật
                filteredTransactions.removeObserver(this);
            }
        });
    }


}
