package com.example.quanlychitieu.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.quanlychitieu.R;
import com.example.quanlychitieu.data.model.FinancialGoal;
import com.example.quanlychitieu.databinding.FragmentGoalsBinding;

import java.util.ArrayList;
import java.util.List;

public class GoalsFragment extends Fragment implements GoalAdapter.GoalClickListener {

    private FragmentGoalsBinding binding;
    private GoalsViewModel viewModel;
    private GoalAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGoalsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(GoalsViewModel.class);

        // Thiết lập toolbar và nút back
        binding.toolbar.setNavigationOnClickListener(v -> {
            Navigation.findNavController(requireView()).popBackStack();
        });

        // Thiết lập RecyclerView
        setupRecyclerView();

        // Quan sát danh sách mục tiêu
        observeGoals();

        // Thiết lập nút thêm mục tiêu mới
        binding.fabAddGoal.setOnClickListener(v -> {
            // Chuyển đến màn hình thêm mục tiêu mới
            Navigation.findNavController(requireView())
                    .navigate(R.id.action_goalsFragment_to_addEditGoalFragment);
        });
    }


    private void setupRecyclerView() {
        adapter = new GoalAdapter(new ArrayList<>(), this);
        binding.goalsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.goalsRecyclerView.setAdapter(adapter);
    }

    private void observeGoals() {
        viewModel.getGoals().observe(getViewLifecycleOwner(), goals -> {
            if (goals != null && !goals.isEmpty()) {
                adapter.updateGoals(goals);
                showContent();
            } else {
                showEmptyState();
            }
        });
    }

    private void showContent() {
        binding.goalsRecyclerView.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        binding.goalsRecyclerView.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.VISIBLE);
    }

    @Override
    public void onGoalClick(FinancialGoal goal) {
        // Chuyển đến màn hình chi tiết mục tiêu
        Bundle args = new Bundle();
        args.putString("goal_id", goal.getFirebaseId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_goalsFragment_to_goalDetailsFragment, args);
    }

    @Override
    public void onContributeClick(FinancialGoal goal) {
        // Hiển thị dialog đóng góp vào mục tiêu
        showContributeDialog(goal);
    }

    private void showContributeDialog(FinancialGoal goal) {
        ContributeGoalDialogFragment dialog = ContributeGoalDialogFragment.newInstance(goal.getFirebaseId());
        dialog.show(getChildFragmentManager(), "ContributeDialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
