package com.example.quanlychitieu.ui.transactions;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.model.Transaction;
import com.example.quanlychitieu.data.repository.TransactionRepository;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TransactionsViewModel extends ViewModel {
    private final TransactionRepository repository;
    private final MediatorLiveData<List<Transaction>> transactions = new MediatorLiveData<>();
    private final MutableLiveData<Date> fromDate = new MutableLiveData<>();
    private final MutableLiveData<Date> toDate = new MutableLiveData<>();
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>("Tất cả danh mục");
    private final MutableLiveData<String> selectedType = new MutableLiveData<>("Tất cả giao dịch");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);

    public interface FilterCallback {
        void onFilterComplete(List<Transaction> transactions);
    }

    public TransactionsViewModel() {
        repository = TransactionRepository.getInstance();

        // Đặt phạm vi ngày mặc định (tháng hiện tại)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        fromDate.setValue(calendar.getTime());

        calendar = Calendar.getInstance();
        toDate.setValue(calendar.getTime());

        // Tải các giao dịch ban đầu với phạm vi ngày mặc định
        applyFilter(fromDate.getValue(), toDate.getValue(),
                "Tất cả danh mục", "Tất cả giao dịch", null);
    }

    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Transaction> getTransactionById(String id) {
        return repository.getTransactionById(id);
    }

    public void deleteTransaction(String id) {
        isLoading.setValue(true);

        repository.deleteTransaction(id).addOnCompleteListener(task -> {
            // Sau khi xóa xong, làm mới các giao dịch với các bộ lọc hiện tại
            refreshTransactions();
        });
    }


    public void applyFilter(Date fromDate, Date toDate, String category, String type, FilterCallback callback) {
        // Cập nhật các giá trị bộ lọc đã lưu trữ
        this.fromDate.setValue(fromDate);
        this.toDate.setValue(toDate);
        this.selectedCategory.setValue(category);
        this.selectedType.setValue(type);

        // Chuẩn bị phạm vi ngày với các thành phần thời gian thích hợp
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(fromDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        Date startDate = startCal.getTime();

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(toDate);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        Date endDate = endCal.getTime();

        isLoading.setValue(true);
        LiveData<List<Transaction>> source = repository.getFilteredTransactions(
                startDate, endDate, category, type);

        transactions.addSource(source, transactionList -> {
            transactions.setValue(transactionList);
            isLoading.setValue(false);

            if (callback != null) {
                callback.onFilterComplete(transactionList);
            }

            transactions.removeSource(source);
        });
    }

    public void refreshTransactions() {
        isLoading.setValue(true);

        // Sử dụng các giá trị bộ lọc hiện tại
        Date startDate = fromDate.getValue();
        Date endDate = toDate.getValue();
        String category = selectedCategory.getValue();
        String type = selectedType.getValue();

        // Áp dụng các thành phần thời gian thích hợp
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        Date adjustedStartDate = startCal.getTime();

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(endDate);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        Date adjustedEndDate = endCal.getTime();

        LiveData<List<Transaction>> source = repository.getFilteredTransactions(
                adjustedStartDate, adjustedEndDate, category, type);

        transactions.addSource(source, transactionList -> {
            transactions.setValue(transactionList);
            isLoading.setValue(false);
            transactions.removeSource(source);
        });
    }

    // Các getter cho các giá trị bộ lọc để sử dụng trong UI
    public Date getFromDate() {
        return fromDate.getValue();
    }

    public Date getToDate() {
        return toDate.getValue();
    }

    public String getSelectedCategory() {
        return selectedCategory.getValue();
    }

    public String getSelectedType() {
        return selectedType.getValue();
    }
}