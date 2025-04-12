package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import com.bluex.mining.models.Withdrawal;
import com.bluex.mining.models.Admin;

public class AdminPanelActivity extends AppCompatActivity {
    private ListView withdrawalListView;
    private DatabaseReference mDatabase;
    private List<String> withdrawalRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        withdrawalListView = findViewById(R.id.withdrawalListView);
        mDatabase = FirebaseDatabase.getInstance().getReference("withdrawals");
        withdrawalRequests = new ArrayList<>();

        fetchWithdrawalRequests();

        Button addAdminButton = findViewById(R.id.addAdminButton);
        addAdminButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPanelActivity.this, AdminManagementActivity.class);
            startActivity(intent);
        });
    }

    private void fetchWithdrawalRequests() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                withdrawalRequests.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Withdrawal withdrawal = snapshot.getValue(Withdrawal.class);
                    if (withdrawal != null) {
                        String requestDetails = "User ID: " + withdrawal.getUserId() +
                                                ", Amount: " + withdrawal.getAmount() +
                                                ", Mobile: " + withdrawal.getMobileNumber() +
                                                ", Status: " + withdrawal.getStatus();
                        withdrawalRequests.add(requestDetails);
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminPanelActivity.this, android.R.layout.simple_list_item_1, withdrawalRequests);
                withdrawalListView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AdminPanelActivity.this, "Failed to load withdrawal requests.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAdmins() {
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("admins");
        adminRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<String> adminList = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Admin admin = snapshot.getValue(Admin.class);
                    if (admin != null) {
                        adminList.add(admin.getDisplayName() + " (" + admin.getEmail() + ")");
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminPanelActivity.this, android.R.layout.simple_list_item_1, adminList);
                withdrawalListView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AdminPanelActivity.this, "Failed to load admins.", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 