package com.bluex.mining.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bluex.mining.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private Button loginButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);

        // Pre-fill admin email for testing
        emailInput.setText("bannysukumar@gmail.com");

        loginButton.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email required");
            return;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password required");
            return;
        }

        // Verify if it's the admin email
        if (!email.equals("bannysukumar@gmail.com")) {
            showError("Not an admin email");
            return;
        }

        showProgress(true);

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                // Check if user is admin
                mDatabase.child("admins")
                    .child(authResult.getUser().getUid())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            startActivity(new Intent(this, AdminDashboardActivity.class));
                            finish();
                        } else {
                            mAuth.signOut();
                            showError("Not authorized as admin");
                        }
                        showProgress(false);
                    })
                    .addOnFailureListener(e -> {
                        mAuth.signOut();
                        showError(e.getMessage());
                        showProgress(false);
                    });
            })
            .addOnFailureListener(e -> {
                showError(e.getMessage());
                showProgress(false);
            });
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
} 