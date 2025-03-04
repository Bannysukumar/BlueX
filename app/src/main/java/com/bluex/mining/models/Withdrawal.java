package com.bluex.mining.models;

public class Withdrawal {
    private String id;
    private String userId;
    private double amount;
    private String type;
    private long timestamp;
    private String status;
    private String rejectionReason;

    public Withdrawal() {
        // Required for Firebase
    }

    public Withdrawal(String userId, double amount, String type) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.status = "pending";
    }

    public Withdrawal(String userId, double amount) {
        this(userId, amount, "Withdrawal"); // Default type for withdrawals
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type != null ? type : "Basic Mining"; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String reason) { this.rejectionReason = reason; }
} 