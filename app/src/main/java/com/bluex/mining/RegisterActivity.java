package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.bluex.mining.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;
import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {
    private EditText firstNameInput, mobileInput, emailInput, referralInput;
    private EditText passwordInput, confirmPasswordInput;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final double REFERRAL_BONUS = 0.001; // Bonus amount for referrer
    private static final String MINING_CONFIG_PATH = "mining_config";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        firstNameInput = findViewById(R.id.firstNameInput);
        mobileInput = findViewById(R.id.mobileInput);
        emailInput = findViewById(R.id.emailInput);
        referralInput = findViewById(R.id.referralInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
    }

    private void setupClickListeners() {
        findViewById(R.id.registerButton).setOnClickListener(v -> handleRegistration());
        findViewById(R.id.loginLink).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleRegistration() {
        String firstName = firstNameInput.getText().toString().trim();
        String mobile = mobileInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String referralCode = referralInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        // Basic validation
        if (!validateInputs(firstName, mobile, email, password, confirmPassword)) {
            return;
        }

        // If referral code is provided, validate it first
        if (!TextUtils.isEmpty(referralCode)) {
            validateReferralCode(referralCode, () -> {
                // Referral code is valid, proceed with registration
                registerUser(email, password, referralCode);
            });
        } else {
            // No referral code, proceed directly
            registerUser(email, password, null);
        }
    }

    private boolean validateInputs(String firstName, String mobile, String email, 
                                 String password, String confirmPassword) {
        if (TextUtils.isEmpty(firstName)) {
            firstNameInput.setError("First name is required");
            return false;
        }

        if (TextUtils.isEmpty(mobile)) {
            mobileInput.setError("Mobile number is required");
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            return false;
        }

        return true;
    }

    private void validateReferralCode(String referralCode, Runnable onSuccess) {
        mDatabase.child("users")
            .orderByChild("referralCode")
            .equalTo(referralCode)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    onSuccess.run();
                } else {
                    referralInput.setError("Invalid referral code");
                    Toast.makeText(this, "Invalid referral code", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void registerUser(String email, String password, String referralCode) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Registration successful
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    String userId = firebaseUser.getUid();

                    // Create a new user object
                    User newUser = new User();
                    newUser.setUid(userId);
                    newUser.setEmail(email);
                    newUser.setBalance(0.0); // Initial balance
                    newUser.setReferralCode(referralCode); // Set the referral code

                    // Save user to the database
                    mDatabase.child("users").child(userId).setValue(newUser)
                        .addOnCompleteListener(saveTask -> {
                            if (saveTask.isSuccessful()) {
                                // Check if referral code is provided
                                if (referralCode != null && !referralCode.isEmpty()) {
                                    // Update the referrer's balance
                                    mDatabase.child("users").orderByChild("referralCode").equalTo(referralCode).get().addOnCompleteListener(referralTask -> {
                                        if (referralTask.isSuccessful() && referralTask.getResult().exists()) {
                                            String referrerId = referralTask.getResult().getChildren().iterator().next().getKey();
                                            mDatabase.child("users").child(referrerId).child("balance").get().addOnCompleteListener(referrerBalanceTask -> {
                                                if (referrerBalanceTask.isSuccessful()) {
                                                    double referrerBalance = referrerBalanceTask.getResult().getValue(Double.class);
                                                    mDatabase.child("users").child(referrerId).child("balance").setValue(referrerBalance + 8.0); // Add 8 BXC to referrer
                                                    
                                                    // Increment the referral count
                                                    mDatabase.child("users").child(referrerId).child("referralCount").get().addOnCompleteListener(countTask -> {
                                                        if (countTask.isSuccessful()) {
                                                            int currentCount = countTask.getResult().getValue(Integer.class);
                                                            mDatabase.child("users").child(referrerId).child("referralCount").setValue(currentCount + 1);
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    });
                                }
                                // Proceed to the next activity
                                showSuccess("Registration successful!");
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            } else {
                                // Handle error
                                showError("Failed to save user data: " + task.getException().getMessage());
                            }
                        });
                } else {
                    // Handle registration failure
                    showError("Registration failed: " + task.getException().getMessage());
                }
            });
    }

    private String generateReferralCode(String username) {
        // Generate a unique referral code using username and random numbers
        String baseCode = username.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        String randomNum = String.format("%04d", new Random().nextInt(10000));
        return baseCode.substring(0, Math.min(baseCode.length(), 4)) + randomNum;
    }

    private void processReferral(String newUserId, User newUser, String referralCode) {
        mDatabase.child("users")
            .orderByChild("referralCode")
            .equalTo(referralCode)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Get referrer user
                        DataSnapshot referrerSnapshot = snapshot.getChildren().iterator().next();
                        String referrerId = referrerSnapshot.getKey();
                        User referrer = referrerSnapshot.getValue(User.class);
                        
                        if (referrer != null) {
                            // Update new user with referral info
                            newUser.setReferredBy(referrerId);
                            
                            // Update referrer's stats
                            referrer.setReferralCount(referrer.getReferralCount() + 1);
                            if (referrer.getTeamMembers() == null) {
                                referrer.setTeamMembers(new ArrayList<>());
                            }
                            referrer.getTeamMembers().add(newUserId);
                            
                            // Calculate and update team bonus
                            double bonusAmount = calculateTeamBonus(referrer.getReferralCount());
                            referrer.setTeamBonus(referrer.getTeamBonus() + bonusAmount);
                            
                            // Save both users
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("/users/" + newUserId, newUser);
                            updates.put("/users/" + referrerId, referrer);
                            
                            mDatabase.updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    // Record bonus transaction
                                    recordTeamBonus(referrerId, bonusAmount, newUserId);
                                    // Send notification to referrer
                                    sendReferralNotification(referrerId, newUser.getUsername());
                                })
                                .addOnFailureListener(e -> showError("Failed to update referral: " + e.getMessage()));
                        }
                    } else {
                        // Invalid referral code, save user without referral
                        saveNewUser(newUserId, newUser);
                        showError("Invalid referral code");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showError("Failed to process referral: " + error.getMessage());
                }
            });
    }

    private double calculateTeamBonus(int referralCount) {
        // Example bonus calculation based on referral count
        // You can adjust these values based on your requirements
        if (referralCount <= 5) return 0.5;      // 0.5 BXC for first 5 referrals
        if (referralCount <= 10) return 1.0;     // 1.0 BXC for 6-10 referrals
        if (referralCount <= 20) return 1.5;     // 1.5 BXC for 11-20 referrals
        return 2.0;                              // 2.0 BXC for 20+ referrals
    }

    private void recordTeamBonus(String userId, double amount, String referredUserId) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("userId", userId);
        transaction.put("amount", amount);
        transaction.put("type", "team_bonus");
        transaction.put("referredUser", referredUserId);
        transaction.put("timestamp", System.currentTimeMillis());
        transaction.put("status", "completed");

        mDatabase.child("transactions").push().setValue(transaction);
    }

    private void sendReferralNotification(String userId, String newMemberUsername) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", "New Team Member!");
        notification.put("message", String.format("%s has joined your team! You earned a team bonus.", newMemberUsername));
        notification.put("timestamp", System.currentTimeMillis());
        notification.put("read", false);

        mDatabase.child("notifications").push().setValue(notification);
    }

    private void saveNewUser(String userId, User user) {
        mDatabase.child("users").child(userId).setValue(user)
            .addOnSuccessListener(aVoid -> {
                // Registration successful
                showSuccess("Registration successful!");
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                finish();
            })
            .addOnFailureListener(e -> showError("Failed to save user data: " + e.getMessage()));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
} 