package com.example.quanlychitieu.ui.transactions;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.model.Transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class TransactionsViewModel extends ViewModel {

    private final List<Transaction> allTransactions = new ArrayList<>();
    private final MutableLiveData<List<Transaction>> transactions = new MutableLiveData<>();

    private Date fromDate;
    private Date toDate;
    private String category;
    private String transactionType = "Tất cả giao dịch";

    public TransactionsViewModel() {
        // Khởi tạo với dữ liệu mẫu
        loadTransactions();
        clearFilter(); // Áp dụng bộ lọc mặc định
    }

    private void loadTransactions() {
        allTransactions.clear();
        // Thêm một số giao dịch mẫu
        allTransactions.add(new Transaction(1, "Tiền ăn trưa", -50000, "Ăn uống", new Date(), false, "Chi phí ăn trưa"));
        allTransactions.add(new Transaction(2, "Xăng xe", -100000, "Di chuyển", new Date(), false, "Chi phí đi lại"));
        allTransactions.add(new Transaction(3, "Mua quần áo", -500000, "Mua sắm", new Date(), false, "Chi phí mua sắm"));
        allTransactions.add(new Transaction(4, "Lương tháng 3", 5000000, "Lương", new Date(), false, "Thu nhập từ lương"));

        // Áp dụng bộ lọc
        applyFilterInternal();
    }

    public LiveData<List<Transaction>> getTransactions() {
        return transactions;
    }

    public void applyFilter(Date fromDate, Date toDate, String category, String transactionType) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.category = category;
        this.transactionType = transactionType;

        applyFilterInternal();
    }

    private void applyFilterInternal() {
        List<Transaction> filteredList = allTransactions;

        // Áp dụng bộ lọc ngày
        if (fromDate != null && toDate != null) {
            filteredList = filteredList.stream()
                    .filter(transaction -> !transaction.getDate().before(fromDate)
                            && !transaction.getDate().after(toDate))
                    .collect(Collectors.toList());
        }

        // Áp dụng bộ lọc loại giao dịch
        if (!"Tất cả giao dịch".equals(transactionType)) {
            boolean isExpense = "Chi tiêu".equals(transactionType);
            filteredList = filteredList.stream()
                    .filter(transaction -> isExpense ? transaction.getAmount() < 0 : transaction.getAmount() > 0)
                    .collect(Collectors.toList());
        }

        // Áp dụng bộ lọc danh mục
        if (category != null && !category.equals("Tất cả danh mục")) {
            filteredList = filteredList.stream()
                    .filter(transaction -> transaction.getCategory().equals(category))
                    .collect(Collectors.toList());
        }

        transactions.setValue(filteredList);
    }

    public void clearFilter() {
        Date now = new Date();
        // Reset bộ lọc về mặc định
        this.fromDate = new Date(now.getYear(), now.getMonth(), 1); // Ngày đầu tháng hiện tại
        this.toDate = now;
        this.category = "Tất cả danh mục";
        this.transactionType = "Tất cả giao dịch"; // Đặt lại loại giao dịch
        applyFilterInternal();
    }

    public void addTransaction(Transaction transaction) {
        allTransactions.add(transaction);
        applyFilterInternal();
    }
}
