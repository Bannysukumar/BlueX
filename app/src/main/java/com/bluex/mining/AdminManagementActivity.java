package com.bluex.mining;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bluex.mining.models.Admin;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdminManagementActivity extends AppCompatActivity {
    private EditText emailInput, displayNameInput;
    private Button addAdminButton;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_management);

        emailInput = findViewById(R.id.emailInput);
        displayNameInput = findViewById(R.id.displayNameInput);
        addAdminButton = findViewById(R.id.addAdminButton);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("admins");

        addAdminButton.setOnClickListener(v -> addAdmin());
    }

    private void addAdmin() {
        String email = emailInput.getText().toString().trim();
        String displayName = displayNameInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(displayName)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new admin object
        String uid = mDatabase.push().getKey(); // Generate a unique ID for the new admin


        Admin newAdmin = new Admin(uid, email, displayName);

        // Save the new admin to the database
        mDatabase.child(uid).setValue(newAdmin)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Admin added successfully!", Toast.LENGTH_SHORT).show();
                emailInput.setText("");
                displayNameInput.setText("");
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to add admin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
} 