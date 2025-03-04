package com.bluex.mining;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.widget.ImageView;
import android.util.Log;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.core.view.GravityCompat;

import com.bluex.mining.models.User;
import com.bluex.mining.ads.AdsManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.MutableData;

import com.bluex.mining.dialogs.ReferralDialog;
import com.bluex.mining.utils.AdminManager;
import com.bluex.mining.utils.AdManager;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Random;

import androidx.annotation.NonNull;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.bluex.mining.services.MiningService;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private TextView balanceText;
    private TextView miningRateText;
    private TextView timeText;
    private TextView streakText;
    private Button startMiningButton;
    private Button withdrawBtn;
    private Button followButton;
    private Button serviceButton;
    private Button kycButton;
    private Button questsButton;
    private Button walletButton;
    private Button leaderboardButton;
    private ImageButton languageButton;
    private ImageButton navHome, navTasks, navMessages, navWallet, navProfile;
    private TextView messagesBadge, walletBadge;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private User currentUser;
    private Handler miningHandler;
    private boolean isMining = false;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 1000; // 1 second
    private static final long SAVE_INTERVAL = 10000; // 10 seconds
    private double accumulatedEarnings = 0;
    private AdsManager adsManager;
    private static final int MINING_BOOST_MULTIPLIER = 2;
    private static final long MINING_BOOST_DURATION = 300000; // 5 minutes
    private boolean isMiningBoosted = false;
    private long boostEndTime = 0;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ValueEventListener userListener;
    private TextView usernameText;
    private ImageView verifiedIcon;
    private TextView referralCodeText;
    private TextView teamSizeText;
    private ProgressBar miningProgressBar;
    private TextView miningStatusText;
    private static final long MINING_DURATION = 3600000; // 1 hour in milliseconds
    private static final double MINING_REWARD = 1.0; // 1 BXC token per hour
    private long miningStartTime;
    private long backPressTime;  // Add this field
    private static final long BACK_PRESS_DELAY = 2000; // 2 seconds
    private Button messageButton;
    private ValueEventListener miningListener;
    private TextView referralCountText;
    private TextView referralBonusText;
    private static final double HOURLY_MINING_RATE = 1.0; // 1 BXC per hour
    private static final long ONE_HOUR_MILLIS = 3600000; // 1 hour in milliseconds
    private ProgressBar miningProgress;
    private long lastMiningTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ads
        LinearLayout adContainer = findViewById(R.id.adContainer);
        adManager.showBannerAd(adContainer);

        // Initialize Firebase instances once
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Check login status before proceeding
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize handler on main thread
        miningHandler = new Handler(Looper.getMainLooper());

        // Initialize views and setup listeners
        initializeViews();
        
        // Load user data
        loadUserData();

        // Check for daily bonus
        checkDailyBonus();

        // Initialize drawer layout and navigation view
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Setup navigation drawer
        findViewById(R.id.menuButton).setOnClickListener(v -> drawerLayout.open());
        
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Already on home
                drawerLayout.close();
            } else if (itemId == R.id.nav_transaction_history) {
                startActivity(new Intent(this, TransactionHistoryActivity.class));
            } else if (itemId == R.id.nav_kyc) {
                startActivity(new Intent(this, KYCActivity.class));
            } else if (itemId == R.id.nav_quests) {
                startActivity(new Intent(this, QuestsActivity.class));
            } else if (itemId == R.id.nav_mine) {
                // Already on mining screen
                drawerLayout.close();
            } else if (itemId == R.id.nav_team) {
                startActivity(new Intent(this, TeamActivity.class));
            } else if (itemId == R.id.nav_wallet) {
                startActivity(new Intent(this, WalletActivity.class));
            } else if (itemId == R.id.nav_leaderboard) {
                startActivity(new Intent(this, LeaderboardActivity.class));
            } else if (itemId == R.id.nav_about) {
                startActivity(new Intent(this, AboutActivity.class));
            } else if (itemId == R.id.nav_roadmap) {
                startActivity(new Intent(this, RoadmapActivity.class));
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Setup navigation drawer header
        View headerView = navigationView.getHeaderView(0);
        usernameText = headerView.findViewById(R.id.usernameText);
        verifiedIcon = headerView.findViewById(R.id.verifiedIcon);
        referralCodeText = headerView.findViewById(R.id.referralCodeText);
        teamSizeText = headerView.findViewById(R.id.teamSizeText);

        // Update user info
        updateUserInfo();

        // Start progress updates if already mining
        if (currentUser != null && currentUser.isMining()) {
            startMiningUpdates();
        }

        // Enable "up" navigation for the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // In onCreate or where you set up your message button
        messageButton = findViewById(R.id.messageButton);
        messageButton.setOnClickListener(v -> {
            startActivity(new Intent(this, MessagesActivity.class));
        });

        startMiningButton.setOnClickListener(v -> startMining());

        // Check mining status when app opens
        checkMiningStatus();

        miningStatusText = findViewById(R.id.miningStatusText);
        balanceText = findViewById(R.id.balanceText);
        miningProgress = findViewById(R.id.miningProgressBar);

        // Load last mining time
        mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).child("lastMiningTime")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            lastMiningTime = snapshot.getValue(Long.class);
                            updateMiningStatus();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError("Failed to load mining status");
                    }
                });

        // Start automatic mining check
        miningHandler.post(miningRunnable);
    }

    private void initializeViews() {
        try {
            // Initialize TextViews
            balanceText = findViewById(R.id.balanceText);
            miningRateText = findViewById(R.id.miningRateText);
            timeText = findViewById(R.id.timeText);
            streakText = findViewById(R.id.streakText);
            miningStatusText = findViewById(R.id.miningStatusText);
            referralCountText = findViewById(R.id.referralCountText);
            referralBonusText = findViewById(R.id.referralBonusText);
            referralCodeText = findViewById(R.id.referralCodeText);
            teamSizeText = findViewById(R.id.teamSizeText);

            // Initialize Buttons
            startMiningButton = findViewById(R.id.startMiningButton);
            withdrawBtn = findViewById(R.id.withdrawButton);
            followButton = findViewById(R.id.followButton);
            serviceButton = findViewById(R.id.serviceButton);
            kycButton = findViewById(R.id.kycButton);
            questsButton = findViewById(R.id.questsButton);
            walletButton = findViewById(R.id.walletButton);
            leaderboardButton = findViewById(R.id.leaderboardButton);
            messageButton = findViewById(R.id.messageButton);
            languageButton = findViewById(R.id.languageButton);

            // Initialize Progress Bar
            miningProgressBar = findViewById(R.id.miningProgressBar);

            // Initialize Navigation Buttons
            navHome = findViewById(R.id.navHome);
            navTasks = findViewById(R.id.navTasks);
            navMessages = findViewById(R.id.navMessages);
            navWallet = findViewById(R.id.navWallet);
            navProfile = findViewById(R.id.navProfile);

            // Initialize Badges
            messagesBadge = findViewById(R.id.messagesBadge);
            walletBadge = findViewById(R.id.walletBadge);

            // Set click listeners with null checks
            if (startMiningButton != null) {
                startMiningButton.setOnClickListener(v -> startMining());
            }

            if (withdrawBtn != null) {
                withdrawBtn.setOnClickListener(v -> {
                    if (!isClickValid()) return;
                    startWithdrawActivity();
                });
            }

            if (followButton != null) {
                followButton.setOnClickListener(v -> handleFollowClick());
            }

            if (serviceButton != null) {
                serviceButton.setOnClickListener(v -> handleServiceClick());
            }

            if (kycButton != null) {
                kycButton.setOnClickListener(v -> handleKYCClick());
            }

            if (questsButton != null) {
                questsButton.setOnClickListener(v -> handleQuestsClick());
            }

            if (walletButton != null) {
                walletButton.setOnClickListener(v -> handleWalletClick());
            }

            if (leaderboardButton != null) {
                leaderboardButton.setOnClickListener(v -> startLeaderboardActivity());
            }

            if (languageButton != null) {
                languageButton.setOnClickListener(v -> handleLanguageClick());
            }

            // Set up bottom navigation click listeners with null checks
            if (navHome != null) {
                navHome.setOnClickListener(v -> handleHomeClick());
            }

            if (navTasks != null) {
                navTasks.setOnClickListener(v -> handleTasksClick());
            }

            if (navMessages != null) {
                navMessages.setOnClickListener(v -> handleMessagesClick());
            }

            if (navWallet != null) {
                navWallet.setOnClickListener(v -> handleWalletClick());
            }

            if (navProfile != null) {
                navProfile.setOnClickListener(v -> handleProfileClick());
            }

            // Set initial badge visibility
            if (walletBadge != null) {
                walletBadge.setVisibility(View.GONE);
            }

            if (messagesBadge != null) {
                messagesBadge.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = mDatabase.child("users").child(userId);
        
        // Enable disk persistence
        userRef.keepSynced(true);
        
        userListener = userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    updateUI();
                    checkDailyBonus();
                    if (currentUser.isMining()) {
                        startMiningUpdates();
                    }
                    // Listen for referral earnings
                    listenToReferralEarnings();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadUserData:onCancelled", error.toException());
            }
        });
    }

    private void listenToReferralEarnings() {
        if (currentUser == null || currentUser.getReferrals() == null) return;

        // Listen to each referral's mining activity
        for (String referralUid : currentUser.getReferrals()) {
            mDatabase.child("users").child(referralUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User referral = snapshot.getValue(User.class);
                        if (referral != null && referral.isMining()) {
                            // Calculate referral's current mining rate
                            double referralMiningRate = referral.getMiningRate() + referral.getBonusRate();
                            
                            // Update referral earnings with 2% commission
                            currentUser.updateReferralEarnings(referralUid, referralMiningRate);
                            
                            // Update Firebase in real-time
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("balance", currentUser.getBalance());
                            updates.put("totalMined", currentUser.getTotalMined());
                            updates.put("referralBonus", currentUser.getReferralBonus());
                            updates.put("referralEarnings", currentUser.getReferralEarnings());
                            
                            mDatabase.child("users").child(currentUser.getUserId())
                                .updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Update UI after successful Firebase update
                                    updateUI();
                                });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "referralListener:onCancelled", error.toException());
                    }
                });
        }
    }

    private void checkDailyBonus() {
        if (currentUser == null) return; // Add null check

        long currentTime = System.currentTimeMillis();
        long lastBonus = currentUser.getLastDailyBonus();
        long oneDayInMillis = 24 * 60 * 60 * 1000;

        if (currentTime - lastBonus >= oneDayInMillis) {
            // Show daily bonus dialog
            showDailyBonusDialog();
        }
    }

    private void updateUI() {
        if (currentUser == null) return;

        try {
            // Update balance and mining rate
            balanceText.setText(String.format("%.8f BXC", currentUser.getBalance()));
            miningRateText.setText(String.format("%.8f BXC/hr", currentUser.getMiningRate()));
            
            // Update verification status if icon exists
            if (verifiedIcon != null) {
                verifiedIcon.setVisibility(currentUser.isVerified() ? View.VISIBLE : View.GONE);
            }

            // Update mining status
            if (startMiningButton != null) {
                startMiningButton.setText(currentUser.isMining() ? "Stop Mining" : "Start Mining");
            }

            // Update mining status text if exists
            if (miningStatusText != null) {
                miningStatusText.setText(currentUser.isMining() ? "Mining in progress..." : "Mining stopped");
            }

            // Update other UI elements as needed
            if (referralCountText != null) {
                referralCountText.setText(String.format("Referrals: %d", currentUser.getReferralCount()));
            }

            if (referralBonusText != null) {
                referralBonusText.setText(String.format("Bonus: %.8f BXC", currentUser.getReferralBonus()));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
            showError("Failed to update UI");
        }
    }

    private void startMining() {
        long currentTime = System.currentTimeMillis();
        
        // Update last mining time
        mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).child("lastMiningTime")
                .setValue(currentTime);

        // Add mining reward
        mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            double newBalance = user.getBalance() + HOURLY_MINING_RATE;
                            mDatabase.child("users").child(mAuth.getCurrentUser().getUid()).child("balance")
                                    .setValue(newBalance)
                                    .addOnSuccessListener(aVoid -> {
                                        showToast("Successfully mined 1 BXC!");
                                        balanceText.setText(String.format("%.2f BXC", newBalance));
                                    })
                                    .addOnFailureListener(e -> showError("Failed to update balance"));
                        }
                    }
                })
                .addOnFailureListener(e -> showError("Failed to fetch user data"));
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void logout() {
        stopMining();
        mAuth.signOut();
        // Also sign out from Google
        GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
                .signOut()
                .addOnCompleteListener(task -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }

    private void updateMiningRate() {
        if (currentUser != null) {
            double baseRate = currentUser.getMiningRate();
            double totalMined = currentUser.getTotalMined();
            double bonusRate = Math.min(totalMined / 1000, 0.5);
            
            // Add referral bonus
            double referralBonus = currentUser.getReferralBonus();
            
            double effectiveRate = baseRate + bonusRate + referralBonus;
            if (isMiningBoosted && System.currentTimeMillis() < boostEndTime) {
                effectiveRate *= MINING_BOOST_MULTIPLIER;
            }
            
            currentUser.setMiningRate(effectiveRate);
            miningRateText.setText(String.format("Rate: %.2f BXC/sec", effectiveRate));
        }
    }

    private void showReferralDialog() {
        if (currentUser != null) {
            ReferralDialog dialog = new ReferralDialog(this, currentUser);
            dialog.show();
        }
    }

    private void showDailyBonusDialog() {
        // Implement daily bonus dialog
        Toast.makeText(this, "Daily bonus dialog coming soon", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miningHandler != null) {
            miningHandler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null && mDatabase != null) {
            mDatabase.removeEventListener(userListener);
        }
        if (miningHandler != null) {
            miningHandler.removeCallbacksAndMessages(null);
        }
        if (adsManager != null) {
            adsManager.destroy();
        }
        if (miningListener != null) {
            mDatabase.removeEventListener(miningListener);
        }
        stopMining();
        if (miningHandler != null) {
            miningHandler.removeCallbacks(miningRunnable);
        }
    }

    private void handleFollowClick() {
        // Array of social media links
        String[] socialLinks = {
            "https://t.me/Bluexcryptolive",
            "https://x.com/Bluexcryptolive?t=lhSJDEoErvRSB0tDuv-tnQ&s=09",
            "https://discord.gg/bX9D93UC"
        };

        try {
            // Generate random index
            int randomIndex = new Random().nextInt(socialLinks.length);
            
            // Create intent to open the random link
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(socialLinks[randomIndex]));
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // If no app can handle the link, open in browser
                intent.setData(Uri.parse(socialLinks[randomIndex]));
                startActivity(Intent.createChooser(intent, "Open with"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening social link", e);
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleServiceClick() {
        // Implement local service functionality
        Toast.makeText(this, "Local service feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void handleKYCClick() {
        startActivity(new Intent(this, KYCActivity.class));
    }

    private void handleQuestsClick() {
        startActivity(new Intent(this, QuestsActivity.class));
    }

    private void handleWalletClick() {
        startActivity(new Intent(this, WalletActivity.class));
    }

    private void handleLanguageClick() {
        // Implement language selection
        Toast.makeText(this, "Language selection coming soon", Toast.LENGTH_SHORT).show();
    }

    private void startLeaderboardActivity() {
        startActivity(new Intent(this, LeaderboardActivity.class));
    }

    private void handleHomeClick() {
        // Already on home screen
        navHome.setColorFilter(getResources().getColor(R.color.green));
        navTasks.setColorFilter(getResources().getColor(R.color.gray));
        navMessages.setColorFilter(getResources().getColor(R.color.gray));
        navWallet.setColorFilter(getResources().getColor(R.color.gray));
        navProfile.setColorFilter(getResources().getColor(R.color.gray));
    }

    private void handleTasksClick() {
        startActivity(new Intent(this, TasksActivity.class));
    }

    private void handleMessagesClick() {
        startActivity(new Intent(this, MessagesActivity.class));
    }

    private void handleProfileClick() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void updateBadges() {
        try {
            if (walletBadge != null) {
                int pendingWithdrawals = currentUser.getPendingWithdrawals();
                walletBadge.setVisibility(pendingWithdrawals > 0 ? View.VISIBLE : View.GONE);
                walletBadge.setText(String.valueOf(pendingWithdrawals));
            }

            if (messagesBadge != null) {
                int unreadMessages = currentUser.getUnreadMessages();
                messagesBadge.setVisibility(unreadMessages > 0 ? View.VISIBLE : View.GONE);
                messagesBadge.setText(String.valueOf(unreadMessages));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating badges", e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        
        // Show admin menu if user is admin
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            menu.findItem(R.id.action_admin).setVisible(AdminManager.isAdmin(user.getEmail()));
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_admin) {
            startActivity(new Intent(this, AdminMainActivity.class));
            return true;
        } else if (itemId == R.id.action_logout) {
            logout();
            return true;
        } else if (itemId == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                onBackPressed();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateUserInfo() {
        if (currentUser == null || navigationView == null) {
            return; // Guard against null values
        }

        try {
            // Update username
            if (usernameText != null) {
                String displayName = currentUser.getDisplayName();
                usernameText.setText(displayName != null ? displayName : "User");
            }
            
            // Update verification status
            if (verifiedIcon != null) {
                verifiedIcon.setVisibility(currentUser.isKycVerified() ? View.VISIBLE : View.GONE);
            }
            
            // Update KYC button state
            if (kycButton != null) {
                kycButton.setEnabled(!currentUser.isKycVerified());
                kycButton.setText(currentUser.isKycVerified() ? "Verified" : "KYC");
            }
            
            // Update referral info
            if (referralCodeText != null) {
                String referralCode = currentUser.getReferralCode();
                referralCodeText.setText("Code: " + (referralCode != null ? referralCode : ""));
            }
            
            if (teamSizeText != null) {
                teamSizeText.setText("Team: " + currentUser.getTeamSize());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating user info", e);
        }
    }

    private void setupNavigation() {
        // Bottom Navigation
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navTasks = findViewById(R.id.navTasks);
        ImageButton navMessages = findViewById(R.id.navMessages);
        ImageButton navWallet = findViewById(R.id.navWallet);
        ImageButton navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            // Already on home, do nothing
        });

        navTasks.setOnClickListener(v -> {
            startActivity(new Intent(this, QuestsActivity.class));
        });

        navMessages.setOnClickListener(v -> {
            startActivity(new Intent(this, MessagesActivity.class));
        });

        navWallet.setOnClickListener(v -> {
            startActivity(new Intent(this, WalletActivity.class));
        });

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, UserProfileActivity.class));
        });

        // Button clicks
        Button walletButton = findViewById(R.id.walletButton);
        walletButton.setOnClickListener(v -> {
            startActivity(new Intent(this, WalletActivity.class));
        });

        Button leaderboardButton = findViewById(R.id.leaderboardButton);
        leaderboardButton.setOnClickListener(v -> {
            startActivity(new Intent(this, LeaderboardActivity.class));
        });
    }

    // Add this runnable to update the timer and progress
    private final Runnable miningRunnable = new Runnable() {
        @Override
        public void run() {
            updateMiningStatus();
            miningHandler.postDelayed(this, 1000); // Update every second
        }
    };

    // Add this to check mining status when activity starts
    private void checkMiningStatus() {
        String userId = mAuth.getCurrentUser().getUid();
        miningListener = mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean mining = snapshot.child("isMining").getValue(Boolean.class);
                isMining = mining != null && mining;
                if (isMining) {
                    startMiningUpdates();
                }
                updateMiningButton();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showError("Failed to check mining status");
            }
        });
    }

    private void updateMiningButton() {
        startMiningButton.setText(isMining ? "Stop Mining" : "Start Mining");
    }

    @Override
    public void onBackPressed() {
        // If drawer is open, close it
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        // If this is the main screen, show "Press back again to exit" message
        if (System.currentTimeMillis() - backPressTime < BACK_PRESS_DELAY) {
            super.onBackPressed();  // Exit app
            return;
        } else {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
        
        backPressTime = System.currentTimeMillis();
    }

    private String formatTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void updateMiningStatus() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMining = currentTime - lastMiningTime;
        
        if (timeSinceLastMining >= ONE_HOUR_MILLIS) {
            // Ready to mine
            startMiningButton.setVisibility(View.VISIBLE);
            miningStatusText.setText("Ready to mine!");
            miningProgress.setVisibility(View.GONE);
            isMining = false;
        } else {
            // Still cooling down
            startMiningButton.setVisibility(View.GONE);
            miningProgress.setVisibility(View.VISIBLE);
            isMining = true;
            
            // Calculate and show remaining time
            long remainingTime = ONE_HOUR_MILLIS - timeSinceLastMining;
            String timeLeft = formatTime(remainingTime);
            miningStatusText.setText("Mining cooldown: " + timeLeft);
            
            // Update progress bar
            int progress = (int) ((timeSinceLastMining * 100) / ONE_HOUR_MILLIS);
            miningProgress.setProgress(progress);
        }
    }

    private void startWithdrawActivity() {
        startActivity(new Intent(this, WithdrawActivity.class));
    }

    private void startMiningUpdates() {
        if (miningHandler == null) {
            miningHandler = new Handler(Looper.getMainLooper());
        }
        miningHandler.post(miningRunnable);
        
        // Update mining status in database
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId).child("isMining").setValue(true)
            .addOnFailureListener(e -> showError("Failed to start mining"));
    }

    private void stopMining() {
        if (miningHandler != null) {
            miningHandler.removeCallbacks(miningRunnable);
        }
        isMining = false;
        
        // Update mining status in database
        String userId = mAuth.getCurrentUser().getUid();
        if (userId != null) {
            mDatabase.child("users").child(userId).child("isMining").setValue(false)
                .addOnFailureListener(e -> showError("Failed to stop mining"));
        }
        
        // Update UI
        if (startMiningButton != null) {
            startMiningButton.setText("Start Mining");
        }
        if (miningStatusText != null) {
            miningStatusText.setText("Mining stopped");
        }
    }

    private void showRewardedAd() {
        adManager.showRewardedAd(this, amount -> {
            // Handle reward
            if (currentUser != null) {
                double currentBalance = currentUser.getBalance();
                currentUser.setBalance(currentBalance + amount);
                updateUserData();
            }
        });
    }

    // Show interstitial ad periodically
    private void showInterstitialIfNeeded() {
        if (Math.random() < 0.3) { // 30% chance to show ad
            adManager.showInterstitialAd(this);
        }
    }

    private void updateUserData() {
        if (currentUser != null && mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            mDatabase.child("users").child(userId).setValue(currentUser)
                .addOnSuccessListener(aVoid -> {
                    updateUI();
                })
                .addOnFailureListener(e -> {
                    showError("Failed to update user data");
                });
        }
    }
}