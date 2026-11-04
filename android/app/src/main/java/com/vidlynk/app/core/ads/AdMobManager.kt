package com.vidlynk.app.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor() {

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    // Google AdMob Test Interstitial ID (replace with production ID in release)
    private val interstitialTestUnitId = "ca-app-pub-3940256099942544/1033173712"

    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            interstitialTestUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    Log.d("AdMobManager", "Interstitial ad loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    Log.w("AdMobManager", "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdDismissedOrSkipped: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd(activity) // Pre-load next ad
                    onAdDismissedOrSkipped()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdDismissedOrSkipped()
                }
            }
            ad.show(activity)
        } else {
            // No ad ready, proceed immediately
            loadInterstitialAd(activity)
            onAdDismissedOrSkipped()
        }
    }
}
