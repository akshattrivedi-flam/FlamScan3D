package com.yourorg.objectcapture.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourorg.objectcapture.core.AppViewModel

@Composable
fun ReconstructionScreen(viewModel: AppViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Reconstruction", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Exported COLMAP files are ready. Run the provided script on a workstation to reconstruct, then load the mesh in the viewer.",
            style = MaterialTheme.typography.bodyMedium
        )
        LinearProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
        Button(onClick = { viewModel.setViewingModel() }) {
            Text("Open Viewer")
        }
    }
}
