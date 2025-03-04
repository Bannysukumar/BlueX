package com.bluex.mining;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.bluex.mining.models.User;
import com.bluex.mining.models.Withdrawal;
import com.bluex.mining.models.AdminMessage;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.Map;
import android.util.Log;

public class WithdrawActivity extends AppCompatActivity {
    private static final String TAG = "WithdrawActivity";
    private static final double MIN_WITHDRAWAL = 400.0;
    private static final double MAX_WITHDRAWAL = 10000.0;
    
    private TextView balanceText;
    private TextInputEditText amountInput;
    private MaterialButton withdrawButton;
    private ProgressBar progressBar;
    private TextView statusText;
    private TextView adminMessageTitle;
    private TextView adminMessageText;
    private CardView adminMessageCard;
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        balanceText = findViewById(R.id.balanceText);
        amountInput = findViewById(R.id.amountInput);
        withdrawButton = findViewById(R.id.withdrawButton);
        progressBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.statusText);
        adminMessageTitle = findViewById(R.id.adminMessageTitle);
        adminMessageText = findViewById(R.id.adminMessageText);
        adminMessageCard = findViewById(R.id.adminMessageCard);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Withdraw BXC");
        }

        // Load user data
        loadUserData();

        // Load admin messages
        loadAdminMessages();

        // Add text change listener for real-time validation
        amountInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateAmount();
            }
        });

        // Set up withdraw button
        withdrawButton.setOnClickListener(v -> {
            if (validateAmount()) {
                double amount = Double.parseDouble(amountInput.getText().toString());
                showConfirmationDialog(amount);
            }
        });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("users").child(userId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    currentUser = snapshot.getValue(User.class);
                    updateUI();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showError("Failed to load user data");
                }
            });
    }

    private void loadAdminMessages() {
        mDatabase.child("admin_messages")
            .orderByChild("timestamp")
            .limitToLast(1)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        adminMessageCard.setVisibility(View.GONE);
                        return;
                    }

                    for (DataSnapshot messageSnap : snapshot.getChildren()) {
                        AdminMessage message = messageSnap.getValue(AdminMessage.class);
                        if (message != null) {
                            adminMessageCard.setVisibility(View.VISIBLE);
                            adminMessageTitle.setText(message.getTitle());
                            adminMessageText.setText(message.getMessage());
                            
                            // Set background color based on importance
                            if (message.isImportant()) {
                                adminMessageCard.setCardBackgroundColor(
                                    getResources().getColor(R.color.light_red));
                                adminMessageTitle.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_warning, 0, 0, 0);
                            } else {
                                adminMessageCard.setCardBackgroundColor(
                                    getResources().getColor(R.color.white));
                                adminMessageTitle.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_info, 0, 0, 0);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Error loading admin messages", error.toException());
                    adminMessageCard.setVisibility(View.GONE);
                }
            });
    }

    private boolean validateAmount() {
        String amountStr = amountInput.getText().toString();
        if (amountStr.isEmpty()) {
            amountInput.setError("Please enter an amount");
            withdrawButton.setEnabled(false);
            return false;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount < MIN_WITHDRAWAL) {
                amountInput.setError("Minimum withdrawal is " + MIN_WITHDRAWAL + " BXC");
                withdrawButton.setEnabled(false);
                return false;
            }
            if (amount > MAX_WITHDRAWAL) {
                amountInput.setError("Maximum withdrawal is " + MAX_WITHDRAWAL + " BXC");
                withdrawButton.setEnabled(false);
                return false;
            }
            if (currentUser != null && amount > currentUser.getBalance()) {
                amountInput.setError("Insufficient balance");
                withdrawButton.setEnabled(false);
                return false;
            }
            withdrawButton.setEnabled(true);
            return true;
        } catch (NumberFormatException e) {
            amountInput.setError("Invalid amount");
            withdrawButton.setEnabled(false);
            return false;
        }
    }

    private void showConfirmationDialog(double amount) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Confirm Withdrawal")
            .setMessage(String.format("Are you sure you want to withdraw %.5f BXC?", amount))
            .setPositiveButton("Confirm", (dialog, which) -> processWithdrawal(amount))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void processWithdrawal(double amount) {
        if (currentUser == null || !validateAmount()) return;

        showLoading(true);
        statusText.setText("Processing withdrawal...");

        String userId = mAuth.getCurrentUser().getUid();
        String withdrawalId = mDatabase.child("withdrawals").push().getKey();

        Map<String, Object> withdrawalData = new HashMap<>();
        withdrawalData.put("userId", userId);
        withdrawalData.put("amount", amount);
        withdrawalData.put("status", "pending");
        withdrawalData.put("timestamp", System.currentTimeMillis());
        withdrawalData.put("walletAddress", currentUser.getWalletAddress());

        Map<String, Object> updates = new HashMap<>();
        updates.put("/withdrawals/" + withdrawalId, withdrawalData);
        updates.put("/users/" + userId + "/balance", currentUser.getBalance() - amount);
        updates.put("/users/" + userId + "/pendingWithdrawals", currentUser.getPendingWithdrawals() + 1);

        mDatabase.updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                showLoading(false);
                statusText.setText("Withdrawal request submitted successfully!");
                statusText.setTextColor(getResources().getColor(R.color.green));
                new Handler().postDelayed(() -> finish(), 2000);
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                statusText.setText("Withdrawal failed: " + e.getMessage());
                statusText.setTextColor(getResources().getColor(R.color.red));
            });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        amountInput.setEnabled(!show);
        withdrawButton.setEnabled(!show);
    }

    private void updateUI() {
        if (currentUser != null) {
            balanceText.setText(String.format("Available Balance: %.5f BXC", currentUser.getBalance()));
            withdrawButton.setEnabled(currentUser.getBalance() >= MIN_WITHDRAWAL);
            
            if (currentUser.getBalance() < MIN_WITHDRAWAL) {
                statusText.setText("Minimum withdrawal amount is " + MIN_WITHDRAWAL + " BXC");
                statusText.setTextColor(getResources().getColor(R.color.orange));
            } else {
                statusText.setText("Enter amount to withdraw");
                statusText.setTextColor(getResources().getColor(R.color.gray));
            }
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 