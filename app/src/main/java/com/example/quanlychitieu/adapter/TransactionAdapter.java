package com.example.quanlychitieu.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.adapter.viewholder.TransactionViewHolder;
import com.example.quanlychitieu.data.model.Transaction;

public class TransactionAdapter extends ListAdapter<Transaction, TransactionViewHolder> {
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(Transaction transaction);
        void onEditClick(Transaction transaction);
        void onDeleteClick(Transaction transaction);
    }

    public TransactionAdapter() {
        super(new DiffUtil.ItemCallback<Transaction>() {
            @Override
            public boolean areItemsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Transaction oldItem, @NonNull Transaction newItem) {
                return oldItem.getDescription().equals(newItem.getDescription()) &&
                        oldItem.getAmount() == newItem.getAmount() &&
                        oldItem.getCategory().equals(newItem.getCategory()) &&
                        oldItem.getDate().equals(newItem.getDate());
            }
        });
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = getItem(position);
        holder.bind(transaction);
    }
}
