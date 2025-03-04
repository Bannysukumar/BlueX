package com.bluex.mining.admin;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bluex.mining.R;
import com.bluex.mining.models.Quest;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.List;

public class QuestAdminActivity extends AppCompatActivity implements QuestAdminAdapter.QuestActionListener {
    private RecyclerView questsRecyclerView;
    private TabLayout tabLayout;
    private DatabaseReference mDatabase;
    private QuestAdminAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quest_admin);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        questsRecyclerView = findViewById(R.id.questsRecyclerView);
        tabLayout = findViewById(R.id.tabLayout);

        // Setup RecyclerView
        adapter = new QuestAdminAdapter(this);
        questsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        questsRecyclerView.setAdapter(adapter);

        // Load quests based on selected tab
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadQuests(tab.getPosition() == 0 ? "weekly" : "basic");
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Load initial quests
        loadQuests("weekly");

        // Setup add button
        findViewById(R.id.addQuestButton).setOnClickListener(v -> showAddQuestDialog());
    }

    private void loadQuests(String type) {
        mDatabase.child("quests")
                .orderByChild("type")
                .equalTo(type)
                .get()
                .addOnSuccessListener(dataSnapshot -> {
                    List<Quest> quests = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Quest quest = snapshot.getValue(Quest.class);
                        if (quest != null) {
                            quest.setId(snapshot.getKey());
                            quests.add(quest);
                        }
                    }
                    adapter.setQuests(quests);
                });
    }

    private void showAddQuestDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quest_edit, null);
        setupDialogSpinner(dialogView);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add New Quest")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String title = getTextFromInput(dialogView, R.id.titleInput);
                    String description = getTextFromInput(dialogView, R.id.descriptionInput);
                    double reward = getDoubleFromInput(dialogView, R.id.rewardInput);
                    String platform = ((Spinner) dialogView.findViewById(R.id.platformSpinner))
                            .getSelectedItem().toString().toLowerCase();

                    Quest newQuest = new Quest(title, description, reward, 
                            tabLayout.getSelectedTabPosition() == 0 ? "weekly" : "basic", platform);
                    
                    String key = mDatabase.child("quests").push().getKey();
                    if (key != null) {
                        newQuest.setId(key);
                        mDatabase.child("quests").child(key).setValue(newQuest)
                                .addOnSuccessListener(aVoid -> loadQuests(newQuest.getType()));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditQuestDialog(Quest quest) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_quest_edit, null);
        setupDialogSpinner(dialogView);

        // Pre-fill existing data
        ((TextInputEditText) dialogView.findViewById(R.id.titleInput)).setText(quest.getTitle());
        ((TextInputEditText) dialogView.findViewById(R.id.descriptionInput)).setText(quest.getDescription());
        ((TextInputEditText) dialogView.findViewById(R.id.rewardInput)).setText(String.valueOf(quest.getReward()));
        
        Spinner platformSpinner = dialogView.findViewById(R.id.platformSpinner);
        int position = getPlatformPosition(quest.getPlatform());
        if (position >= 0) {
            platformSpinner.setSelection(position);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Quest")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    quest.setTitle(getTextFromInput(dialogView, R.id.titleInput));
                    quest.setDescription(getTextFromInput(dialogView, R.id.descriptionInput));
                    quest.setReward(getDoubleFromInput(dialogView, R.id.rewardInput));
                    quest.setPlatform(platformSpinner.getSelectedItem().toString().toLowerCase());

                    mDatabase.child("quests").child(quest.getId()).setValue(quest)
                            .addOnSuccessListener(aVoid -> loadQuests(quest.getType()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupDialogSpinner(View dialogView) {
        Spinner spinner = dialogView.findViewById(R.id.platformSpinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Twitter", "YouTube", "Other"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private String getTextFromInput(View dialogView, int inputId) {
        TextInputEditText input = dialogView.findViewById(inputId);
        return input.getText() != null ? input.getText().toString() : "";
    }

    private double getDoubleFromInput(View dialogView, int inputId) {
        String text = getTextFromInput(dialogView, inputId);
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int getPlatformPosition(String platform) {
        switch (platform.toLowerCase()) {
            case "twitter": return 0;
            case "youtube": return 1;
            default: return 2;
        }
    }

    @Override
    public void onEditQuest(Quest quest) {
        // Show edit dialog
        showEditQuestDialog(quest);
    }

    @Override
    public void onDeleteQuest(Quest quest) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Quest")
                .setMessage("Are you sure you want to delete this quest?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    mDatabase.child("quests").child(quest.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> loadQuests(quest.getType()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
} 