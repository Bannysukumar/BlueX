package com.bluex.mining;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluex.mining.adapters.WithdrawalAdapter;
import com.bluex.mining.models.Withdrawal;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private WithdrawalAdapter adapter;
    private ProgressBar progressBar;
    private TextView emptyView;
    private DatabaseReference mDatabase;
    private ValueEventListener withdrawalsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Transaction History");
        }

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WithdrawalAdapter();
        recyclerView.setAdapter(adapter);

        // Load withdrawals
        loadWithdrawals(userId);
    }

    private void loadWithdrawals(String userId) {
        showLoading(true);

        Query query = mDatabase.child("withdrawals")
                .orderByChild("userId")
                .equalTo(userId);

        withdrawalsListener = query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Withdrawal> withdrawals = new ArrayList<>();
                for (DataSnapshot withdrawalSnapshot : snapshot.getChildren()) {
                    Withdrawal withdrawal = withdrawalSnapshot.getValue(Withdrawal.class);
                    if (withdrawal != null) {
                        withdrawals.add(withdrawal);
                    }
                }

                // Sort by timestamp descending (newest first)
                Collections.sort(withdrawals, (w1, w2) -> 
                    Long.compare(w2.getTimestamp(), w1.getTimestamp()));

                showLoading(false);
                adapter.setWithdrawals(withdrawals);
                updateEmptyView(withdrawals.isEmpty());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                updateEmptyView(true);
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (withdrawalsListener != null && mDatabase != null) {
            mDatabase.removeEventListener(withdrawalsListener);
        }
    }
} 