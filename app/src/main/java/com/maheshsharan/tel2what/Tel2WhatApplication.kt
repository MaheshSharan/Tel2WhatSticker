package com.maheshsharan.tel2what

import android.app.Application
import android.os.StrictMode
import android.util.Log

class Tel2WhatApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        if (BuildConfig.DEBUG) {
            setupStrictMode()
        }
    }

    private fun setupStrictMode() {
        Log.w("Tel2What:Validation", "🔥 Strict Mode Enabled for Media & JNI Profiling")

        // Track UI thread blocks & latency
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls() // Native encoding shouldn't block main
                .penaltyLog()
                .penaltyFlashScreen() // Obvious visual indicator on dev builds
                .build()
        )

        // Track Memory Boundaries, IO handles and Native Object lifecycles
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()      // Specifically looking for MediaCodec/Surface/FileDescriptors
                .detectActivityLeaks()
                .detectLeakedRegistrationObjects()
                .detectCleartextNetwork()
                .penaltyLog()
                // .penaltyDeath() // We'll disable death until the stress test finishes otherwise it halts the batch
                .build()
        )
    }
}
