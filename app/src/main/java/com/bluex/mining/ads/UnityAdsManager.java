package com.bluex.mining.ads;

import android.app.Activity;
import android.content.Context;

import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.ads.UnityAdsShowOptions;

public class UnityAdsManager {
    private static final String GAME_ID = "your_unity_game_id";
    private static final String REWARDED_AD_UNIT_ID = "Rewarded_Android";
    private static final boolean TEST_MODE = true;

    private Context context;
    private boolean isInitialized = false;
    private boolean isLoading = false;

    public interface UnityAdsCallback {
        void onRewardEarned();
        void onAdFailed(String message);
    }

    public UnityAdsManager(Context context) {
        this.context = context;
        initializeAds();
    }

    private void initializeAds() {
        UnityAds.initialize(context, GAME_ID, TEST_MODE, new IUnityAdsInitializationListener() {
            @Override
            public void onInitializationComplete() {
                isInitialized = true;
                loadRewardedAd();
            }

            @Override
            public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                isInitialized = false;
            }
        });
    }

    public void loadRewardedAd() {
        if (!isInitialized || isLoading) return;

        isLoading = true;
        UnityAds.load(REWARDED_AD_UNIT_ID, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {
                isLoading = false;
            }

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {
                isLoading = false;
            }
        });
    }

    public void showRewardedAd(Activity activity, UnityAdsCallback callback) {
        if (!isInitialized) {
            callback.onAdFailed("Unity Ads not initialized");
            return;
        }

        UnityAds.show(activity, REWARDED_AD_UNIT_ID, new UnityAdsShowOptions(), new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    callback.onRewardEarned();
                }
                loadRewardedAd(); // Preload next ad
            }

            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                callback.onAdFailed(message);
                loadRewardedAd(); // Try to load again
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {}

            @Override
            public void onUnityAdsShowClick(String placementId) {}
        });
    }
} 