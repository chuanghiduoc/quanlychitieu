package com.example.quanlychitieu.ui.chatbot;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.quanlychitieu.data.repository.FinancialAdviceRepository;

import java.util.ArrayList;
import java.util.List;

public class FinancialAdvisorViewModel extends ViewModel {
    private final FinancialAdviceRepository repository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<List<ChatMessage>> chatMessages = new MutableLiveData<>(new ArrayList<>());

    public FinancialAdvisorViewModel() {
        repository = FinancialAdviceRepository.getInstance();
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<List<ChatMessage>> getChatMessages() {
        return chatMessages;
    }

    public void sendMessage(String message) {
        if (message.trim().isEmpty()) {
            return;
        }

        // Add user message to chat
        List<ChatMessage> currentMessages = chatMessages.getValue();
        if (currentMessages == null) {
            currentMessages = new ArrayList<>();
        }
        
        ChatMessage userMessage = new ChatMessage(message, true);
        currentMessages.add(userMessage);
        chatMessages.setValue(currentMessages);

        // Show loading
        isLoading.setValue(true);

        // Send to API
        repository.getFinancialAdvice(message).observeForever(response -> {
            isLoading.setValue(false);
            
            // Add AI response to chat
            List<ChatMessage> updatedMessages = chatMessages.getValue();
            if (updatedMessages == null) {
                updatedMessages = new ArrayList<>();
            }
            
            String responseText = response != null ? response.getResponse() : 
                    "Không thể kết nối đến dịch vụ tư vấn tài chính. Vui lòng thử lại sau.";
            
            ChatMessage aiMessage = new ChatMessage(responseText, false);
            updatedMessages.add(aiMessage);
            chatMessages.setValue(updatedMessages);
        });
    }

    // Model class for chat messages
    public static class ChatMessage {
        private final String message;
        private final boolean isUser;
        private final long timestamp;

        public ChatMessage(String message, boolean isUser) {
            this.message = message;
            this.isUser = isUser;
            this.timestamp = System.currentTimeMillis();
        }

        public String getMessage() {
            return message;
        }

        public boolean isUser() {
            return isUser;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }
} 