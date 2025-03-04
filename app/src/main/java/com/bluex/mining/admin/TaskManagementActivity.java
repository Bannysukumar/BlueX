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
import com.bluex.mining.models.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class TaskManagementActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private DatabaseReference mDatabase;
    private ProgressBar progressBar;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_management);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Task Management");
        }

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        fab = findViewById(R.id.fab);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(task -> showTaskOptionsDialog(task));
        recyclerView.setAdapter(adapter);

        // Setup FAB
        fab.setOnClickListener(v -> showAddTaskDialog());

        // Load tasks
        loadTasks();
    }

    private void loadTasks() {
        showLoading(true);
        mDatabase.child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Task> tasks = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Task task = ds.getValue(Task.class);
                    if (task != null) {
                        task.setId(ds.getKey());
                        tasks.add(task);
                    }
                }
                adapter.setTasks(tasks);
                showLoading(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                showError(error.getMessage());
            }
        });
    }

    private void showAddTaskDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_task, null);
        showTaskDialog(dialogView, null);
    }

    private void showEditTaskDialog(Task task) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_task, null);
        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        EditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        EditText rewardInput = dialogView.findViewById(R.id.rewardInput);

        titleInput.setText(task.getTitle());
        descriptionInput.setText(task.getDescription());
        rewardInput.setText(String.valueOf(task.getReward()));

        showTaskDialog(dialogView, task);
    }

    private void showTaskDialog(View dialogView, Task existingTask) {
        EditText titleInput = dialogView.findViewById(R.id.titleInput);
        EditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        EditText rewardInput = dialogView.findViewById(R.id.rewardInput);

        new MaterialAlertDialogBuilder(this)
            .setTitle(existingTask == null ? "Add Task" : "Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save", (dialog, which) -> {
                String title = titleInput.getText().toString().trim();
                String description = descriptionInput.getText().toString().trim();
                String rewardStr = rewardInput.getText().toString().trim();

                if (title.isEmpty() || description.isEmpty() || rewardStr.isEmpty()) {
                    showError("All fields are required");
                    return;
                }

                double reward;
                try {
                    reward = Double.parseDouble(rewardStr);
                } catch (NumberFormatException e) {
                    showError("Invalid reward amount");
                    return;
                }

                Task task = existingTask != null ? existingTask : new Task();
                task.setTitle(title);
                task.setDescription(description);
                task.setReward(reward);

                if (existingTask == null) {
                    addTask(task);
                } else {
                    updateTask(task);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void addTask(Task task) {
        String taskId = mDatabase.child("tasks").push().getKey();
        if (taskId != null) {
            task.setId(taskId);
            mDatabase.child("tasks").child(taskId).setValue(task)
                .addOnSuccessListener(aVoid -> Toast.makeText(this,
                    "Task added successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> showError("Failed to add task: " + e.getMessage()));
        }
    }

    private void updateTask(Task task) {
        mDatabase.child("tasks").child(task.getId()).setValue(task)
            .addOnSuccessListener(aVoid -> Toast.makeText(this,
                "Task updated successfully", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> showError("Failed to update task: " + e.getMessage()));
    }

    private void deleteTask(Task task) {
        mDatabase.child("tasks").child(task.getId()).removeValue()
            .addOnSuccessListener(aVoid -> Toast.makeText(this,
                "Task deleted successfully", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> showError("Failed to delete task: " + e.getMessage()));
    }

    private void showTaskOptionsDialog(Task task) {
        String[] options = {"Edit", "Delete"};
        new MaterialAlertDialogBuilder(this)
            .setTitle("Task Options")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Edit
                        showEditTaskDialog(task);
                        break;
                    case 1: // Delete
                        showDeleteConfirmation(task);
                        break;
                }
            })
            .show();
    }

    private void showDeleteConfirmation(Task task) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete", (dialog, which) -> deleteTask(task))
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