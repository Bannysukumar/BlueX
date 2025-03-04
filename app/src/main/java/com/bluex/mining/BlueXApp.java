package com.bluex.mining;

import android.app.Application;
import com.google.firebase.database.FirebaseDatabase;

public class BlueXApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Enable Firebase offline persistence
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        
        // Set cache size
        FirebaseDatabase.getInstance().getReference().keepSynced(true);
    }
} 