package com.bluex.mining;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bluex.mining.models.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private Button weeklyButton;
    private Button monthlyButton;
    private DatabaseReference mDatabase;
    private boolean isWeeklyMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Leaderboard");
        }

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initializeViews();

        // Load initial data
        loadLeaderboardData(true);
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyView = findViewById(R.id.emptyView);
        weeklyButton = findViewById(R.id.weeklyButton);
        monthlyButton = findViewById(R.id.monthlyButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        weeklyButton.setOnClickListener(v -> {
            isWeeklyMode = true;
            updateButtonStates();
            loadLeaderboardData(true);
        });

        monthlyButton.setOnClickListener(v -> {
            isWeeklyMode = false;
            updateButtonStates();
            loadLeaderboardData(false);
        });

        updateButtonStates();
    }

    private void updateButtonStates() {
        weeklyButton.setEnabled(!isWeeklyMode);
        monthlyButton.setEnabled(isWeeklyMode);
        weeklyButton.setAlpha(isWeeklyMode ? 1.0f : 0.5f);
        monthlyButton.setAlpha(isWeeklyMode ? 0.5f : 1.0f);
    }

    private void loadLeaderboardData(boolean isWeekly) {
        showLoading(true);

        String path = isWeekly ? "weeklyStats" : "monthlyStats";
        Query query = mDatabase.child("users")
                .orderByChild(path + "/totalMined")
                .limitToLast(50);  // Top 50 users

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    User user = userSnapshot.getValue(User.class);
                    if (user != null) {
                        users.add(user);
                    }
                }

                showLoading(false);
                updateEmptyView(users.isEmpty());
                // TODO: Set up and update adapter
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
} 