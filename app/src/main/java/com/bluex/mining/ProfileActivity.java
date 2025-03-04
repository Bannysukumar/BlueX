package com.bluex.mining;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bluex.mining.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.bumptech.glide.Glide;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private TextView usernameText;
    private TextView emailText;
    private TextView totalMinedText;
    private TextView miningRateText;
    private TextView teamSizeText;
    private TextView referralCodeText;
    private ImageView profileImage;
    private Button signOutButton;
    private Button changeEmailButton;
    private Button purchaseSubscriptionButton;
    private Spinner languageSpinner;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private ValueEventListener userListener;
    private ProgressBar progressBar;
    private View contentLayout;
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Verify user is logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initializeViews();
        setupLanguageSpinner();
        loadUserData();
        setupClickListeners();

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeViews() {
        usernameText = findViewById(R.id.usernameText);
        emailText = findViewById(R.id.userEmailText);
        totalMinedText = findViewById(R.id.totalMinedText);
        miningRateText = findViewById(R.id.miningRateText);
        teamSizeText = findViewById(R.id.teamSizeText);
        referralCodeText = findViewById(R.id.referralCodeText);
        profileImage = findViewById(R.id.profileImage);
        signOutButton = findViewById(R.id.signOutButton);
        changeEmailButton = findViewById(R.id.changeEmailButton);
        purchaseSubscriptionButton = findViewById(R.id.purchaseSubscriptionButton);
        languageSpinner = findViewById(R.id.languageSpinner);
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"EN", "ES", "FR"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
    }

    private void loadUserData() {
        showLoading(true);
        String userId = mAuth.getCurrentUser().getUid();

        userListener = mDatabase.child("users").child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        showLoading(false);
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            validateAndUpdateUI(user);
                        } else {
                            showError("Failed to load user data");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showLoading(false);
                        Log.e(TAG, "loadUserData:onCancelled", error.toException());
                        showError("Failed to load profile: " + error.getMessage());
                    }
                });
    }

    private void validateAndUpdateUI(User user) {
        try {
            // Validate required fields
            if (user.getUserId() == null || user.getUserId().isEmpty()) {
                throw new IllegalStateException("User ID is missing");
            }

            // Validate and set username/display name
            String displayName = user.getDisplayName();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "User #" + user.getUserId().substring(0, 4);
            }
            usernameText.setText(displayName);

            // Validate and set email
            String email = user.getEmail();
            emailText.setText(email != null ? email : "No email provided");

            // Validate and format mining stats
            double totalMined = Math.max(0, user.getTotalMined()); // Ensure non-negative
            totalMinedText.setText(String.format(Locale.US, "%.5f $bxc", totalMined));

            double miningRate = Math.max(0, user.getMiningRate()); // Ensure non-negative
            miningRateText.setText(String.format(Locale.US, "%.5f $bxc/hour", miningRate * 3600));

            // Validate and set team stats
            int teamSize = Math.max(0, user.getTeamSize()); // Ensure non-negative
            teamSizeText.setText(String.valueOf(teamSize));

            String referralCode = user.getReferralCode();
            if (referralCode == null || referralCode.isEmpty()) {
                referralCode = generateReferralCode(user.getUserId());
                // Update missing referral code
                mDatabase.child("users").child(user.getUserId())
                        .child("referralCode").setValue(referralCode);
            }
            referralCodeText.setText(referralCode);

            // Load profile image
            String imageUrl = user.getProfileImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_profile);
            }

            // Update UI based on verification status
            boolean isVerified = user.isKycVerified();
            purchaseSubscriptionButton.setEnabled(!isVerified);
            purchaseSubscriptionButton.setText(isVerified ? "Verified" : "Complete KYC");

            // Show content after successful validation
            showContent();

        } catch (Exception e) {
            Log.e(TAG, "Error validating user data", e);
            showError("Invalid user data: " + e.getMessage());
        }
    }

    private String generateReferralCode(String userId) {
        return userId.substring(0, 6).toUpperCase();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showContent() {
        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void setupClickListeners() {
        signOutButton.setOnClickListener(v -> logout());
        
        changeEmailButton.setOnClickListener(v -> {
            // Implement email change functionality
            Toast.makeText(this, "Email change coming soon", Toast.LENGTH_SHORT).show();
        });

        purchaseSubscriptionButton.setOnClickListener(v -> {
            // Implement subscription purchase
            Toast.makeText(this, "Subscription feature coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.personalInfoButton).setOnClickListener(v -> {
            // Navigate to personal info screen
            Toast.makeText(this, "Personal info coming soon", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.accountSettingsButton).setOnClickListener(v -> {
            // Navigate to account settings
            Toast.makeText(this, "Account settings coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null && mDatabase != null) {
            mDatabase.child("users").child(mAuth.getCurrentUser().getUid())
                    .removeEventListener(userListener);
        }
    }
} 