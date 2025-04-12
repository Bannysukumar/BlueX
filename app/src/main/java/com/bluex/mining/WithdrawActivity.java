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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.bluex.mining.models.User;
import com.bluex.mining.models.Withdrawal;
import com.bluex.mining.models.AdminMessage;
import com.bluex.mining.models.WithdrawalRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private static final double MIN_WITHDRAWAL = 20.0;
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

        // Retrieve current user data
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    currentUser = task.getResult().getValue(User.class);
                    updateUI();
                }
            });
        }

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
        withdrawButton.setOnClickListener(v -> processWithdrawal());
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

    private void processWithdrawal() {
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount < MIN_WITHDRAWAL || amount > MAX_WITHDRAWAL) {
            Toast.makeText(this, "Amount must be between " + MIN_WITHDRAWAL + " and " + MAX_WITHDRAWAL, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if mobile number is set
        if (TextUtils.isEmpty(currentUser.getPhoneNumber())) {
            Toast.makeText(this, "Mobile number is required to withdraw funds.", Toast.LENGTH_SHORT).show();
            return;
        }

        double adminCharge = amount * 0.10; // Calculate 10% admin charge
        double totalAmount = amount + adminCharge; // Total amount to deduct from user's balance

        String userId = mAuth.getCurrentUser().getUid();

        // Check sender's balance
        mDatabase.child("users").child(userId).child("balance").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                double currentBalance = task.getResult().getValue(Double.class);
                if (currentBalance >= totalAmount) {
                    // Proceed with the withdrawal
                    withdrawFunds(userId, amount);
                } else {
                    Toast.makeText(this, "Insufficient balance after admin charges", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void withdrawFunds(String userId, double amount) {
        String mobileNumber = currentUser.getPhoneNumber();
        String requestId = mDatabase.child("withdrawalRequests").push().getKey(); // Generate a unique ID for the request

        WithdrawalRequest request = new WithdrawalRequest(userId, amount, mobileNumber, "Pending");

        mDatabase.child("withdrawalRequests").child(requestId).setValue(request)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Withdrawal request submitted successfully!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to submit request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 