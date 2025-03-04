package com.bluex.mining.admin;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bluex.mining.R;
import com.bluex.mining.models.AdminMessage;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class MessageManagementActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private DatabaseReference mDatabase;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_management);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Message Management");
        }

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        fab = findViewById(R.id.fab);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter(message -> showMessageOptionsDialog(message));
        recyclerView.setAdapter(adapter);

        // Setup FAB
        fab.setOnClickListener(v -> showAddMessageDialog());

        // Load messages
        loadMessages();
    }

    private void loadMessages() {
        showLoading(true);
        mDatabase.child("admin_messages").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<AdminMessage> messages = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AdminMessage message = ds.getValue(AdminMessage.class);
                    if (message != null) {
                        messages.add(message);
                    }
                }
                adapter.setMessages(messages);
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                showError(error.getMessage());
            }
        });
    }

    private void showAddMessageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        showMessageDialog(dialogView, null);
    }

    private void showEditMessageDialog(AdminMessage message) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_message, null);
        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        EditText messageInput = dialogView.findViewById(R.id.messageInput);
        SwitchMaterial importantSwitch = dialogView.findViewById(R.id.importantSwitch);

        titleInput.setText(message.getTitle());
        messageInput.setText(message.getMessage());
        importantSwitch.setChecked(message.isImportant());

        showMessageDialog(dialogView, message);
    }

    private void showMessageDialog(View dialogView, AdminMessage existingMessage) {
        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        EditText messageInput = dialogView.findViewById(R.id.messageInput);
        SwitchMaterial importantSwitch = dialogView.findViewById(R.id.importantSwitch);

        new MaterialAlertDialogBuilder(this)
            .setTitle(existingMessage == null ? "Add Message" : "Edit Message")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String title = titleInput.getText().toString().trim();
                String message = messageInput.getText().toString().trim();
                boolean isImportant = importantSwitch.isChecked();

                if (title.isEmpty() || message.isEmpty()) {
                    showError("All fields are required");
                    return;
                }

                AdminMessage adminMessage = new AdminMessage();
                adminMessage.setTitle(title);
                adminMessage.setMessage(message);
                adminMessage.setImportant(isImportant);
                adminMessage.setTimestamp(System.currentTimeMillis());

                if (existingMessage == null) {
                    addMessage(adminMessage);
                } else {
                    updateMessage(existingMessage.getTimestamp(), adminMessage);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void addMessage(AdminMessage message) {
        String key = mDatabase.child("admin_messages").push().getKey();
        if (key != null) {
            mDatabase.child("admin_messages").child(key).setValue(message)
                .addOnSuccessListener(aVoid -> Toast.makeText(this,
                    "Message sent successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> showError("Failed to send message: " + e.getMessage()));
        }
    }

    private void updateMessage(long timestamp, AdminMessage message) {
        mDatabase.child("admin_messages").orderByChild("timestamp").equalTo(timestamp)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        ds.getRef().setValue(message)
                            .addOnSuccessListener(aVoid -> Toast.makeText(MessageManagementActivity.this,
                                "Message updated successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> showError("Failed to update message: " + e.getMessage()));
                        break;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showError(error.getMessage());
                }
            });
    }

    private void deleteMessage(AdminMessage message) {
        mDatabase.child("admin_messages").orderByChild("timestamp").equalTo(message.getTimestamp())
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        ds.getRef().removeValue()
                            .addOnSuccessListener(aVoid -> Toast.makeText(MessageManagementActivity.this,
                                "Message deleted successfully", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> showError("Failed to delete message: " + e.getMessage()));
                        break;
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showError(error.getMessage());
                }
            });
    }

    private void showMessageOptionsDialog(AdminMessage message) {
        String[] options = {"Edit", "Delete"};
        new MaterialAlertDialogBuilder(this)
            .setTitle("Message Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Edit
                        showEditMessageDialog(message);
                        break;
                    case 1: // Delete
                        showDeleteConfirmation(message);
                        break;
                }
            })
            .show();
    }

    private void showDeleteConfirmation(AdminMessage message) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Message")
            .setMessage("Are you sure you want to delete this message?")
            .setPositiveButton("Delete", (dialog, which) -> deleteMessage(message))
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
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