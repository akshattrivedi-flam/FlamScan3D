package com.yourorg.objectcapture

import android.app.Application
import com.google.android.filament.Filament
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ObjectCaptureApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Filament.init()
    }
}
