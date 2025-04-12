package com.example.quanlychitieu.data.model;

public class FinancialAdviceRequest {
    private String type;
    private Content content;

    public FinancialAdviceRequest(String userId, String message) {
        this.type = "financial-advice";
        this.content = new Content(userId, message);
    }


    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
    }

    public static class Content {
        private String userId;
        private String message;

        public Content(String userId, String message) {
            this.userId = userId;
            this.message = message;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
} 