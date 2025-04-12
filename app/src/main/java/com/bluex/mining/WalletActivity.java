package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bluex.mining.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WalletActivity extends AppCompatActivity {
    private TextView balanceText;
    private Button withdrawButton;
    private Button historyButton;
    private TextView minWithdrawText;
    private DatabaseReference mDatabase;
    private ValueEventListener userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Wallet");
        }

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        balanceText = findViewById(R.id.balanceText);
        withdrawButton = findViewById(R.id.withdrawButton);
        historyButton = findViewById(R.id.historyButton);
        minWithdrawText = findViewById(R.id.minWithdrawText);

        // Setup click listeners
        withdrawButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, WithdrawActivity.class);
            intent.putExtra("balance", getIntent().getDoubleExtra("balance", 0.0));
            startActivity(intent);
        });

        historyButton.setOnClickListener(v -> 
            startActivity(new Intent(this, TransactionHistoryActivity.class)));

        // Load user data
        loadUserData(userId);
    }

    private void loadUserData(String userId) {
        userListener = mDatabase.child("users").child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            updateUI(user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error
                    }
                });
    }

    private void updateUI(User user) {
        balanceText.setText(String.format("BXC %.5f", user.getBalance()));
        boolean hasMinBalance = user.getBalance() >= 400.0;
        withdrawButton.setEnabled(hasMinBalance);
        minWithdrawText.setVisibility(hasMinBalance ? View.GONE : View.VISIBLE);
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
        if (userListener != null && mDatabase != null) {
            mDatabase.removeEventListener(userListener);
        }
    }
} 