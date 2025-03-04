package com.bluex.mining.models;

public class Quest {
    private String id;
    private String title;
    private String description;
    private double reward;
    private String type; // "weekly" or "basic"
    private String platform; // "twitter", "youtube", etc.
    private boolean isCompleted;
    private long expiryTime; // for weekly quests

    public Quest() {
        // Required for Firebase
    }

    public Quest(String title, String description, double reward, String type, String platform) {
        this.title = title;
        this.description = description;
        this.reward = reward;
        this.type = type;
        this.platform = platform;
        this.isCompleted = false;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getReward() { return reward; }
    public void setReward(double reward) { this.reward = reward; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public long getExpiryTime() { return expiryTime; }
    public void setExpiryTime(long expiryTime) { this.expiryTime = expiryTime; }
} 