package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bluex.mining.adapters.TeamAdapter;
import com.bluex.mining.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TeamActivity extends AppCompatActivity {
    private TextView teamSizeText;
    private TextView totalBonusText;
    private TextView referralCodeText;
    private Button shareButton;
    private RecyclerView teamRecyclerView;
    private ProgressBar progressBar;
    private TeamAdapter adapter;
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Team");
        }

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadTeamData();
    }

    private void initializeViews() {
        teamSizeText = findViewById(R.id.teamSizeText);
        totalBonusText = findViewById(R.id.totalBonusText);
        referralCodeText = findViewById(R.id.referralCodeText);
        shareButton = findViewById(R.id.shareButton);
        teamRecyclerView = findViewById(R.id.teamRecyclerView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        teamRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TeamAdapter();
        teamRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        shareButton.setOnClickListener(v -> shareReferralCode());
    }

    private void loadTeamData() {
        showLoading(true);
        mDatabase.child("users").child(currentUserId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    User currentUser = snapshot.getValue(User.class);
                    if (currentUser != null) {
                        updateUserInfo(currentUser);
                        loadTeamMembers(currentUser.getTeamMembers());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showLoading(false);
                    showError(error.getMessage());
                }
            });
    }

    private void updateUserInfo(User user) {
        int teamSize = user.getTeamMembers() != null ? user.getTeamMembers().size() : 0;
        teamSizeText.setText(String.format(Locale.getDefault(), 
            "Team Size: %d members", teamSize));
        totalBonusText.setText(String.format(Locale.getDefault(),
            "Total Team Bonus: %.2f BXC", user.getTeamBonus()));
        referralCodeText.setText(String.format("Referral Code: %s", 
            user.getReferralCode()));
    }

    private void loadTeamMembers(List<String> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            showLoading(false);
            return;
        }

        List<User> teamMembers = new ArrayList<>();
        int[] loadedCount = {0};

        for (String memberId : memberIds) {
            mDatabase.child("users").child(memberId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User member = snapshot.getValue(User.class);
                        if (member != null) {
                            member.setUserId(snapshot.getKey());
                            teamMembers.add(member);
                        }

                        loadedCount[0]++;
                        if (loadedCount[0] == memberIds.size()) {
                            adapter.setTeamMembers(teamMembers);
                            showLoading(false);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        showError(error.getMessage());
                        showLoading(false);
                    }
                });
        }
    }

    private void shareReferralCode() {
        String referralCode = referralCodeText.getText().toString()
            .replace("Referral Code: ", "");
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join my mining team!");
        shareIntent.putExtra(Intent.EXTRA_TEXT, 
            "Join my mining team in BlueX Mining App! Use my referral code: " + 
            referralCode + " to get started!");
        
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        teamRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
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