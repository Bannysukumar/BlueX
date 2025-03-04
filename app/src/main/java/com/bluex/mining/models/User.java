package com.bluex.mining.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    public static final long ONE_HOUR_MILLIS = 3600000; // 1 hour in milliseconds
    public static final double REFERRAL_MINING_BONUS = 0.0002; // Bonus mining rate per referral
    public static final double REFERRAL_COMMISSION = 0.02; // 2% commission on referral earnings

    private String uid;
    private String email;
    private String displayName;
    private double balance;
    private long joinDate;
    private boolean isBlocked;
    private double miningRate;
    private double referralRate;
    private String username;
    private String profilePicUrl;
    private String referralCode;
    private double totalMined;
    private double totalWithdrawn;
    private long lastDailyBonus;
    private boolean isSubscribed;
    private String profileImageUrl;
    private long lastMiningTime;
    private double bonusRate = 0.0; // Additional bonus rate
    private boolean isMining = false;
    private long miningStartTime;
    private long miningEndTime;
    private String walletAddress;
    private int referralCount = 0;
    private double referralBonus = 0.0;
    private String referredBy;
    private boolean isKycVerified = false;
    private LeaderboardStats weeklyStats;
    private LeaderboardStats monthlyStats;
    private int streak;
    private long lastRewardClaim;
    private int miningStreak;
    private int pendingWithdrawals;
    private int unreadMessages;
    private int teamSize;
    private String kycStatus;
    private String phoneNumber;
    private double miningProgress; // 0 to 1.0 (represents progress to 1 BXC)
    private List<String> referrals = new ArrayList<>(); // Store referral UIDs
    private Map<String, Double> referralEarnings = new HashMap<>(); // Track earnings per referral
    private boolean isVerified;
    private double teamBonus = 0.0;
    private List<String> teamMembers = new ArrayList<>();

    public static class LeaderboardStats {
        private double totalMined;
        private long lastReset;
        private int rank;

        public LeaderboardStats() {
            // Required for Firebase
        }

        public LeaderboardStats(long resetTime) {
            this.totalMined = 0.0;
            this.lastReset = resetTime;
            this.rank = 0;
        }

        // Getters and setters
        public double getTotalMined() { return totalMined; }
        public void setTotalMined(double totalMined) { this.totalMined = totalMined; }
        
        public long getLastReset() { return lastReset; }
        public void setLastReset(long lastReset) { this.lastReset = lastReset; }
        
        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
    }

    // Default constructor required for Firebase
    public User() {
        this.balance = 0;
        this.miningRate = 0.00001;
        this.totalMined = 0;
        this.referralCount = 0;
        this.referralBonus = 0;
        this.isKycVerified = false;
        this.kycStatus = "pending";
        this.pendingWithdrawals = 0;
        this.isBlocked = false;
        this.miningStartTime = 0;
        this.isMining = false;
        this.miningProgress = 0;
        this.walletAddress = "";
        this.weeklyStats = new LeaderboardStats(System.currentTimeMillis());
        this.monthlyStats = new LeaderboardStats(System.currentTimeMillis());
        this.lastRewardClaim = 0;
        this.isSubscribed = false;
        this.streak = 0;
        this.referrals = new ArrayList<>();
        this.referralEarnings = new HashMap<>();
    }

    public User(String uid) {
        this();
        this.uid = uid;
        this.balance = 0.0;
        this.miningRate = 0.1; // Default mining rate per second
        this.lastMiningTime = System.currentTimeMillis();
        this.totalMined = 0.0;
        this.totalWithdrawn = 0.0;
        this.referralCode = generateReferralCode(uid);
        this.referredBy = "";
        this.referralCount = 0;
        this.referralBonus = 0.0;
        this.lastDailyBonus = 0;
        this.displayName = "Miner #" + uid.substring(0, 4);
        this.profilePicUrl = "";
        this.weeklyStats = new LeaderboardStats(System.currentTimeMillis());
        this.monthlyStats = new LeaderboardStats(System.currentTimeMillis());
        this.lastRewardClaim = 0;
        this.isSubscribed = false;
        this.streak = 0;
    }

    public User(String uid, String username, String email) {
        this(uid);
        this.username = username;
        this.email = email;
        this.displayName = username;
    }

    private String generateReferralCode(String uid) {
        return uid.substring(0, 6).toUpperCase();
    }

    private String generateReferralCode() {
        return username != null ? 
            username.substring(0, Math.min(username.length(), 5)).toUpperCase() + 
            String.format("%04d", (int)(Math.random() * 10000)) :
            generateReferralCode(uid);
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getProfilePicUrl() { return profilePicUrl; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getTotalMined() { return totalMined; }
    public void setTotalMined(double totalMined) { this.totalMined = totalMined; }

    public double getTotalWithdrawn() { return totalWithdrawn; }
    public void setTotalWithdrawn(double totalWithdrawn) { this.totalWithdrawn = totalWithdrawn; }

    public long getLastDailyBonus() { return lastDailyBonus; }
    public void setLastDailyBonus(long lastDailyBonus) { this.lastDailyBonus = lastDailyBonus; }

    public boolean isSubscribed() { return isSubscribed; }
    public void setSubscribed(boolean subscribed) { isSubscribed = subscribed; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public long getLastMiningTime() { return lastMiningTime; }
    public void setLastMiningTime(long lastMiningTime) { this.lastMiningTime = lastMiningTime; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public double getMiningRate() { return miningRate; }
    public void setMiningRate(double miningRate) { this.miningRate = miningRate; }

    public double getBonusRate() { return bonusRate; }
    public void setBonusRate(double rate) { this.bonusRate = rate; }

    public long getLastRewardClaim() { return lastRewardClaim; }
    public void setLastRewardClaim(long time) { this.lastRewardClaim = time; }

    public String getReferredBy() { return referredBy; }
    public void setReferredBy(String referredBy) { this.referredBy = referredBy; }

    public int getReferralCount() { return referralCount; }
    public void setReferralCount(int referralCount) { this.referralCount = referralCount; }

    public double getReferralBonus() { return referralBonus; }
    public void setReferralBonus(double referralBonus) { this.referralBonus = referralBonus; }

    public LeaderboardStats getWeeklyStats() { return weeklyStats; }
    public void setWeeklyStats(LeaderboardStats stats) { this.weeklyStats = stats; }

    public LeaderboardStats getMonthlyStats() { return monthlyStats; }
    public void setMonthlyStats(LeaderboardStats stats) { this.monthlyStats = stats; }

    public boolean isKycVerified() { return isKycVerified; }
    public void setKycVerified(boolean verified) { this.isKycVerified = verified; }

    public int getMiningStreak() { return miningStreak; }
    public void setMiningStreak(int streak) { this.miningStreak = streak; }
    
    public int getPendingWithdrawals() { return pendingWithdrawals; }
    public void setPendingWithdrawals(int count) { this.pendingWithdrawals = count; }
    
    public int getUnreadMessages() { return unreadMessages; }
    public void setUnreadMessages(int count) { this.unreadMessages = count; }
    
    public int getTeamSize() { return teamSize; }
    public void setTeamSize(int size) { this.teamSize = size; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void addToBalance(double amount) {
        this.balance += amount;
        this.totalMined += amount;
    }

    public boolean withdraw(double amount) {
        if (amount <= balance) {
            balance -= amount;
            totalWithdrawn += amount;
            return true;
        }
        return false;
    }

    public long getMiningStartTime() { return miningStartTime; }
    public void setMiningStartTime(long time) { this.miningStartTime = time; }
    
    public boolean isMining() { return isMining; }
    public void setMining(boolean mining) { this.isMining = mining; }
    
    public double getMiningProgress() { return miningProgress; }
    public void setMiningProgress(double progress) { this.miningProgress = progress; }

    public void startMining() {
        if (!isMining) {
            isMining = true;
            miningStartTime = System.currentTimeMillis();
            // Set mining duration to 24 hours
            miningEndTime = miningStartTime + (24 * ONE_HOUR_MILLIS);
            lastMiningTime = miningStartTime;
        }
    }

    public void stopMining() {
        this.isMining = false;
        this.miningStartTime = 0;
    }

    public double calculateMiningProgress() {
        if (!isMining) return 0;
        
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - miningStartTime;
        return Math.min(1.0, (double) elapsedTime / ONE_HOUR_MILLIS);
    }

    public long getMiningEndTime() { return miningEndTime; }
    public void setMiningEndTime(long time) { this.miningEndTime = time; }

    public boolean isMiningActive() {
        if (!isMining) return false;
        long currentTime = System.currentTimeMillis();
        return currentTime < miningEndTime;
    }

    public String getWalletAddress() {
        return walletAddress != null ? walletAddress : "";
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public boolean hasWalletAddress() {
        return walletAddress != null && !walletAddress.isEmpty();
    }

    public void updateMiningEarnings() {
        if (!isMining) return;

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastMiningTime;
        
        if (elapsedTime >= 1000) { // Update every second
            // Calculate base earnings
            double baseEarnings = (elapsedTime / 1000.0) * miningRate;
            double bonusEarnings = (elapsedTime / 1000.0) * bonusRate;
            
            // Calculate referral bonus from mining (2% per referral)
            double referralMiningBonus = (elapsedTime / 1000.0) * (REFERRAL_MINING_BONUS * referralCount);
            
            double totalEarnings = baseEarnings + bonusEarnings + referralMiningBonus;

            // Update user stats
            balance += totalEarnings;
            totalMined += totalEarnings;
            referralBonus += referralMiningBonus;
            lastMiningTime = currentTime;

            // Check if mining period has ended
            if (currentTime >= miningEndTime) {
                isMining = false;
            }
        }
    }

    public void addReferral(String referralUid) {
        if (!referrals.contains(referralUid)) {
            referrals.add(referralUid);
            referralCount++;
            referralEarnings.put(referralUid, 0.0);
            // Increase mining rate for getting a new referral
            bonusRate += REFERRAL_MINING_BONUS;
        }
    }

    public void updateReferralEarnings(String referralUid, double referralMiningAmount) {
        // Calculate 2% commission from referral's mining
        double commission = referralMiningAmount * REFERRAL_COMMISSION; // 2% of mining amount
        
        // Add commission to balance immediately
        balance += commission;
        referralBonus += commission;
        
        // Track earnings from this referral
        Double currentEarnings = referralEarnings.get(referralUid);
        if (currentEarnings != null) {
            referralEarnings.put(referralUid, currentEarnings + commission);
        }

        // Update total mined amount to include referral commission
        totalMined += commission;
    }

    // Getters and setters for new fields
    public List<String> getReferrals() { return referrals; }
    public void setReferrals(List<String> referrals) { 
        this.referrals = referrals;
        this.referralCount = referrals.size();
    }

    public Map<String, Double> getReferralEarnings() { return referralEarnings; }
    public void setReferralEarnings(Map<String, Double> earnings) { this.referralEarnings = earnings; }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }

    public double getTeamBonus() { return teamBonus; }
    public void setTeamBonus(double bonus) { this.teamBonus = bonus; }

    public List<String> getTeamMembers() { return teamMembers; }
    public void setTeamMembers(List<String> members) { this.teamMembers = members; }

    public long getJoinDate() { return joinDate; }
    public void setJoinDate(long joinDate) { this.joinDate = joinDate; }

    public double getReferralRate() { return referralRate; }
    public void setReferralRate(double referralRate) { this.referralRate = referralRate; }

    // Compatibility methods for existing code using userId
    public String getUserId() { return uid; }
    public void setUserId(String userId) { this.uid = userId; }
} 