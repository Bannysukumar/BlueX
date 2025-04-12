package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bluex.mining.models.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfileActivity extends BaseActivity {
    private DatabaseReference mDatabase;
    private User currentUser;
    private TextView displayNameText;
    private TextView referralCountText;
    private TextView referralBonusText;
    private TextView phoneNumberInput;
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Profile");
            }
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        loadUserData(userId);
        setupButtons();
    }

    private void initializeViews() {
        displayNameText = findViewById(R.id.displayNameText);
        referralCountText = findViewById(R.id.referralCountText);
        referralBonusText = findViewById(R.id.referralBonusText);
        phoneNumberInput = findViewById(R.id.phoneNumberInput);
        profileImage = findViewById(R.id.profileImage);
    }

    private void loadUserData(String userId) {
        mDatabase.child("users").child(userId).get()
            .addOnSuccessListener(snapshot -> {
                currentUser = snapshot.getValue(User.class);
                if (currentUser != null) {
                    updateUI();
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        if (currentUser != null) {
            displayNameText.setText(currentUser.getDisplayName());
            referralCountText.setText("Referrals: " + currentUser.getReferralCount());
            referralBonusText.setText("Referral Bonus: " + currentUser.getReferralBonus());
            
            // Set phone number if it exists
            if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
                phoneNumberInput.setText(currentUser.getPhoneNumber());
            }
        }
    }

    private void setupButtons() {
        findViewById(R.id.editProfileButton).setOnClickListener(v -> showEditProfileDialog());
        findViewById(R.id.shareReferralButton).setOnClickListener(v -> shareReferralCode());
        findViewById(R.id.updateProfileButton).setOnClickListener(v -> updateProfile());
    }

    private void showEditProfileDialog() {
        // Similar to the edit dialog in AdminActivity but for the current user
    }

    private void shareReferralCode() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
            "Join me on BlueX Mining! Use my referral code: " + currentUser.getReferralCode());
        startActivity(Intent.createChooser(shareIntent, "Share Referral Code"));
    }

    private void updateProfile() {
        String phoneNumber = phoneNumberInput.getText().toString().trim();

        if (TextUtils.isEmpty(phoneNumber)) {
            Toast.makeText(this, "Mobile number is required to proceed with withdrawals or transfers.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update user profile with the new phone number
        currentUser.setPhoneNumber(phoneNumber);
        mDatabase.child("users").child(currentUser.getUid()).setValue(currentUser)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to update profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
} 