package com.bluex.mining.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bluex.mining.R;
import com.bluex.mining.models.User;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ReferralDialog extends Dialog {
    private static final double REFERRAL_BONUS_RATE = 0.05; // +0.05/sec per referral
    private User currentUser;
    private DatabaseReference mDatabase;
    
    private TextView referralCodeText;
    private TextView referralStatsText;
    private TextInputEditText referralInput;
    private Button shareButton;
    private Button submitButton;

    public ReferralDialog(@NonNull Context context, User currentUser) {
        super(context);
        this.currentUser = currentUser;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_referral);
        setTitle("Referral System");

        // Initialize views
        referralCodeText = findViewById(R.id.referralCodeText);
        referralStatsText = findViewById(R.id.referralStatsText);
        referralInput = findViewById(R.id.referralInput);
        shareButton = findViewById(R.id.shareButton);
        
        // Set referral code
        referralCodeText.setText(currentUser.getReferralCode());
        
        // Update stats
        updateReferralStats();

        // Share button
        shareButton.setOnClickListener(v -> shareReferralCode());

        // Submit button
        findViewById(android.R.id.button1).setOnClickListener(v -> {
            String code = referralInput.getText().toString().trim();
            if (!code.isEmpty()) {
                submitReferralCode(code);
            }
        });
    }

    private void updateReferralStats() {
        String stats = String.format("Referrals: %d\nBonus Rate: +%.2f/sec",
                currentUser.getReferralCount(),
                currentUser.getReferralCount() * REFERRAL_BONUS_RATE);
        referralStatsText.setText(stats);
    }

    private void shareReferralCode() {
        String shareText = String.format("Use my referral code %s to get a bonus in BlueX Mining App!",
                currentUser.getReferralCode());
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        getContext().startActivity(Intent.createChooser(shareIntent, "Share Referral Code"));
    }

    private void submitReferralCode(String code) {
        if (code.equals(currentUser.getReferralCode())) {
            Toast.makeText(getContext(), "Cannot use your own referral code", 
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!currentUser.getReferredBy().isEmpty()) {
            Toast.makeText(getContext(), "You have already used a referral code", 
                    Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("users")
                .orderByChild("referralCode")
                .equalTo(code)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    if (dataSnapshot.exists()) {
                        // Get referrer user
                        DataSnapshot referrerSnapshot = dataSnapshot.getChildren().iterator().next();
                        User referrer = referrerSnapshot.getValue(User.class);
                        
                        // Update referrer
                        referrer.setReferralCount(referrer.getReferralCount() + 1);
                        referrer.setReferralBonus(referrer.getReferralCount() * REFERRAL_BONUS_RATE);
                        mDatabase.child("users")
                                .child(referrer.getUserId())
                                .setValue(referrer);

                        // Update current user
                        currentUser.setReferredBy(referrer.getUserId());
                        mDatabase.child("users")
                                .child(currentUser.getUserId())
                                .child("referredBy")
                                .setValue(referrer.getUserId());

                        Toast.makeText(getContext(), "Referral code applied successfully!", 
                                Toast.LENGTH_SHORT).show();
                        dismiss();
                    } else {
                        Toast.makeText(getContext(), "Invalid referral code", 
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
} 