package com.bluex.mining.models;

public class Task {
    private String id;
    private String title;
    private String description;
    private double reward;
    private String verificationKey;
    private boolean isCompleted;

    public Task() {
        // Required for Firebase
    }

    public Task(String id, String title, String description, double reward, String verificationKey) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.reward = reward;
        this.verificationKey = verificationKey;
        this.isCompleted = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getReward() { return reward; }
    public void setReward(double reward) { this.reward = reward; }

    public String getVerificationKey() { return verificationKey; }
    public void setVerificationKey(String verificationKey) { this.verificationKey = verificationKey; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
} 