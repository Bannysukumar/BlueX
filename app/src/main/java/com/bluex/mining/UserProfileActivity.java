package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bluex.mining.models.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class UserProfileActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private User currentUser;
    private TextView displayNameText, emailText, balanceText, miningRateText;
    private TextView totalMinedText, referralCodeText, referralCountText, referralBonusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        initializeViews();
        loadUserData(userId);
        setupButtons();
    }

    private void initializeViews() {
        displayNameText = findViewById(R.id.displayNameText);
        emailText = findViewById(R.id.emailText);
        balanceText = findViewById(R.id.balanceText);
        miningRateText = findViewById(R.id.miningRateText);
        totalMinedText = findViewById(R.id.totalMinedText);
        referralCodeText = findViewById(R.id.referralCodeText);
        referralCountText = findViewById(R.id.referralCountText);
        referralBonusText = findViewById(R.id.referralBonusText);
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
        displayNameText.setText(currentUser.getDisplayName());
        emailText.setText(currentUser.getEmail());
        balanceText.setText("Balance: " + currentUser.getBalance());
        miningRateText.setText("Mining Rate: " + currentUser.getMiningRate() + "/sec");
        totalMinedText.setText("Total Mined: " + currentUser.getTotalMined());
        referralCodeText.setText("Your Referral Code: " + currentUser.getReferralCode());
        referralCountText.setText("Referrals: " + currentUser.getReferralCount());
        referralBonusText.setText("Referral Bonus: " + currentUser.getReferralBonus());
    }

    private void setupButtons() {
        findViewById(R.id.editProfileButton).setOnClickListener(v -> showEditProfileDialog());
        findViewById(R.id.shareReferralButton).setOnClickListener(v -> shareReferralCode());
        findViewById(R.id.withdrawButton).setOnClickListener(v -> showWithdrawDialog());
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

    private void showWithdrawDialog() {
        // Show withdrawal dialog
        // This should be implemented based on your withdrawal process
    }
} 