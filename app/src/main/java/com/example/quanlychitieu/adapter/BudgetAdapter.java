package com.example.quanlychitieu.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.example.quanlychitieu.adapter.viewholder.BudgetViewHolder;
import com.example.quanlychitieu.data.model.Budget;
import com.example.quanlychitieu.databinding.ItemBudgetBinding;

public class BudgetAdapter extends ListAdapter<Budget, BudgetViewHolder> {

    private final BudgetClickListener clickListener;

    public BudgetAdapter(BudgetClickListener clickListener) {
        super(new BudgetDiffCallback());
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemBudgetBinding binding = ItemBudgetBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new BudgetViewHolder(binding, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    // DiffUtil callback for efficient updates
    private static class BudgetDiffCallback extends DiffUtil.ItemCallback<Budget> {
        @Override
        public boolean areItemsTheSame(@NonNull Budget oldItem, @NonNull Budget newItem) {
            // Nếu cả hai đều có firebaseId, so sánh theo firebaseId
            if (oldItem.getFirebaseId() != null && newItem.getFirebaseId() != null) {
                return oldItem.getFirebaseId().equals(newItem.getFirebaseId());
            }
            // Nếu không có firebaseId, so sánh theo category
            return oldItem.getCategory().equals(newItem.getCategory());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Budget oldItem, @NonNull Budget newItem) {
            return oldItem.getCategory().equals(newItem.getCategory()) &&
                    oldItem.getAmount() == newItem.getAmount() &&
                    oldItem.getSpent() == newItem.getSpent();
        }
    }

    // Interface for click events
    public interface BudgetClickListener {
        void onBudgetClick(Budget budget);
    }
}
