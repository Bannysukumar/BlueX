package com.bluex.mining.services;

import android.app.*;
import android.content.Intent;
import android.os.*;
import androidx.core.app.NotificationCompat;
import com.bluex.mining.R;
import com.bluex.mining.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class MiningService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MiningServiceChannel";
    private static final long MINING_INTERVAL = 1000; // 1 second

    private boolean isMining = false;
    private DatabaseReference mDatabase;
    private String userId;
    private Handler handler;
    private long startTime;
    private double miningRate = 0.00001; // BXC per second

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "START_MINING":
                    startMining();
                    break;
                case "STOP_MINING":
                    stopMining();
                    break;
            }
        }
        return START_STICKY;
    }

    private void startMining() {
        if (!isMining) {
            isMining = true;
            startTime = System.currentTimeMillis();
            startForeground(NOTIFICATION_ID, createNotification());
            updateMiningStatus(true);
            handler.postDelayed(miningRunnable, MINING_INTERVAL);
        }
    }

    private void stopMining() {
        isMining = false;
        handler.removeCallbacks(miningRunnable);
        updateMiningStatus(false);
        stopForeground(true);
        stopSelf();
    }

    private final Runnable miningRunnable = new Runnable() {
        @Override
        public void run() {
            if (isMining) {
                updateBalance();
                updateNotification();
                handler.postDelayed(this, MINING_INTERVAL);
            }
        }
    };

    private void updateBalance() {
        mDatabase.child("users").child(userId).runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if (mutableData.getValue() == null) return Transaction.success(mutableData);
                
                try {
                    double currentBalance = mutableData.child("balance").getValue(Double.class);
                    mutableData.child("balance").setValue(currentBalance + miningRate);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {}
        });
    }

    private void updateMiningStatus(boolean isActive) {
        mDatabase.child("users").child(userId).child("isMining").setValue(isActive);
        if (isActive) {
            mDatabase.child("users").child(userId).child("miningStartTime")
                .setValue(ServerValue.TIMESTAMP);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 
            PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mining BXC")
            .setContentText("Mining in progress...")
            .setSmallIcon(R.drawable.ic_mine)
            .setContentIntent(pendingIntent)
            .build();
    }

    private void updateNotification() {
        long duration = System.currentTimeMillis() - startTime;
        String durationStr = String.format("%02d:%02d:%02d", 
            duration / 3600000, // hours
            (duration % 3600000) / 60000, // minutes
            (duration % 60000) / 1000); // seconds

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Mining BXC")
            .setContentText("Mining time: " + durationStr)
            .setSmallIcon(R.drawable.ic_mine)
            .build();

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Mining Service Channel",
                NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 