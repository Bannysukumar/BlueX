package com.bluex.mining.api;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientManager {
    private final DatabaseReference mDatabase;

    public interface ClientCallback {
        void onSuccess(String clientId, String clientSecret);
        void onError(String error);
    }

    public ClientManager() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    public void registerClient(String clientName, ClientCallback callback) {
        String clientId = UUID.randomUUID().toString();
        String clientSecret = UUID.randomUUID().toString();

        Map<String, Object> clientData = new HashMap<>();
        clientData.put("name", clientName);
        clientData.put("secret", clientSecret);
        clientData.put("status", "active");
        clientData.put("createdAt", System.currentTimeMillis());

        mDatabase.child("clients").child(clientId).setValue(clientData)
            .addOnSuccessListener(aVoid -> callback.onSuccess(clientId, clientSecret))
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
} 