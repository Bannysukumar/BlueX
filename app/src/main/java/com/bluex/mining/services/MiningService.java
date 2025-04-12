package com.bluex.mining.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.bluex.mining.MainActivity;
import com.bluex.mining.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class MiningService extends Service {
    private static final String TAG = "MiningService";
    private static final String CHANNEL_ID = "MiningServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long MINING_DURATION = 3600000; // 1 hour in milliseconds

    private Handler miningHandler;
    private boolean isMining = false;
    private long startTime;
    private DatabaseReference mDatabase;
    private String userId;
    private double miningRate = 0.00001; // BXC per second

    @Override
    public void onCreate() {
        super.onCreate();
        miningHandler = new Handler(Looper.getMainLooper());
        mDatabase = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startMining();
        return START_STICKY;
    }

    private void startMining() {
        try {
            // Create notification
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this,
                    0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Mining in Progress")
                    .setContentText("Your device is currently mining")
                    .setSmallIcon(R.drawable.mining_icon)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            // Start foreground service
            startForeground(NOTIFICATION_ID, notification);
            
            // Start mining process
            // Add your mining logic here
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting mining service", e);
            stopSelf();
        }
    }

    private void completeMining() {
        isMining = false;
        
        // Update notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createCompletionNotification());
        
        // Stop service
        stopSelf();
    }

    private Notification createMiningNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BlueX Mining")
                .setContentText("Mining in progress...")
                .setSmallIcon(R.drawable.mining_icon_active)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private Notification createCompletionNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BlueX Mining")
                .setContentText("Mining complete! +0.0001 BTC")
                .setSmallIcon(R.drawable.mining_icon)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Mining Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("Channel for mining service notifications");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (miningHandler != null) {
            miningHandler.removeCallbacksAndMessages(null);
        }
        stopForeground(true);
    }
} 