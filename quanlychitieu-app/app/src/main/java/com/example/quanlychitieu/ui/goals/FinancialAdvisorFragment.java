package com.example.quanlychitieu.ui.goals;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.quanlychitieu.databinding.FragmentFinancialAdvisorBinding;

import io.noties.markwon.Markwon;

public class FinancialAdvisorFragment extends Fragment {

    private FragmentFinancialAdvisorBinding binding;
    private FinancialAdvisorViewModel viewModel;
    private ChatMessageAdapter adapter;
    private Markwon markwon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFinancialAdvisorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Markwon for Markdown rendering
        markwon = Markwon.create(requireContext());

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(FinancialAdvisorViewModel.class);

        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener(v ->
                Navigation.findNavController(requireView()).popBackStack());

        // Setup RecyclerView
        setupRecyclerView();

        // Setup send button
        binding.sendButton.setOnClickListener(v -> sendMessage());

        // Setup edit text action
        binding.messageInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        // Observe chat messages
        observeChatMessages();

        // Observe loading state
        observeLoadingState();
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(markwon);
        binding.chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.chatRecyclerView.setAdapter(adapter);
    }

    private void observeChatMessages() {
        viewModel.getChatMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.updateMessages(messages);
            
            if (messages.size() > 0) {
                // Hide welcome container if we have messages
                binding.welcomeContainer.setVisibility(View.GONE);
                
                // Scroll to the latest message
                binding.chatRecyclerView.scrollToPosition(messages.size() - 1);
            } else {
                // Show welcome container if no messages
                binding.welcomeContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    private void observeLoadingState() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.typingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.sendButton.setEnabled(!isLoading);
            binding.messageInput.setEnabled(!isLoading);
        });
    }

    private void sendMessage() {
        String message = binding.messageInput.getText().toString().trim();
        if (!message.isEmpty()) {
            viewModel.sendMessage(message);
            binding.messageInput.setText("");
        } else {
            Toast.makeText(requireContext(), "Vui lòng nhập câu hỏi", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
} 