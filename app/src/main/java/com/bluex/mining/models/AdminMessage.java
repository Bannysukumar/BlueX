package com.bluex.mining.models;

public class AdminMessage {
    private String title;
    private String message;
    private long timestamp;
    private boolean isImportant;

    public AdminMessage() {
        // Required for Firebase
    }

    public AdminMessage(String title, String message, boolean isImportant) {
        this.title = title;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.isImportant = isImportant;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isImportant() { return isImportant; }
    public void setImportant(boolean important) { isImportant = important; }
} 