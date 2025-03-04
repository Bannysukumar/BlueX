package com.bluex.mining.admin;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bluex.mining.R;
import com.bluex.mining.models.MiningEvent;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalSettingsActivity extends AppCompatActivity {
    private DatabaseReference mDatabase;
    private EditText defaultMiningRateInput;
    private EditText defaultReferralBonusInput;
    private EditText eventNameInput;
    private EditText multiplierInput;
    private EditText durationInput;
    private Button updateRatesButton;
    private Button startEventButton;
    private RecyclerView activeEventsRecyclerView;
    private EventAdapter eventAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_settings);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Global Settings");
        }

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadCurrentSettings();
        loadActiveEvents();
    }

    private void initializeViews() {
        defaultMiningRateInput = findViewById(R.id.defaultMiningRateInput);
        defaultReferralBonusInput = findViewById(R.id.defaultReferralBonusInput);
        eventNameInput = findViewById(R.id.eventNameInput);
        multiplierInput = findViewById(R.id.multiplierInput);
        durationInput = findViewById(R.id.durationInput);
        updateRatesButton = findViewById(R.id.updateRatesButton);
        startEventButton = findViewById(R.id.startEventButton);
        activeEventsRecyclerView = findViewById(R.id.activeEventsRecyclerView);
    }

    private void setupRecyclerView() {
        activeEventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventAdapter = new EventAdapter(event -> showEventOptionsDialog(event));
        activeEventsRecyclerView.setAdapter(eventAdapter);
    }

    private void setupClickListeners() {
        updateRatesButton.setOnClickListener(v -> updateGlobalRates());
        startEventButton.setOnClickListener(v -> startNewEvent());
    }

    private void loadCurrentSettings() {
        mDatabase.child("settings").child("mining_rates").addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Double defaultRate = snapshot.child("default_rate").getValue(Double.class);
                        Double referralBonus = snapshot.child("referral_bonus").getValue(Double.class);
                        
                        if (defaultRate != null) {
                            defaultMiningRateInput.setText(String.valueOf(defaultRate));
                        }
                        if (referralBonus != null) {
                            defaultReferralBonusInput.setText(String.valueOf(referralBonus));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showError(error.getMessage());
                }
            });
    }

    private void updateGlobalRates() {
        try {
            double defaultRate = Double.parseDouble(defaultMiningRateInput.getText().toString().trim());
            double referralBonus = Double.parseDouble(defaultReferralBonusInput.getText().toString().trim());

            Map<String, Object> rates = new HashMap<>();
            rates.put("default_rate", defaultRate);
            rates.put("referral_bonus", referralBonus);

            mDatabase.child("settings").child("mining_rates").setValue(rates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Global rates updated successfully", Toast.LENGTH_SHORT).show();
                    updateAllUsers(defaultRate, referralBonus);
                })
                .addOnFailureListener(e -> showError("Failed to update rates: " + e.getMessage()));

        } catch (NumberFormatException e) {
            showError("Please enter valid numbers");
        }
    }

    private void updateAllUsers(double defaultRate, double referralBonus) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("miningRate", defaultRate);
        updates.put("referralBonus", referralBonus);

        mDatabase.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    if (!userSnapshot.child("isCustomRate").exists() || 
                        !userSnapshot.child("isCustomRate").getValue(Boolean.class)) {
                        userSnapshot.getRef().updateChildren(updates);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError(error.getMessage());
            }
        });
    }

    private void startNewEvent() {
        String name = eventNameInput.getText().toString().trim();
        String multiplierStr = multiplierInput.getText().toString().trim();
        String durationStr = durationInput.getText().toString().trim();

        if (name.isEmpty() || multiplierStr.isEmpty() || durationStr.isEmpty()) {
            showError("All fields are required");
            return;
        }

        try {
            double multiplier = Double.parseDouble(multiplierStr);
            int duration = Integer.parseInt(durationStr);

            MiningEvent event = new MiningEvent();
            event.setName(name);
            event.setMultiplier(multiplier);
            event.setStartTime(System.currentTimeMillis());
            event.setEndTime(System.currentTimeMillis() + (duration * 60 * 60 * 1000)); // Convert hours to milliseconds

            String eventId = mDatabase.child("events").push().getKey();
            if (eventId != null) {
                event.setId(eventId);
                mDatabase.child("events").child(eventId).setValue(event)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Event started successfully", Toast.LENGTH_SHORT).show();
                        clearEventInputs();
                    })
                    .addOnFailureListener(e -> showError("Failed to start event: " + e.getMessage()));
            }

        } catch (NumberFormatException e) {
            showError("Please enter valid numbers");
        }
    }

    private void clearEventInputs() {
        eventNameInput.setText("");
        multiplierInput.setText("");
        durationInput.setText("");
    }

    private void loadActiveEvents() {
        mDatabase.child("events")
            .orderByChild("endTime")
            .startAt(System.currentTimeMillis())
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<MiningEvent> events = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        MiningEvent event = ds.getValue(MiningEvent.class);
                        if (event != null) {
                            event.setId(ds.getKey());
                            events.add(event);
                        }
                    }
                    eventAdapter.setEvents(events);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showError(error.getMessage());
                }
            });
    }

    private void showEventOptionsDialog(MiningEvent event) {
        String[] options = {"End Event", "Delete"};
        new MaterialAlertDialogBuilder(this)
            .setTitle("Event Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // End Event
                        endEvent(event);
                        break;
                    case 1: // Delete
                        deleteEvent(event);
                        break;
                }
            })
            .show();
    }

    private void endEvent(MiningEvent event) {
        event.setEndTime(System.currentTimeMillis());
        mDatabase.child("events").child(event.getId()).setValue(event)
            .addOnSuccessListener(aVoid -> Toast.makeText(this,
                "Event ended successfully", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> showError("Failed to end event: " + e.getMessage()));
    }

    private void deleteEvent(MiningEvent event) {
        mDatabase.child("events").child(event.getId()).removeValue()
            .addOnSuccessListener(aVoid -> Toast.makeText(this,
                "Event deleted successfully", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> showError("Failed to delete event: " + e.getMessage()));
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eventAdapter != null) {
            eventAdapter.onDestroy();
        }
    }
} 