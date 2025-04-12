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
import androidx.appcompat.app.ActionBar;
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
import android.app.AlertDialog;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.drawable.TransitionDrawable;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.facebook.shimmer.ShimmerFrameLayout;
import android.content.SharedPreferences;
import com.google.android.material.button.MaterialButton;
import android.widget.EditText;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "BlueXPrefs";
    private static final String BALANCE_KEY = "user_balance";
    private static final String LAST_MINING_TIME_KEY = "last_mining_time";
    private TextView balanceText;
    private TextView miningRateLabel;
    private TextView timeText;
    private TextView streakText;
    private Button startMiningButton;
    private ImageButton languageButton;
    private ImageButton navHome, navTasks, navWallet, navProfile, navMining, navMessages;
    private TextView messagesBadge;
    private TextView countdownText;
    
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
    private ImageView miningIcon;
    private MiningService miningService;
    private static final long DOUBLE_BACK_PRESS_INTERVAL = 2000; // 2 seconds
    private long backPressedTime = 0;
    private Toast backToast;
    private TextView totalMinedValue;
    private TextView miningTimeValue;
    private TextView teamSizeValue;
    private TextView rankValue;
    private MaterialButton inviteButton;
    private TextView miningRateValue;
    private TextView referralBonusValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize shimmer effect for mining status
        ShimmerFrameLayout shimmerLayout = findViewById(R.id.miningStatusShimmer);
        shimmerLayout.startShimmer();

        // Initialize mining button pulse animation
        Button startMiningButton = findViewById(R.id.startMiningButton);
        Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse);
        startMiningButton.startAnimation(pulseAnim);

        // Initialize countdown blink animation
        TextView countdownText = findViewById(R.id.countdownText);
        Animation blinkAnim = AnimationUtils.loadAnimation(this, R.anim.blink);
        countdownText.startAnimation(blinkAnim);

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
        
        // Setup navigation
        setupNavigation();
        
        // Load user data
        loadUserData();

        // Check for daily bonus
        checkDailyBonus();

        // Initialize drawer layout and navigation view
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
        setSupportActionBar(toolbar);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayShowTitleEnabled(false);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

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
            } else if (itemId == R.id.nav_fund_transfer) {
                Intent intent = new Intent(this, TransferActivity.class);
                startActivity(intent);
            } else if (itemId == R.id.nav_privacy_policy) {
                // Open privacy policy URL
                String url = "https://sites.google.com/view/bluex1232/home";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Unable to open privacy policy", Toast.LENGTH_SHORT).show();
                }
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

        inviteButton = findViewById(R.id.inviteButton);
        inviteButton.setOnClickListener(v -> shareApp());
    }

    private void initializeViews() {
        try {
            // Initialize Toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            // Initialize DrawerLayout and NavigationView
            drawerLayout = findViewById(R.id.drawerLayout);
            navigationView = findViewById(R.id.navigationView);

            // Initialize Mining Icon
            miningIcon = findViewById(R.id.miningIcon);
            miningIcon.setImageResource(R.drawable.mining_icon);

            // Initialize Mining Controls
            startMiningButton = findViewById(R.id.startMiningButton);
            miningProgressBar = findViewById(R.id.miningProgressBar);
            miningStatusText = findViewById(R.id.miningStatusText);
            miningProgress = findViewById(R.id.miningProgressBar);
            countdownText = findViewById(R.id.countdownText);

            // Initialize Balance and Rate TextViews
            balanceText = findViewById(R.id.balanceText);
            miningRateLabel = findViewById(R.id.miningRateLabel);

            // Initialize Time and Streak TextViews
            timeText = findViewById(R.id.timeText);
            streakText = findViewById(R.id.streakText);

            // Initialize Statistics Views
            totalMinedValue = findViewById(R.id.totalMinedValue);
            miningTimeValue = findViewById(R.id.miningTimeValue);
            teamSizeValue = findViewById(R.id.teamSizeValue);
            rankValue = findViewById(R.id.rankValue);
            miningRateValue = findViewById(R.id.miningRateValue);
            referralBonusValue = findViewById(R.id.referralBonusValue);

            // Initialize Mining Service
            miningService = new MiningService();

            // Initialize Navigation Buttons
            navHome = findViewById(R.id.navHome);
            navTasks = findViewById(R.id.navTasks);
            navMessages = findViewById(R.id.navMessages);
            navWallet = findViewById(R.id.navWallet);
            navProfile = findViewById(R.id.navProfile);

            // Initialize Follow Button
            Button followButton = findViewById(R.id.followButton);
            if (followButton != null) {
                followButton.setOnClickListener(v -> openTelegram());
            }

            // Initialize Fund Transfer Button
            Button fundTransferButton = findViewById(R.id.fundTransferButton);
            if (fundTransferButton != null) {
                fundTransferButton.setOnClickListener(v -> startActivity(new Intent(this, TransferActivity.class)));
            }

            // Initialize Tasks Button
            Button tasksButton = findViewById(R.id.tasksButton);
            if (tasksButton != null) {
                tasksButton.setOnClickListener(v -> startActivity(new Intent(this, TasksActivity.class)));
            }

            // Initialize Leaderboard Button
            Button leaderboardButton = findViewById(R.id.leaderboardButton);
            if (leaderboardButton != null) {
                leaderboardButton.setOnClickListener(v -> startActivity(new Intent(this, LeaderboardActivity.class)));
            }

            // Initialize Team Button
            Button teamButton = findViewById(R.id.teamButton);
            if (teamButton != null) {
                teamButton.setOnClickListener(v -> startActivity(new Intent(this, TeamActivity.class)));
            }

            // Initialize Wallet Button
            Button walletButton = findViewById(R.id.walletButton);
            if (walletButton != null) {
                walletButton.setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));
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

            // Set initial colors
            handleHomeClick();

            // Set mining button click listener
            if (startMiningButton != null) {
                startMiningButton.setOnClickListener(v -> startMining());
            }

            // Initialize message button
            messageButton = findViewById(R.id.messageButton);
            if (messageButton != null) {
                messageButton.setOnClickListener(v -> {
                    startActivity(new Intent(this, MessagesActivity.class));
                });
            }

            // Initialize statistics views
            totalMinedValue = findViewById(R.id.totalMinedValue);
            miningTimeValue = findViewById(R.id.miningTimeValue);
            teamSizeValue = findViewById(R.id.teamSizeValue);
            rankValue = findViewById(R.id.rankValue);

            // Initialize quick action buttons
            inviteButton = findViewById(R.id.inviteButton);

            // Set click listeners
            inviteButton.setOnClickListener(v -> shareApp());

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
            miningRateLabel.setText(String.format("Mining Rate: %.8f/sec", currentUser.getMiningRate()));
            
            // Update statistics
            updateStatistics();

            // Update mining status
            if (startMiningButton != null) {
                startMiningButton.setText(currentUser.isMining() ? "Stop Mining" : "Start Mining");
            }

            // Update mining status text if exists
            if (miningStatusText != null) {
                miningStatusText.setText(currentUser.isMining() ? "Mining in progress..." : "Mining stopped");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI", e);
            showError("Failed to update UI");
        }
    }

    private void startMining() {
        if (isMining) {
            Toast.makeText(this, "Mining is already in progress", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if enough time has passed since last mining
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMining = currentTime - lastMiningTime;
        
        if (timeSinceLastMining < ONE_HOUR_MILLIS) {
            long remainingTime = ONE_HOUR_MILLIS - timeSinceLastMining;
            String timeLeft = formatTime(remainingTime);
            Toast.makeText(this, "Please wait " + timeLeft + " before mining again", Toast.LENGTH_LONG).show();
            return;
        }

        // Request necessary permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.FOREGROUND_SERVICE,
                    android.Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE
            }, 1);
        }

        // Start mining
        isMining = true;
        lastMiningTime = currentTime;
        
        // Update UI
        startMiningButton.setVisibility(View.GONE);
        miningProgress.setVisibility(View.VISIBLE);
        miningIcon.setImageResource(R.drawable.mining_icon_active);
        miningStatusText.setText("Mining in progress...");
        
        // Start mining service
        Intent serviceIntent = new Intent(this, MiningService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        // Start progress animation
        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(ONE_HOUR_MILLIS);
        animator.addUpdateListener(animation -> {
            int progress = (int) animation.getAnimatedValue();
            miningProgress.setProgress(progress);
        });
        animator.start();
        
        // Schedule status update
        miningHandler.postDelayed(() -> {
            updateMiningStatus();
        // Add mining reward
            addMiningReward();
            // Re-enable start mining button
            startMiningButton.setVisibility(View.VISIBLE);
            isMining = false;
            
            // Update mining status in database
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                if (userId != null) {
                    mDatabase.child("users").child(userId).child("isMining").setValue(false);
                }
            }
        }, ONE_HOUR_MILLIS);
        
        // Update mining status in database
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            if (userId != null) {
                mDatabase.child("users").child(userId).child("isMining").setValue(true);
                mDatabase.child("users").child(userId).child("lastMiningTime").setValue(currentTime);
            }
        }
    }

    private void addMiningReward() {
        if (currentUser == null) return;

        // Calculate mining reward based on mining rate and duration
        double miningReward = HOURLY_MINING_RATE; // 1 BXC per hour
        
        // Add reward to user's balance
        double newBalance = currentUser.getBalance() + miningReward;
        currentUser.setBalance(newBalance);
        
        // Update total mined
        double newTotalMined = currentUser.getTotalMined() + miningReward;
        currentUser.setTotalMined(newTotalMined);
        
        // Update Firebase using transaction to ensure atomicity
        String userId = mAuth.getCurrentUser().getUid();
        if (userId != null) {
            mDatabase.child("users").child(userId).runTransaction(new Transaction.Handler() {
                @NonNull
                @Override
                public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                    User user = mutableData.getValue(User.class);
                        if (user != null) {
                        user.setBalance(user.getBalance() + miningReward);
                        user.setTotalMined(user.getTotalMined() + miningReward);
                        user.setLastMiningTime(System.currentTimeMillis());
                        user.setMining(false);
                        mutableData.setValue(user);
                    }
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                    if (committed) {
                        // Update UI
                        updateUI();
                        // Show reward notification
                        Toast.makeText(MainActivity.this, 
                            "Mining complete! +" + miningReward + " BXC", 
                            Toast.LENGTH_LONG).show();
                    } else {
                        showError("Failed to update mining reward");
                    }
                }
            });
        }
    }

    private double getCurrentBalance() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getFloat(BALANCE_KEY, 0.0f);
    }

    private long getLastMiningTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getLong(LAST_MINING_TIME_KEY, 0);
    }

    private void updateBalanceText() {
        if (balanceText != null) {
            double balance = currentUser.getBalance();
            balanceText.setText(String.format("Balance: %.4f BTC", balance));
        }
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
            miningRateLabel.setText(String.format("Mining Rate: %.2f/sec", effectiveRate));
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
        if (miningListener != null && mDatabase != null) {
            mDatabase.removeEventListener(miningListener);
        }
        
        // Only call stopMining if we're not in the process of being destroyed
        if (!isFinishing() && !isChangingConfigurations()) {
        stopMining();
        }
        
        if (miningHandler != null) {
            miningHandler.removeCallbacks(miningRunnable);
        }
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
        navHome.setColorFilter(getResources().getColor(R.color.attractive_blue));
        navTasks.setColorFilter(getResources().getColor(R.color.gray));
        navMessages.setColorFilter(getResources().getColor(R.color.gray));
        navWallet.setColorFilter(getResources().getColor(R.color.gray));
        navProfile.setColorFilter(getResources().getColor(R.color.gray));
    }

    private void handleTasksClick() {
        startActivity(new Intent(this, TasksActivity.class));
        navHome.setColorFilter(getResources().getColor(R.color.gray));
        navTasks.setColorFilter(getResources().getColor(R.color.attractive_blue));
        navMessages.setColorFilter(getResources().getColor(R.color.gray));
        navWallet.setColorFilter(getResources().getColor(R.color.gray));
        navProfile.setColorFilter(getResources().getColor(R.color.gray));
    }

    private void handleMessagesClick() {
        startActivity(new Intent(this, MessagesActivity.class));
        
        // Add null checks for all navigation buttons
        if (navHome != null) {
            navHome.setColorFilter(getResources().getColor(R.color.gray));
        }
        
        if (navTasks != null) {
            navTasks.setColorFilter(getResources().getColor(R.color.gray));
        }
        
        if (navMessages != null) {
            navMessages.setColorFilter(getResources().getColor(R.color.attractive_blue));
        }
        
        if (navWallet != null) {
            navWallet.setColorFilter(getResources().getColor(R.color.gray));
        }
        
        if (navProfile != null) {
            navProfile.setColorFilter(getResources().getColor(R.color.gray));
        }
    }

    private void handleProfileClick() {
        startActivity(new Intent(this, UserProfileActivity.class));
        navHome.setColorFilter(getResources().getColor(R.color.gray));
        navTasks.setColorFilter(getResources().getColor(R.color.gray));
        navMessages.setColorFilter(getResources().getColor(R.color.gray));
        navWallet.setColorFilter(getResources().getColor(R.color.gray));
        navProfile.setColorFilter(getResources().getColor(R.color.attractive_blue));
    }

    private void updateBadges() {
        try {
            // Removed messagesBadge update since we no longer have that view
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
            // if (kycButton != null) {
            //     kycButton.setEnabled(!currentUser.isKycVerified());
            //     kycButton.setText(currentUser.isKycVerified() ? "Verified" : "KYC");
            // }
            
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
        ImageButton navWallet = findViewById(R.id.navWallet);
        ImageButton navProfile = findViewById(R.id.navProfile);

        navHome.setOnClickListener(v -> {
            // Already on home, do nothing
        });

        navTasks.setOnClickListener(v -> {
            startActivity(new Intent(this, QuestsActivity.class));
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

        // Button leaderboardButton = findViewById(R.id.leaderboardButton);
        // leaderboardButton.setOnClickListener(v -> {
        //     startActivity(new Intent(this, LeaderboardActivity.class));
        // });
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        
        String userId = currentUser.getUid();
        miningListener = mDatabase.child("users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean mining = snapshot.child("isMining").getValue(Boolean.class);
                isMining = mining != null && mining;
                
                // Get last mining time
                Long lastMiningTimeValue = snapshot.child("lastMiningTime").getValue(Long.class);
                if (lastMiningTimeValue != null) {
                    lastMiningTime = lastMiningTimeValue;
                }
                
                if (isMining) {
                    startMiningUpdates();
                    // Update UI to show mining in progress
                    startMiningButton.setVisibility(View.GONE);
                    miningProgress.setVisibility(View.VISIBLE);
                    miningIcon.setImageResource(R.drawable.mining_icon_active);
                    miningStatusText.setText("Mining in progress...");
                } else {
                    // Check if mining is completed
                    long currentTime = System.currentTimeMillis();
                    long timeSinceLastMining = currentTime - lastMiningTime;
                    
                    if (timeSinceLastMining < ONE_HOUR_MILLIS) {
                        // Mining is still in progress
                        startMiningUpdates();
                        startMiningButton.setVisibility(View.GONE);
                        miningProgress.setVisibility(View.VISIBLE);
                        miningIcon.setImageResource(R.drawable.mining_icon_active);
                        miningStatusText.setText("Mining in progress...");
                    } else {
                        // Mining is completed
                        startMiningButton.setVisibility(View.VISIBLE);
                        miningProgress.setVisibility(View.GONE);
                        miningIcon.setImageResource(R.drawable.mining_icon);
                        miningStatusText.setText("Mining completed");
                    }
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
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (backPressedTime + DOUBLE_BACK_PRESS_INTERVAL > System.currentTimeMillis()) {
            if (backToast != null) {
                backToast.cancel();
            }
            super.onBackPressed();
            return;
        } else {
            backToast = Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT);
            backToast.show();
        }
        
        backPressedTime = System.currentTimeMillis();
    }

    private String formatTime(long millis) {
        long hours = millis / 3600000;
        long minutes = (millis % 3600000) / 60000;
        long seconds = (millis % 60000) / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void updateMiningStatus() {
        if (currentUser != null) {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastMining = currentTime - lastMiningTime;
        
            if (timeSinceLastMining < ONE_HOUR_MILLIS) {
                // Mining in progress
                isMining = true;
            startMiningButton.setVisibility(View.GONE);
            miningProgress.setVisibility(View.VISIBLE);
                miningIcon.setImageResource(R.drawable.mining_icon_active);
                miningStatusText.setText("Mining in progress...");
                
                // Update progress
                int progress = (int) ((timeSinceLastMining * 100) / ONE_HOUR_MILLIS);
                miningProgress.setProgress(progress);
                
                // Calculate remaining time
            long remainingTime = ONE_HOUR_MILLIS - timeSinceLastMining;
            String timeLeft = formatTime(remainingTime);
                countdownText.setText("Time remaining: " + timeLeft);
            } else {
                // Mining completed
                isMining = false;
                startMiningButton.setVisibility(View.VISIBLE);
                miningProgress.setVisibility(View.GONE);
                miningIcon.setImageResource(R.drawable.mining_icon);
                miningStatusText.setText("Mining completed");
                countdownText.setText("");
                
                // Add mining reward
                addMiningReward();
                
                // Update mining status in database
                FirebaseUser currentUser = mAuth.getCurrentUser();
                if (currentUser != null) {
                    String userId = currentUser.getUid();
                    if (userId != null) {
                        mDatabase.child("users").child(userId).child("isMining").setValue(false);
                    }
                }
            }
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
        if (userId != null) {
            mDatabase.child("users").child(userId).child("isMining").setValue(false)
                .addOnFailureListener(e -> showError("Failed to stop mining"));
            }
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

    public void switchTheme(boolean isDark) {
        if (isDark) {
            setTheme(R.style.DarkTheme);
        } else {
            setTheme(R.style.LightTheme);
        }
        recreate(); // Recreate the activity to apply the new theme
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /**
     * Opens Telegram channel
     */
    private void openTelegram() {
        String telegramUrl = "https://t.me/Bluexcryptolive";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(telegramUrl));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            Toast.makeText(this, "Opening Telegram", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Unable to open Telegram", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareApp() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = "Join me on BlueX Mining! Use my referral code: " + getReferralCode();
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showWithdrawDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Withdraw BXC")
            .setMessage("Enter amount to withdraw")
            .setView(R.layout.dialog_withdraw)
            .setPositiveButton("Withdraw", (dialog, which) -> {
                // Handle withdrawal
                EditText amountInput = ((AlertDialog) dialog).findViewById(R.id.withdrawAmount);
                if (amountInput != null) {
                    String amount = amountInput.getText().toString();
                    processWithdrawal(amount);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void processWithdrawal(String amount) {
        try {
            double withdrawAmount = Double.parseDouble(amount);
            if (withdrawAmount <= 0) {
                showToast("Please enter a valid amount");
                return;
            }
            // TODO: Implement actual withdrawal logic
            showToast("Withdrawal request submitted");
        } catch (NumberFormatException e) {
            showToast("Please enter a valid number");
        }
    }

    private String getReferralCode() {
        // TODO: Implement actual referral code logic
        return "USER123";
    }

    private void updateStatistics() {
        if (currentUser == null) return;
        
        // Update statistics with actual values from currentUser
        totalMinedValue.setText(String.format("%.2f BXC", currentUser.getTotalMined()));
        miningTimeValue.setText(formatMiningTime(currentUser.getTotalMiningTime()));
        teamSizeValue.setText(String.valueOf(currentUser.getTeamSize()));
        rankValue.setText("#" + getGlobalRank());
    }

    private String formatMiningTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    private int getGlobalRank() {
        if (currentUser == null || currentUser.getWeeklyStats() == null) return 0;
        return currentUser.getWeeklyStats().getRank();
    }
}