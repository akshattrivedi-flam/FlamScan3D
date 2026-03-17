package com.yourorg.objectcapture.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourorg.objectcapture.core.AppViewModel

@Composable
fun ReconstructionScreen(viewModel: AppViewModel) {
    val uploadState by viewModel.uploadState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Reconstruction", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Upload your capture to the server for reconstruction. We'll poll progress and open the web viewer when ready.",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = { viewModel.startUpload() },
            enabled = !uploadState.inProgress,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(if (uploadState.inProgress) "Uploading..." else "Process on Server")
        }

        if (uploadState.inProgress || uploadState.progress > 0) {
            LinearProgressIndicator(
                progress = { (uploadState.progress / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Text("Progress: ${uploadState.progress}%", style = MaterialTheme.typography.bodyMedium)
        }

        if (uploadState.isCompleted) {
            Button(onClick = { viewModel.setViewingModel() }, modifier = Modifier.padding(top = 16.dp)) {
                Text("Open Web Viewer")
            }
        }

        if (!uploadState.error.isNullOrEmpty()) {
            Text(
                text = uploadState.error ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
