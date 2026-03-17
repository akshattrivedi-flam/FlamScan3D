package com.yourorg.objectcapture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.yourorg.objectcapture.ui.screens.AppNavHost
import com.yourorg.objectcapture.ui.theme.ObjectCaptureTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ObjectCaptureTheme(darkTheme = isSystemInDarkTheme()) {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    AppNavHost()
}
