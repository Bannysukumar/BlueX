package com.bluex.mining.admin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bluex.mining.R;
import com.bluex.mining.api.ClientManager;
import com.bluex.mining.api.APIKeyManager;

public class APIManagementActivity extends AppCompatActivity {
    private ClientManager clientManager;
    private APIKeyManager apiKeyManager;
    private EditText clientNameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_management);

        clientManager = new ClientManager();
        apiKeyManager = new APIKeyManager();
        clientNameInput = findViewById(R.id.clientNameInput);

        Button registerButton = findViewById(R.id.registerClientButton);
        registerButton.setOnClickListener(v -> registerNewClient());
    }

    private void registerNewClient() {
        String clientName = clientNameInput.getText().toString();
        if (clientName.isEmpty()) {
            clientNameInput.setError("Client name required");
            return;
        }

        clientManager.registerClient(clientName, new ClientManager.ClientCallback() {
            @Override
            public void onSuccess(String clientId, String clientSecret) {
                generateAPIKey(clientId, clientSecret);
            }

            @Override
            public void onError(String error) {
                Toast.makeText(APIManagementActivity.this, 
                    "Error: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void generateAPIKey(String clientId, String clientSecret) {
        apiKeyManager.generateAPIKey(clientId, clientSecret, 
            new APIKeyManager.APIKeyCallback() {
                @Override
                public void onSuccess(String apiKey) {
                    showCredentials(clientId, clientSecret, apiKey);
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(APIManagementActivity.this, 
                        "Error: " + error, Toast.LENGTH_LONG).show();
                }
            });
    }

    private void showCredentials(String clientId, String clientSecret, String apiKey) {
        new AlertDialog.Builder(this)
            .setTitle("API Credentials")
            .setMessage("Client ID: " + clientId + "\n\n" +
                       "Client Secret: " + clientSecret + "\n\n" +
                       "API Key: " + apiKey)
            .setPositiveButton("Copy", (dialog, which) -> {
                // Copy to clipboard
                ClipboardManager clipboard = (ClipboardManager) 
                    getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("API Credentials", 
                    "Client ID: " + clientId + "\n" +
                    "Client Secret: " + clientSecret + "\n" +
                    "API Key: " + apiKey);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null)
            .show();
    }
} 