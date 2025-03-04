package com.bluex.mining.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bluex.mining.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.bluex.mining.models.User;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.widget.ProgressBar;

public class UserManagementActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private DatabaseReference mDatabase;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("User Management");
        }

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(user -> showUserOptionsDialog(user));
        recyclerView.setAdapter(adapter);

        // Load users
        loadUsers();
    }

    private void loadUsers() {
        showLoading(true);
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        user.setUid(ds.getKey());
                        users.add(user);
                    }
                }
                adapter.setUsers(users);
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                showError(error.getMessage());
            }
        });
    }

    private void showUserOptionsDialog(User user) {
        String[] options = {"Edit", "Block/Unblock", "Delete", "Reset Password", "Adjust Rates", "Adjust Balance"};
        new MaterialAlertDialogBuilder(this)
            .setTitle("User Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Edit
                        showEditUserDialog(user);
                        break;
                    case 1: // Block/Unblock
                        toggleUserBlock(user);
                        break;
                    case 2: // Delete
                        showDeleteConfirmation(user);
                        break;
                    case 3: // Reset Password
                        resetUserPassword(user);
                        break;
                    case 4: // Adjust Rates
                        showAdjustRatesDialog(user);
                        break;
                    case 5: // Adjust Balance
                        showAdjustBalanceDialog(user);
                        break;
                }
            })
            .show();
    }

    private void showEditUserDialog(User user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);
        EditText usernameInput = dialogView.findViewById(R.id.usernameInput);
        EditText emailInput = dialogView.findViewById(R.id.emailInput);

        usernameInput.setText(user.getUsername());
        emailInput.setText(user.getEmail());

        new MaterialAlertDialogBuilder(this)
            .setTitle("Edit User")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String username = usernameInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();

                if (username.isEmpty() || email.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                user.setUsername(username);
                user.setEmail(email);
                updateUser(user);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void toggleUserBlock(User user) {
        new MaterialAlertDialogBuilder(this)
            .setTitle(user.isBlocked() ? "Unblock User" : "Block User")
            .setMessage("Are you sure you want to " + 
                (user.isBlocked() ? "unblock" : "block") + " this user?")
            .setPositiveButton("Yes", (dialog, which) -> {
                user.setBlocked(!user.isBlocked());
                updateUser(user);
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void showDeleteConfirmation(User user) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete this user? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> deleteUser(user))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void resetUserPassword(User user) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setMessage("Send password reset email to " + user.getEmail() + "?")
            .setPositiveButton("Yes", (dialog, which) -> {
                FirebaseAuth.getInstance().sendPasswordResetEmail(user.getEmail())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, 
                        "Password reset email sent", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to send reset email: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("No", null)
            .show();
    }

    private void showAdjustRatesDialog(User user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_adjust_user_rates, null);
        EditText miningRateInput = dialogView.findViewById(R.id.miningRateInput);
        EditText referralBonusInput = dialogView.findViewById(R.id.referralBonusInput);
        EditText balanceAdjustmentInput = dialogView.findViewById(R.id.balanceAdjustmentInput);

        miningRateInput.setText(String.valueOf(user.getMiningRate()));
        referralBonusInput.setText(String.valueOf(user.getReferralBonus()));
        balanceAdjustmentInput.setText("0.0");

        new MaterialAlertDialogBuilder(this)
            .setTitle("Adjust User Rates")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                try {
                    double miningRate = Double.parseDouble(miningRateInput.getText().toString().trim());
                    double referralBonus = Double.parseDouble(referralBonusInput.getText().toString().trim());
                    double balanceAdjustment = Double.parseDouble(balanceAdjustmentInput.getText().toString().trim());

                    // Update mining rate and referral bonus
                    user.setMiningRate(miningRate);
                    user.setReferralBonus(referralBonus);

                    // Adjust balance if needed
                    if (balanceAdjustment != 0) {
                        user.setBalance(user.getBalance() + balanceAdjustment);
                    }

                    // Update user in database
                    updateUser(user);

                    // Add to transaction history if balance was adjusted
                    if (balanceAdjustment != 0) {
                        addBalanceAdjustmentToHistory(user.getUid(), balanceAdjustment);
                    }

                } catch (NumberFormatException e) {
                    showError("Please enter valid numbers");
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAdjustBalanceDialog(User user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_adjust_balance, null);
        TextView currentBalanceText = dialogView.findViewById(R.id.currentBalanceText);
        EditText amountInput = dialogView.findViewById(R.id.amountInput);
        EditText reasonInput = dialogView.findViewById(R.id.reasonInput);
        Button decreaseButton = dialogView.findViewById(R.id.decreaseButton);
        Button increaseButton = dialogView.findViewById(R.id.increaseButton);

        currentBalanceText.setText(String.format("Current Balance: %.2f BXC", user.getBalance()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Adjust Balance")
            .setView(dialogView)
            .create();

        decreaseButton.setOnClickListener(v -> {
            adjustBalance(user, amountInput, reasonInput, false);
            dialog.dismiss();
        });

        increaseButton.setOnClickListener(v -> {
            adjustBalance(user, amountInput, reasonInput, true);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void adjustBalance(User user, EditText amountInput, EditText reasonInput, boolean increase) {
        String amountStr = amountInput.getText().toString().trim();
        String reason = reasonInput.getText().toString().trim();

        if (amountStr.isEmpty()) {
            showError("Please enter an amount");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showError("Amount must be greater than 0");
                return;
            }

            // Calculate new balance
            double currentBalance = user.getBalance();
            double newBalance = increase ? currentBalance + amount : currentBalance - amount;

            // Update user balance
            user.setBalance(newBalance);
            updateUser(user);

            // Record transaction
            Map<String, Object> transaction = new HashMap<>();
            transaction.put("userId", user.getUid());
            transaction.put("amount", increase ? amount : -amount);
            transaction.put("type", "admin_adjustment");
            transaction.put("reason", reason.isEmpty() ? "Admin adjustment" : reason);
            transaction.put("timestamp", System.currentTimeMillis());
            transaction.put("status", "completed");
            transaction.put("adminId", FirebaseAuth.getInstance().getCurrentUser().getUid());

            mDatabase.child("transactions").push().setValue(transaction)
                .addOnSuccessListener(aVoid -> Toast.makeText(this,
                    "Balance adjusted successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> showError("Failed to record transaction: " + e.getMessage()));

            // Send notification to user
            Map<String, Object> notification = new HashMap<>();
            notification.put("userId", user.getUid());
            notification.put("title", "Balance Adjustment");
            notification.put("message", String.format("Your balance has been %s by %.2f BXC. Reason: %s",
                increase ? "increased" : "decreased", amount,
                reason.isEmpty() ? "Admin adjustment" : reason));
            notification.put("timestamp", System.currentTimeMillis());
            notification.put("read", false);

            mDatabase.child("notifications").push().setValue(notification);

        } catch (NumberFormatException e) {
            showError("Please enter a valid number");
        }
    }

    private void addBalanceAdjustmentToHistory(String userId, double amount) {
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("userId", userId);
        transaction.put("amount", amount);
        transaction.put("type", "admin_adjustment");
        transaction.put("timestamp", System.currentTimeMillis());
        transaction.put("status", "completed");

        mDatabase.child("transactions").push().setValue(transaction)
            .addOnSuccessListener(aVoid -> Toast.makeText(this,
                "Transaction history updated", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> showError("Failed to update transaction history"));
    }

    private void updateUser(User user) {
        mDatabase.child("users").child(user.getUid()).setValue(user)
            .addOnSuccessListener(aVoid -> Toast.makeText(this,
                "User updated successfully", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this,
                "Failed to update user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteUser(User user) {
        mDatabase.child("users").child(user.getUid()).removeValue()
            .addOnSuccessListener(aVoid -> Toast.makeText(this,
                "User deleted successfully", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this,
                "Failed to delete user: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 