package com.vidlynk.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VidLynkApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Google Mobile Ads SDK asynchronously
        MobileAds.initialize(this) {}
    }
}
