package com.bluex.mining;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bluex.mining.adapters.TaskAdapter;
import com.bluex.mining.models.Task;
import com.bluex.mining.models.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends BaseActivity implements TaskAdapter.TaskClickListener {
    
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private List<Task> tasks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Tasks");
            }
        }

        // Set up RecyclerView
        recyclerView = findViewById(R.id.tasksRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TaskAdapter(tasks, this);
        recyclerView.setAdapter(adapter);

        loadTasks();
    }

    private void loadTasks() {
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("tasks").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                tasks.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Task task = snapshot.getValue(Task.class);
                    if (task != null) {
                        tasks.add(task);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(TasksActivity.this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onVerifyTask(Task task) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_verify_task, null);
        EditText keyInput = dialogView.findViewById(R.id.keyInput);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Verify Task")
            .setView(dialogView)
            .setPositiveButton("Verify", (dialog, which) -> {
                String enteredKey = keyInput.getText().toString().trim();
                verifyTask(task, enteredKey);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void verifyTask(Task task, String enteredKey) {
        if (enteredKey.equals(task.getVerificationKey())) {
            String userId = mAuth.getCurrentUser().getUid();
            
            // Update task completion status
            mDatabase.child("tasks").child(task.getId()).child("completed").setValue(true);

            // Add reward to user's balance
            mDatabase.child("users").child(userId).runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    User user = mutableData.getValue(User.class);
                    if (user != null) {
                        user.setBalance(user.getBalance() + task.getReward());
                    }
                    mutableData.setValue(user);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                    if (committed) {
                        Toast.makeText(TasksActivity.this, 
                            "Task completed! Earned " + task.getReward() + " BXC", 
                            Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(TasksActivity.this, 
                            "Failed to update balance", 
                            Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(this, "Invalid verification key", Toast.LENGTH_SHORT).show();
        }
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