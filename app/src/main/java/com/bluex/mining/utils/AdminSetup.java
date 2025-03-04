package com.bluex.mining.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class AdminSetup {
    public static void setupAdmin() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        // Create admin user
        mAuth.createUserWithEmailAndPassword("bannysukumar@gmail.com", "Banny2255@@##")
            .addOnSuccessListener(authResult -> {
                String uid = authResult.getUser().getUid();
                
                // Add admin data to database
                Map<String, Object> adminData = new HashMap<>();
                adminData.put("email", "bannysukumar@gmail.com");
                adminData.put("role", "admin");
                adminData.put("createdAt", System.currentTimeMillis());
                adminData.put("isActive", true);

                mDatabase.child("admins").child(uid).setValue(adminData);
            });
    }
} 