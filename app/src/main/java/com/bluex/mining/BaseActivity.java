package com.bluex.mining;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import com.bluex.mining.utils.AdManager;

public class BaseActivity extends AppCompatActivity {
    protected AdManager adManager;
    private static final long MIN_CLICK_INTERVAL = 500; // 500ms
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adManager = AdManager.getInstance(this);
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
    public void onBackPressed() {
        // Add any cleanup needed before navigating back
        if (adManager != null) {
            adManager.destroy();
        }
        super.onBackPressed();
    }
} 