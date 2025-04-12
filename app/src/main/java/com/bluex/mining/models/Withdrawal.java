package com.bluex.mining.models;

public class Withdrawal {
    private String id;
    private String userId;
    private double amount;
    private String status;
    private long timestamp;
    private String walletAddress;
    private String mobileNumber;

    public Withdrawal() {
        // Default constructor required for Firebase
    }

    public Withdrawal(String userId, double amount, String status, long timestamp, String walletAddress, String mobileNumber) {
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.timestamp = timestamp;
        this.walletAddress = walletAddress;
        this.mobileNumber = mobileNumber;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getWalletAddress() { return walletAddress; }
    public void setWalletAddress(String walletAddress) { this.walletAddress = walletAddress; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
}