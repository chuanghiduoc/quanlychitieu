package com.example.quanlychitieu.ui.goals;

import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.quanlychitieu.R;

import java.util.ArrayList;
import java.util.List;

import io.noties.markwon.Markwon;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {
    private List<FinancialAdvisorViewModel.ChatMessage> messages = new ArrayList<>();
    private final Markwon markwon;

    public ChatMessageAdapter(Markwon markwon) {
        this.markwon = markwon;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new MessageViewHolder(view, markwon);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        FinancialAdvisorViewModel.ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void updateMessages(List<FinancialAdvisorViewModel.ChatMessage> newMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(this.messages, newMessages));
        this.messages = new ArrayList<>(newMessages);
        diffResult.dispatchUpdatesTo(this);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final CardView userMessageContainer;
        private final TextView userMessageText;
        private final CardView aiMessageContainer;
        private final TextView aiMessageText;
        private final Markwon markwon;

        public MessageViewHolder(@NonNull View itemView, Markwon markwon) {
            super(itemView);
            this.markwon = markwon;
            userMessageContainer = itemView.findViewById(R.id.user_message_container);
            userMessageText = itemView.findViewById(R.id.user_message_text);
            aiMessageContainer = itemView.findViewById(R.id.ai_message_container);
            aiMessageText = itemView.findViewById(R.id.ai_message_text);
            
            // Enable link clicking in markdown text
            aiMessageText.setMovementMethod(LinkMovementMethod.getInstance());
        }

        public void bind(FinancialAdvisorViewModel.ChatMessage message) {
            if (message.isUser()) {
                // User message
                userMessageContainer.setVisibility(View.VISIBLE);
                aiMessageContainer.setVisibility(View.GONE);
                userMessageText.setText(message.getMessage());
            } else {
                // AI message
                userMessageContainer.setVisibility(View.GONE);
                aiMessageContainer.setVisibility(View.VISIBLE);
                
                // Render markdown for AI messages
                markwon.setMarkdown(aiMessageText, message.getMessage());
            }
        }
    }

    static class MessageDiffCallback extends DiffUtil.Callback {
        private final List<FinancialAdvisorViewModel.ChatMessage> oldMessages;
        private final List<FinancialAdvisorViewModel.ChatMessage> newMessages;

        public MessageDiffCallback(List<FinancialAdvisorViewModel.ChatMessage> oldMessages, 
                                  List<FinancialAdvisorViewModel.ChatMessage> newMessages) {
            this.oldMessages = oldMessages;
            this.newMessages = newMessages;
        }

        @Override
        public int getOldListSize() {
            return oldMessages.size();
        }

        @Override
        public int getNewListSize() {
            return newMessages.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Check if it's the same message instance based on timestamp
            return oldMessages.get(oldItemPosition).getTimestamp() == 
                   newMessages.get(newItemPosition).getTimestamp();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Check if message content is the same
            FinancialAdvisorViewModel.ChatMessage oldMessage = oldMessages.get(oldItemPosition);
            FinancialAdvisorViewModel.ChatMessage newMessage = newMessages.get(newItemPosition);
            return oldMessage.getMessage().equals(newMessage.getMessage()) &&
                   oldMessage.isUser() == newMessage.isUser();
        }
    }
} 