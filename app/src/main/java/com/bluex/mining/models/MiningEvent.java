package com.bluex.mining.models;

public class MiningEvent {
    private String id;
    private String name;
    private double multiplier;
    private long startTime;
    private long endTime;

    public MiningEvent() {
        // Required empty constructor for Firebase
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getMultiplier() { return multiplier; }
    public void setMultiplier(double multiplier) { this.multiplier = multiplier; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public boolean isActive() {
        long currentTime = System.currentTimeMillis();
        return currentTime >= startTime && currentTime <= endTime;
    }

    public long getRemainingTime() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }
} 