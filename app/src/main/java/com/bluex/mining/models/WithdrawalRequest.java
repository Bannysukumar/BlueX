package com.bluex.mining.models;

public class WithdrawalRequest {
    private String userId;
    private double amount;
    private String mobileNumber;
    private String status; // e.g., "Pending", "Completed"

    public WithdrawalRequest() {
        // Default constructor required for calls to DataSnapshot.getValue(WithdrawalRequest.class)
    }

    public WithdrawalRequest(String userId, double amount, String mobileNumber, String status) {
        this.userId = userId;
        this.amount = amount;
        this.mobileNumber = mobileNumber;
        this.status = status;
    }

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
} 