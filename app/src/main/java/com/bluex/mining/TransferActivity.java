package com.bluex.mining;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.bluex.mining.models.User;

import java.util.regex.Pattern;

public class TransferActivity extends AppCompatActivity {

    private EditText emailInput, amountInput;
    private Button transferButton;
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        emailInput = findViewById(R.id.emailInput);
        amountInput = findViewById(R.id.amountInput);
        transferButton = findViewById(R.id.transferButton);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Retrieve current user data
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            mDatabase.child("users").child(userId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    currentUser = task.getResult().getValue(User.class);
                }
            });
        }

        transferButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();

            // Validate inputs
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(amountStr)) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate email format
            if (!isValidEmail(email)) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
                return;
            }

            // Proceed with transfer logic
            double amount = Double.parseDouble(amountStr);
            transferFunds(email, amount);
        });
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return Pattern.matches(emailPattern, email);
    }

    private void transferFunds(String email, double amount) {
        // Check if mobile number is set
        if (TextUtils.isEmpty(currentUser.getPhoneNumber())) {
            Toast.makeText(this, "Mobile number is required to transfer funds.", Toast.LENGTH_SHORT).show();
            return;
        }

        double adminCharge = amount * 0.10; // Calculate 10% admin charge
        double totalAmount = amount + adminCharge; // Total amount to deduct from sender

        String userId = mAuth.getCurrentUser().getUid();

        // Check sender's balance
        mDatabase.child("users").child(userId).child("balance").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                double currentBalance = task.getResult().getValue(Double.class);
                if (currentBalance >= totalAmount) {
                    // Find recipientId by email
                    mDatabase.child("users").orderByChild("email").equalTo(email).get().addOnCompleteListener(recipientTask -> {
                        if (recipientTask.isSuccessful() && recipientTask.getResult().exists()) {
                            String recipientId = recipientTask.getResult().getChildren().iterator().next().getKey(); // Get the first matching user ID

                            // Update sender's balance
                            mDatabase.child("users").child(userId).child("balance").setValue(currentBalance - totalAmount);

                            // Update recipient's balance
                            mDatabase.child("users").child(recipientId).child("balance").get().addOnCompleteListener(recipientBalanceTask -> {
                                if (recipientBalanceTask.isSuccessful()) {
                                    double recipientBalance = recipientBalanceTask.getResult().getValue(Double.class);
                                    mDatabase.child("users").child(recipientId).child("balance").setValue(recipientBalance + amount); // Recipient gets the original amount
                                }
                            });

                            Toast.makeText(this, "Transfer successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Recipient not found", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, "Insufficient balance after admin charges", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
} 