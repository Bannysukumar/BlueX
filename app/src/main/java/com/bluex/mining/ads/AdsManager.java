package com.bluex.mining.ads;

import android.app.Activity;
import android.content.Context;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdsManager {
    private static final String TAG = "AdsManager";
    
    // Replace with your actual ad unit IDs
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-4029860754779720/3822866955";
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-4029860754779720/1803208279";

    private Context context;
    private AdView bannerAd;
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private boolean isInterstitialAdLoading = false;
    private boolean isRewardedAdLoading = false;
    private UnityAdsManager unityAdsManager;
    private int consecutiveAdMobFailures = 0;
    private static final int MAX_ADMOB_FAILURES = 3;

    public interface RewardCallback {
        void onRewardEarned(int rewardAmount);
    }

    public AdsManager(Context context) {
        this.context = context;
        
        // Initialize AdMob
        MobileAds.initialize(context, initializationStatus -> {
            loadInterstitialAd();
            loadRewardedAd();
        });

        // Initialize Unity Ads
        unityAdsManager = new UnityAdsManager(context);
    }

    public void setupBannerAd(LinearLayout adContainer) {
        if (bannerAd != null) {
            adContainer.removeView(bannerAd);
            bannerAd.destroy();
        }

        bannerAd = new AdView(context);
        bannerAd.setAdSize(AdSize.BANNER);
        bannerAd.setAdUnitId(BANNER_AD_UNIT_ID);
        adContainer.addView(bannerAd);

        AdRequest adRequest = new AdRequest.Builder().build();
        bannerAd.loadAd(adRequest);
    }

    public void loadInterstitialAd() {
        if (interstitialAd != null || isInterstitialAdLoading) return;

        isInterstitialAdLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialAd = ad;
                        isInterstitialAdLoading = false;
                        setupInterstitialCallbacks();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        interstitialAd = null;
                        isInterstitialAdLoading = false;
                    }
                });
    }

    private void setupInterstitialCallbacks() {
        if (interstitialAd == null) return;

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                interstitialAd = null;
                loadInterstitialAd(); // Load the next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                interstitialAd = null;
            }
        });
    }

    public void showInterstitialAd(Activity activity) {
        if (interstitialAd != null) {
            interstitialAd.show(activity);
        } else if (!isInterstitialAdLoading) {
            loadInterstitialAd();
        }
    }

    public void loadRewardedAd() {
        if (rewardedAd != null || isRewardedAdLoading) return;

        isRewardedAdLoading = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        isRewardedAdLoading = false;
                        setupRewardedCallbacks();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        rewardedAd = null;
                        isRewardedAdLoading = false;
                        consecutiveAdMobFailures++;
                    }
                });
    }

    private void setupRewardedCallbacks() {
        if (rewardedAd == null) return;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                loadRewardedAd(); // Load the next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                rewardedAd = null;
            }
        });
    }

    public void showRewardedAd(Activity activity, RewardCallback callback) {
        // Try AdMob first
        if (rewardedAd != null && consecutiveAdMobFailures < MAX_ADMOB_FAILURES) {
            rewardedAd.show(activity, rewardItem -> {
                consecutiveAdMobFailures = 0;
                callback.onRewardEarned(rewardItem.getAmount());
            });
        } else {
            // Fallback to Unity Ads
            unityAdsManager.showRewardedAd(activity, new UnityAdsManager.UnityAdsCallback() {
                @Override
                public void onRewardEarned() {
                    callback.onRewardEarned(1); // Default reward amount for Unity Ads
                }

                @Override
                public void onAdFailed(String message) {
                    // If both ad networks fail, show message to user
                    Toast.makeText(context, "No ads available at the moment", 
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Preload next ad
        if (!isRewardedAdLoading) {
            loadRewardedAd();
        }
    }

    public void destroy() {
        if (bannerAd != null) {
            bannerAd.destroy();
            bannerAd = null;
        }
    }
} 