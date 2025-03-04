package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluex.mining.R;
import com.bluex.mining.adapters.WithdrawalAdapter;
import com.bluex.mining.models.User;
import com.bluex.mining.models.Withdrawal;
import com.bluex.mining.utils.AdminManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.bluex.mining.admin.UserManagementActivity;
import com.bluex.mining.admin.WithdrawalManagementActivity;
import com.bluex.mining.admin.TaskManagementActivity;
import com.bluex.mining.admin.MessageManagementActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminMainActivity extends AppCompatActivity implements WithdrawalAdapter.WithdrawalActionListener {
    private DatabaseReference mDatabase;
    private RecyclerView recyclerView;
    private WithdrawalAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private ValueEventListener withdrawalsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }

        // Initialize views
        recyclerView = findViewById(R.id.withdrawalsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WithdrawalAdapter(this);
        recyclerView.setAdapter(adapter);

        // Verify admin status
        verifyAdminStatus();

        // Load withdrawals
        loadWithdrawals();

        // Setup click listeners
        setupClickListeners();
    }

    private void setupClickListeners() {
        findViewById(R.id.manageUsersButton).setOnClickListener(v -> 
            startActivity(new Intent(this, UserManagementActivity.class)));
            
        findViewById(R.id.manageWithdrawalsButton).setOnClickListener(v -> 
            startActivity(new Intent(this, WithdrawalManagementActivity.class)));
            
        findViewById(R.id.manageTasksButton).setOnClickListener(v -> 
            startActivity(new Intent(this, TaskManagementActivity.class)));
            
        findViewById(R.id.manageMessagesButton).setOnClickListener(v -> 
            startActivity(new Intent(this, MessageManagementActivity.class)));
    }

    private void verifyAdminStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || !AdminManager.isAdmin(currentUser.getEmail())) {
            // Not an admin, kick them out
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }

    private void loadWithdrawals() {
        showLoading(true);
        withdrawalsListener = mDatabase.child("withdrawals")
            .orderByChild("status")
            .equalTo("pending")
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Withdrawal> withdrawals = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        Withdrawal withdrawal = ds.getValue(Withdrawal.class);
                        if (withdrawal != null) {
                            withdrawal.setId(ds.getKey());
                            withdrawals.add(withdrawal);
                        }
                    }
                    adapter.setWithdrawals(withdrawals);
                    showLoading(false);
                    updateEmptyView(withdrawals.isEmpty());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showLoading(false);
                    showError(error.getMessage());
                }
            });
    }

    @Override
    public void onWithdrawalClick(Withdrawal withdrawal) {
        showStatusChangeDialog(withdrawal);
    }

    @Override
    public void onWithdrawalLongClick(Withdrawal withdrawal) {
        showWithdrawalDetails(withdrawal);
    }

    @Override
    public void onStatusChange(Withdrawal withdrawal, String newStatus) {
        updateWithdrawalStatus(withdrawal, newStatus);
    }

    private void showStatusChangeDialog(Withdrawal withdrawal) {
        String[] statuses = {"pending", "processing", "completed", "failed"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Update Status")
                .setItems(statuses, (dialog, which) -> {
                    String newStatus = statuses[which];
                    updateWithdrawalStatus(withdrawal, newStatus);
                })
                .show();
    }

    private void showWithdrawalDetails(Withdrawal withdrawal) {
        String details = String.format(
                "ID: %s\nUser: %s\nAmount: %.5f BXC\nStatus: %s\nTimestamp: %s",
                withdrawal.getId(),
                withdrawal.getUserId(),
                withdrawal.getAmount(),
                withdrawal.getStatus(),
                new java.util.Date(withdrawal.getTimestamp()).toString()
        );

        new MaterialAlertDialogBuilder(this)
                .setTitle("Withdrawal Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private void updateWithdrawalStatus(Withdrawal withdrawal, String newStatus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/withdrawals/" + withdrawal.getId() + "/status", newStatus);

        mDatabase.updateChildren(updates)
                .addOnSuccessListener(aVoid -> 
                    Toast.makeText(this, "Status updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> 
                    showError("Failed to update status: " + e.getMessage()));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (withdrawalsListener != null && mDatabase != null) {
            mDatabase.removeEventListener(withdrawalsListener);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.admin_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 