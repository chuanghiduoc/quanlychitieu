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

    public TransactionsViewModel() {
        repository = TransactionRepository.getInstance();
        transactions = repository.getAllTransactions();

        // Set default date range (current month)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        fromDate.setValue(calendar.getTime());

        calendar = Calendar.getInstance();
        toDate.setValue(calendar.getTime());
    }

    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
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
        repository.deleteTransaction(id);
    }

    public void applyFilter(Date fromDate, Date toDate, String category, String type) {
        this.fromDate.setValue(fromDate);
        this.toDate.setValue(toDate);
        this.selectedCategory.setValue(category);
        this.selectedType.setValue(type);

        Log.d("TransactionsViewModel", "Applying filter - Type: " + type + ", Category: " + category);

        // Lấy dữ liệu đã lọc từ repository
        LiveData<List<Transaction>> filteredTransactions = repository.getFilteredTransactions(fromDate, toDate, category, type);

        // Đảm bảo cập nhật LiveData transactions
        if (transactions instanceof MutableLiveData) {
            // Nếu đang sử dụng MutableLiveData, cần cập nhật giá trị
            filteredTransactions.observeForever(new Observer<List<Transaction>>() {
                @Override
                public void onChanged(List<Transaction> transactionList) {
                    ((MutableLiveData<List<Transaction>>) transactions).setValue(transactionList);
                    // Hủy quan sát sau khi cập nhật
                    filteredTransactions.removeObserver(this);
                }
            });
        } else {
            // Nếu không, gán trực tiếp LiveData mới
            transactions = filteredTransactions;
        }
    }


}
