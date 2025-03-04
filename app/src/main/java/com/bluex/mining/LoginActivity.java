package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.bluex.mining.models.User;
import com.bluex.mining.utils.AdminManager;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private TextInputEditText emailInput, passwordInput;
    private MaterialButton loginButton, registerButton;
    private SignInButton googleSignInButton;
    private View progressBar;
    private TextView forgotPasswordText;
    private TextView registerText;
    private Button adminLoginButton;

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                handleGoogleSignInResult(task);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize views
        initializeViews();
        
        // Setup click listeners
        setupClickListeners();

        // Add text change listener to email for admin detection
        emailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show admin login button if email is an admin email
                adminLoginButton.setVisibility(
                    AdminManager.isAdmin(s.toString()) ? View.VISIBLE : View.GONE
                );
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        adminLoginButton = findViewById(R.id.adminLoginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        progressBar = findViewById(R.id.progressBar);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        registerText = findViewById(R.id.registerText);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Check if the user is an admin
            if (AdminManager.isAdmin(currentUser.getEmail())) {
                startActivity(new Intent(this, AdminMainActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }
    }

    private void setupClickListeners() {
        forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());
        registerText.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        loginButton.setOnClickListener(v -> handleLogin());
        googleSignInButton.setOnClickListener(v -> handleGoogleSignIn());
        adminLoginButton.setOnClickListener(v -> handleAdminLogin());
    }

    private void handleLogin() {
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

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                showLoading(false);
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && AdminManager.isAdmin(user.getEmail())) {
                        startAdminActivity();
                    } else {
                        startMainActivity();
                    }
                } else {
                    showError("Login failed: " + task.getException().getMessage());
                }
            });
    }

    private void handleGoogleSignIn() {
        showLoading(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            showLoading(false);
            Toast.makeText(this, "Google sign in failed: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        showLoading(true);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if user is admin
                            if (AdminManager.isAdmin(user.getEmail())) {
                                startAdminActivity();
                            } else {
                                checkAndCreateUser(user);
                            }
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                        showLoading(false);
                    }
                });
    }

    private void checkAndCreateUser(FirebaseUser firebaseUser) {
        String userId = firebaseUser.getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId);
        
        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    // Create new user
                    User newUser = new User(userId);
                    newUser.setDisplayName(firebaseUser.getDisplayName());
                    if (firebaseUser.getPhotoUrl() != null) {
                        newUser.setProfilePicUrl(firebaseUser.getPhotoUrl().toString());
                    }
                    userRef.setValue(newUser)
                            .addOnSuccessListener(aVoid -> startMainActivity())
                            .addOnFailureListener(e -> {
                                Toast.makeText(LoginActivity.this, 
                                        "Failed to create user profile", Toast.LENGTH_SHORT).show();
                                showLoading(false);
                            });
                } else {
                    startMainActivity();
                }
            } else {
                Toast.makeText(LoginActivity.this, 
                        "Failed to check user profile", Toast.LENGTH_SHORT).show();
                showLoading(false);
            }
        });
    }

    private void startAdminActivity() {
        Intent intent = new Intent(this, AdminMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!show);
        adminLoginButton.setEnabled(!show);
        googleSignInButton.setEnabled(!show);
        emailInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
    }

    private void showForgotPasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        EditText emailInput = dialogView.findViewById(R.id.emailInput);
        ProgressBar progressBar = dialogView.findViewById(R.id.progressBar);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Send Reset Link", null)
            .setNegativeButton("Cancel", null)
            .create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String email = emailInput.getText().toString().trim();
                
                if (email.isEmpty()) {
                    emailInput.setError("Email is required");
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                button.setEnabled(false);

                mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this,
                                "Password reset email sent. Check your inbox.",
                                Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } else {
                            button.setEnabled(true);
                            String error = "Failed to send reset email";
                            if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                error = "No account found with this email";
                                emailInput.setError("Invalid email");
                            }
                            Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                        }
                    });
            });
        });

        dialog.show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void handleAdminLogin() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please enter email and password");
            return;
        }

        if (!AdminManager.isAdmin(email)) {
            showError("Invalid admin credentials");
            return;
        }

        showLoading(true);
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                showLoading(false);
                if (task.isSuccessful()) {
                    startAdminActivity();
                } else {
                    showError("Admin login failed: " + task.getException().getMessage());
                }
            });
    }
} 