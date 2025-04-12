package com.bluex.mining;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdminSettingsActivity extends AppCompatActivity {
    private EditText adminChargeInput;
    private Button saveChargeButton;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        adminChargeInput = findViewById(R.id.adminChargeInput);
        saveChargeButton = findViewById(R.id.saveChargeButton);
        mDatabase = FirebaseDatabase.getInstance().getReference("settings");

        saveChargeButton.setOnClickListener(v -> saveAdminCharge());
    }

    private void saveAdminCharge() {
        String chargeStr = adminChargeInput.getText().toString().trim();
        if (TextUtils.isEmpty(chargeStr)) {
            Toast.makeText(this, "Please enter a charge percentage", Toast.LENGTH_SHORT).show();
            return;
        }

        double charge = Double.parseDouble(chargeStr);
        if (charge < 0 || charge > 100) {
            Toast.makeText(this, "Charge must be between 0 and 100", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("adminCharge").setValue(charge)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Admin charge updated successfully!", Toast.LENGTH_SHORT).show();
                adminChargeInput.setText("");
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to update charge: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
} 