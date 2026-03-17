package com.yourorg.objectcapture.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourorg.objectcapture.core.AppViewModel
import com.yourorg.objectcapture.model.CaptureState

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val viewModel: AppViewModel = hiltViewModel()
    val state by viewModel.captureState.collectAsState()

    LaunchedEffect(state) {
        val route = when (state) {
            CaptureState.READY, CaptureState.CAPTURING -> "capture"
            CaptureState.REVIEWING -> "review"
            CaptureState.PREPARE_RECONSTRUCTION, CaptureState.RECONSTRUCTING -> "reconstruction"
            CaptureState.VIEWING_MODEL -> "viewer"
            CaptureState.COMPLETED -> "capture"
            CaptureState.FAILED -> "capture"
        }
        navController.navigate(route) {
            popUpTo(0)
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = "capture") {
        composable("capture") { CaptureScreen(viewModel) }
        composable("review") { ReviewScreen(viewModel) }
        composable("reconstruction") { ReconstructionScreen(viewModel) }
        composable("viewer") { ViewerScreen(viewModel) }
    }
}
