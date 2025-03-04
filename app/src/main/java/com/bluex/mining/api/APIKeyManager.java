package com.bluex.mining.api;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class APIKeyManager {
    private static final int API_KEY_LENGTH = 32;
    private final DatabaseReference mDatabase;
    private final SecureRandom secureRandom;

    public interface APIKeyCallback {
        void onSuccess(String apiKey);
        void onError(String error);
    }

    public APIKeyManager() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        secureRandom = new SecureRandom();
    }

    public void generateAPIKey(String clientId, String clientSecret, APIKeyCallback callback) {
        validateCredentials(clientId, clientSecret, new APIKeyCallback() {
            @Override
            public void onSuccess(String unused) {
                String apiKey = generateSecureKey();
                storeAPIKey(clientId, apiKey, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void validateCredentials(String clientId, String clientSecret, APIKeyCallback callback) {
        mDatabase.child("clients").child(clientId).addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists() && 
                        clientSecret.equals(snapshot.child("secret").getValue(String.class))) {
                        callback.onSuccess(null);
                    } else {
                        callback.onError("Invalid client credentials");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    callback.onError(error.getMessage());
                }
            });
    }

    private String generateSecureKey() {
        byte[] randomBytes = new byte[API_KEY_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void storeAPIKey(String clientId, String apiKey, APIKeyCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("apiKey", apiKey);
        updates.put("createdAt", System.currentTimeMillis());
        updates.put("lastUsed", System.currentTimeMillis());

        mDatabase.child("api_keys").child(clientId).setValue(updates)
            .addOnSuccessListener(aVoid -> callback.onSuccess(apiKey))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void validateAPIKey(String apiKey, APIKeyCallback callback) {
        mDatabase.child("api_keys").orderByChild("apiKey").equalTo(apiKey)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        updateLastUsed(snapshot.getKey());
                        callback.onSuccess(apiKey);
                    } else {
                        callback.onError("Invalid API key");
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    callback.onError(error.getMessage());
                }
            });
    }

    private void updateLastUsed(String clientId) {
        mDatabase.child("api_keys").child(clientId).child("lastUsed")
            .setValue(System.currentTimeMillis());
    }
} 
 
 
