package com.bluex.mining;

import android.os.Bundle;
import android.widget.ArrayAdapter;
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

import com.bluex.mining.models.WithdrawalRequest;

public class AdminWithdrawalRequestsActivity extends AppCompatActivity {
    private ListView withdrawalRequestsListView;
    private DatabaseReference mDatabase;
    private List<String> withdrawalRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_withdrawal_requests);

        withdrawalRequestsListView = findViewById(R.id.withdrawalRequestsListView);
        mDatabase = FirebaseDatabase.getInstance().getReference("withdrawalRequests");
        withdrawalRequests = new ArrayList<>();

        fetchWithdrawalRequests();
    }

    private void fetchWithdrawalRequests() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                withdrawalRequests.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    WithdrawalRequest request = snapshot.getValue(WithdrawalRequest.class);
                    if (request != null) {
                        String requestDetails = "User ID: " + request.getUserId() +
                                                ", Amount: " + request.getAmount() +
                                                ", Mobile: " + request.getMobileNumber() +
                                                ", Status: " + request.getStatus();
                        withdrawalRequests.add(requestDetails);
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(AdminWithdrawalRequestsActivity.this, android.R.layout.simple_list_item_1, withdrawalRequests);
                withdrawalRequestsListView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(AdminWithdrawalRequestsActivity.this, "Failed to load withdrawal requests.", Toast.LENGTH_SHORT).show();
            }
        });
    }
} 