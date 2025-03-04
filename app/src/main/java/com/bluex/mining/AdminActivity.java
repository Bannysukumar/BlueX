package com.bluex.mining;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluex.mining.adapters.UserAdapter;
import com.bluex.mining.models.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminActivity extends AppCompatActivity implements UserAdapter.UserActionListener {
    private static final String MINING_CONFIG_PATH = "mining_config";
    private DatabaseReference mDatabase;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize RecyclerView
        RecyclerView recyclerView = findViewById(R.id.usersRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(this);
        recyclerView.setAdapter(userAdapter);

        // Load users
        loadUsers();

        // Initialize buttons
        findViewById(R.id.setRatesButton).setOnClickListener(v -> showMiningRatesDialog());
        findViewById(R.id.pendingKycButton).setOnClickListener(v -> showPendingKYCUsers());
    }

    private void loadUsers() {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    try {
                        User user = userSnapshot.getValue(User.class);
                        if (user != null) {
                            user.setUserId(userSnapshot.getKey());
                            users.add(user);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                userAdapter.setUsers(users);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminActivity.this, "Error loading users: " + error.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onUserClick(User user) {
        try {
            if (user == null) {
                Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (user.getUserId() == null) {
                Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show();
                return;
            }

            showUserActionDialog(user);
        } catch (Exception e) {
            Toast.makeText(this, "Error showing user actions", Toast.LENGTH_SHORT).show();
            e.printStackTrace(); // Log the error for debugging
        }
    }

    private void showUserActionDialog(User user) {
        try {
            String[] actions = {
                "Edit User",
                "Set Mining Rate",
                user.isBlocked() ? "Unblock User" : "Block User",
                user.isKycVerified() ? "Revoke KYC" : "Verify KYC",
                "Reset Balance",
                "Delete User"
            };

            new MaterialAlertDialogBuilder(this)
                .setTitle("User Actions: " + (user.getDisplayName() != null ? user.getDisplayName() : "Unknown User"))
                .setItems(actions, (dialog, which) -> {
                    try {
                        switch (which) {
                            case 0: // Edit User
                                showEditUserDialog(user);
                                break;
                            case 1: // Set Mining Rate
                                showSetUserMiningRateDialog(user);
                                break;
                            case 2: // Block/Unblock
                                toggleUserBlock(user);
                                break;
                            case 3: // KYC
                                toggleUserVerification(user);
                                break;
                            case 4: // Reset Balance
                                showResetBalanceConfirmation(user);
                                break;
                            case 5: // Delete
                                showDeleteConfirmation(user);
                                break;
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Error performing action", Toast.LENGTH_SHORT).show();
                        e.printStackTrace(); // Log the error for debugging
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        } catch (Exception e) {
            Toast.makeText(this, "Error showing dialog", Toast.LENGTH_SHORT).show();
            e.printStackTrace(); // Log the error for debugging
        }
    }

    private void toggleUserBlock(User user) {
        user.setBlocked(!user.isBlocked());
        updateUser(user);
    }

    private void toggleUserVerification(User user) {
        user.setKycVerified(!user.isKycVerified());
        user.setKycStatus(user.isKycVerified() ? "approved" : "rejected");
        updateUser(user);
    }

    private void showDeleteConfirmation(User user) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to delete this user? This action cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                mDatabase.child("users").child(user.getUserId()).removeValue()
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showEditUserDialog(User user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);
        EditText displayNameInput = dialogView.findViewById(R.id.displayNameInput);
        EditText emailInput = dialogView.findViewById(R.id.emailInput);
        
        displayNameInput.setText(user.getDisplayName());
        emailInput.setText(user.getEmail());

        new MaterialAlertDialogBuilder(this)
            .setTitle("Edit User")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                user.setDisplayName(displayNameInput.getText().toString());
                user.setEmail(emailInput.getText().toString());
                updateUser(user);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showSetUserMiningRateDialog(User user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_set_mining_rate, null);
        EditText rateInput = dialogView.findViewById(R.id.rateInput);
        rateInput.setText(String.valueOf(user.getMiningRate()));

        new MaterialAlertDialogBuilder(this)
            .setTitle("Set Mining Rate")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                try {
                    double rate = Double.parseDouble(rateInput.getText().toString());
                    user.setMiningRate(rate);
                    updateUser(user);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid rate format", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showMiningRatesDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_mining_rates, null);
        EditText baseRateInput = dialogView.findViewById(R.id.baseRateInput);
        EditText referralBonusInput = dialogView.findViewById(R.id.referralBonusInput);
        EditText kycBonusInput = dialogView.findViewById(R.id.kycBonusInput);

        // Load current values
        mDatabase.child(MINING_CONFIG_PATH).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                baseRateInput.setText(String.valueOf(snapshot.child("baseRate").getValue(Double.class)));
                referralBonusInput.setText(String.valueOf(snapshot.child("referralBonus").getValue(Double.class)));
                kycBonusInput.setText(String.valueOf(snapshot.child("kycBonus").getValue(Double.class)));
            }
        });

        new MaterialAlertDialogBuilder(this)
            .setTitle("Set Mining Rates")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                try {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("baseRate", Double.parseDouble(baseRateInput.getText().toString()));
                    updates.put("referralBonus", Double.parseDouble(referralBonusInput.getText().toString()));
                    updates.put("kycBonus", Double.parseDouble(kycBonusInput.getText().toString()));
                    
                    mDatabase.child(MINING_CONFIG_PATH).updateChildren(updates)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Rates updated", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid rate format", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showPendingKYCUsers() {
        mDatabase.child("users")
            .orderByChild("kycStatus")
            .equalTo("pending")
            .get()
            .addOnSuccessListener(snapshot -> {
                List<User> pendingUsers = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        user.setUserId(userSnapshot.getKey());
                        pendingUsers.add(user);
                    }
                }
                showPendingKYCDialog(pendingUsers);
            });
    }

    private void showPendingKYCDialog(List<User> pendingUsers) {
        if (pendingUsers.isEmpty()) {
            Toast.makeText(this, "No pending KYC requests", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] userNames = pendingUsers.stream()
            .map(User::getDisplayName)
            .toArray(String[]::new);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Pending KYC Requests")
            .setItems(userNames, (dialog, which) -> {
                User selectedUser = pendingUsers.get(which);
                showKYCDetailsDialog(selectedUser);
            })
            .show();
    }

    private void showKYCDetailsDialog(User user) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_kyc_details, null);
        // Set KYC details in the dialog
        // This depends on what KYC data you collect

        new MaterialAlertDialogBuilder(this)
            .setTitle("KYC Details")
            .setView(dialogView)
            .setPositiveButton("Approve", (dialog, which) -> approveKYC(user))
            .setNegativeButton("Reject", (dialog, which) -> rejectKYC(user))
            .show();
    }

    private void approveKYC(User user) {
        user.setKycVerified(true);
        user.setKycStatus("approved");
        updateUser(user);
    }

    private void rejectKYC(User user) {
        user.setKycStatus("rejected");
        updateUser(user);
    }

    private void showResetBalanceConfirmation(User user) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Reset Balance")
            .setMessage("Are you sure you want to reset this user's balance to 0?")
            .setPositiveButton("Reset", (dialog, which) -> {
                user.setBalance(0);
                updateUser(user);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateUser(User user) {
        mDatabase.child("users").child(user.getUserId()).setValue(user)
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "User updated", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
    }
} 