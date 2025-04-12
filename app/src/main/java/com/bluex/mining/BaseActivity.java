package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bluex.mining.utils.AdManager;

public class BaseActivity extends AppCompatActivity {
    protected AdManager adManager;
    private static final long MIN_CLICK_INTERVAL = 500; // 500ms
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adManager = AdManager.getInstance(this);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }
    }

    // Prevent double clicks
    protected boolean isClickValid() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < MIN_CLICK_INTERVAL) {
            return false;
        }
        lastClickTime = currentTime;
        return true;
    }

    @Override
    public void startActivity(Intent intent) {
        if (!isClickValid()) return;
        
        // Add flags to prevent activity stacking
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        super.startActivity(intent);
        
        // Show ad with reduced frequency
        if (Math.random() < 0.3) { // 30% chance to show ad
            adManager.showInterstitialAd(this);
        }
    }

    protected void showRewardedAd(AdManager.OnRewardEarnedListener listener) {
        if (!isClickValid()) return;
        adManager.showRewardedAd(this, listener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adManager != null) {
            adManager.destroy();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the up navigation properly
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Handle back navigation
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Handle back button press
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
} 