package com.bluex.mining.utils;

import android.app.Activity;
import android.content.Context;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.unity3d.ads.IUnityAdsLoadListener;
import com.unity3d.ads.IUnityAdsShowListener;
import com.unity3d.ads.UnityAds;

public class AdManager {
    private static AdManager instance;
    private static final String ADMOB_BANNER_ID = "ca-app-pub-4029860754779720/2376300180";
    private static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-4029860754779720/9750136846";
    private static final String ADMOB_REWARDED_ID = "ca-app-pub-4029860754779720/8437055171";
    private static final String UNITY_GAME_ID = "5495131";
    private static final String UNITY_REWARDED_ID = "Rewarded_Android";
    private static final boolean TEST_MODE = false;

    private Context context;
    private InterstitialAd mInterstitialAd;
    private RewardedAd mRewardedAd;
    private boolean isRewardedAdLoading = false;
    private boolean isDestroyed = false;

    private AdManager(Context context) {
        this.context = context;
        initializeAds();
    }

    public static AdManager getInstance(Context context) {
        if (instance == null || instance.isDestroyed) {
            instance = new AdManager(context);
        }
        return instance;
    }

    private void initializeAds() {
        // Initialize AdMob
        MobileAds.initialize(context, initializationStatus -> {
            loadInterstitialAd();
            loadRewardedAd();
        });

        // Initialize Unity Ads
        UnityAds.initialize(context, UNITY_GAME_ID, TEST_MODE);
    }

    public void showBannerAd(LinearLayout adContainer) {
        if (context == null || isDestroyed) return;

        AdView adView = new AdView(context);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(ADMOB_BANNER_ID);
        adContainer.addView(adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    public void showInterstitialAd(Activity activity) {
        if (mInterstitialAd != null && !isDestroyed) {
            mInterstitialAd.show(activity);
        } else {
            loadInterstitialAd();
        }
    }

    private void loadInterstitialAd() {
        if (context == null || isDestroyed) return;

        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, ADMOB_INTERSTITIAL_ID, adRequest, 
            new InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    mInterstitialAd = interstitialAd;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mInterstitialAd = null;
                }
            });
    }

    public void showRewardedAd(Activity activity, OnRewardEarnedListener listener) {
        if (mRewardedAd != null) {
            mRewardedAd.show(activity, rewardItem -> {
                if (listener != null) {
                    listener.onRewardEarned(rewardItem.getAmount());
                }
                loadRewardedAd();
            });
        } else {
            // Unity Ads doesn't have isReady() in newer versions
            // Just try to show directly
            showUnityRewardedAd(activity, listener);
            loadRewardedAd(); // Load next ad
        }
    }

    private void loadRewardedAd() {
        if (context == null || isDestroyed || isRewardedAdLoading) return;

        isRewardedAdLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, ADMOB_REWARDED_ID, adRequest, 
            new RewardedAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                    mRewardedAd = rewardedAd;
                    isRewardedAdLoading = false;
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    mRewardedAd = null;
                    isRewardedAdLoading = false;
                    loadUnityRewardedAd();
                }
            });
    }

    private void showUnityRewardedAd(Activity activity, OnRewardEarnedListener listener) {
        UnityAds.show(activity, UNITY_REWARDED_ID, new IUnityAdsShowListener() {
            @Override
            public void onUnityAdsShowComplete(String placementId, UnityAds.UnityAdsShowCompletionState state) {
                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                    if (listener != null) {
                        listener.onRewardEarned(1);
                    }
                }
                loadUnityRewardedAd();
            }

            @Override
            public void onUnityAdsShowFailure(String placementId, UnityAds.UnityAdsShowError error, String message) {
                loadUnityRewardedAd();
            }

            @Override
            public void onUnityAdsShowStart(String placementId) {}

            @Override
            public void onUnityAdsShowClick(String placementId) {}
        });
    }

    private void loadUnityRewardedAd() {
        UnityAds.load(UNITY_REWARDED_ID, new IUnityAdsLoadListener() {
            @Override
            public void onUnityAdsAdLoaded(String placementId) {}

            @Override
            public void onUnityAdsFailedToLoad(String placementId, UnityAds.UnityAdsLoadError error, String message) {}
        });
    }

    public void destroy() {
        if (isDestroyed) return;
        
        isDestroyed = true;
        mInterstitialAd = null;
        mRewardedAd = null;
        instance = null;
    }

    public interface OnRewardEarnedListener {
        void onRewardEarned(int amount);
    }
} 