package com.vidlynk.app.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.vidlynk.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor() {

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var pendingShowRequest: ((InterstitialAd?) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val interstitialUnitId = BuildConfig.ADMOB_INTERSTITIAL_ID

    fun loadInterstitialAd(context: Context) {
        if (interstitialAd != null || isAdLoading) return

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            interstitialUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    Log.d("AdMobManager", "Interstitial ad loaded successfully")

                    // If a show request was waiting for this ad, trigger it immediately
                    pendingShowRequest?.let { callback ->
                        pendingShowRequest = null
                        callback(ad)
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    Log.w("AdMobManager", "Interstitial ad failed to load: ${error.message}")

                    pendingShowRequest?.let { callback ->
                        pendingShowRequest = null
                        callback(null)
                    }
                }
            }
        )
    }

    /**
     * Shows fullscreen interstitial ad before video playback starts.
     * If the ad is already loaded, it displays immediately.
     * If the ad is currently loading (e.g. user quickly tapped from Home), it waits up to 2.5s for completion.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissedOrSkipped: () -> Unit) {
        val readyAd = interstitialAd

        if (readyAd != null) {
            displayLoadedAd(activity, readyAd, onAdDismissedOrSkipped)
            return
        }

        if (isAdLoading) {
            // Wait up to 2500ms for in-flight ad load
            var hasHandled = false
            val timeoutRunnable = Runnable {
                if (!hasHandled) {
                    hasHandled = true
                    pendingShowRequest = null
                    loadInterstitialAd(activity)
                    onAdDismissedOrSkipped()
                }
            }
            mainHandler.postDelayed(timeoutRunnable, 2500)

            pendingShowRequest = { loadedAd ->
                if (!hasHandled) {
                    hasHandled = true
                    mainHandler.removeCallbacks(timeoutRunnable)
                    if (loadedAd != null) {
                        displayLoadedAd(activity, loadedAd, onAdDismissedOrSkipped)
                    } else {
                        onAdDismissedOrSkipped()
                    }
                }
            }
        } else {
            // Start loading for next time and continue
            loadInterstitialAd(activity)
            onAdDismissedOrSkipped()
        }
    }

    private fun displayLoadedAd(
        activity: Activity,
        ad: InterstitialAd,
        onAdDismissedOrSkipped: () -> Unit
    ) {
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
    }
}
