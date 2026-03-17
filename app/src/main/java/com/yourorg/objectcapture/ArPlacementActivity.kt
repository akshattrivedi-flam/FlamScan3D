package com.yourorg.objectcapture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourorg.objectcapture.ar.ARCoreManager
import com.yourorg.objectcapture.ui.theme.ObjectCaptureTheme
import com.yourorg.objectcapture.ui.viewer.ARPlacementViewer
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ArPlacementActivity : ComponentActivity() {
    @Inject lateinit var arCoreManager: ARCoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modelPath = intent.getStringExtra(EXTRA_MODEL_PATH)
        setContent {
            ObjectCaptureTheme(darkTheme = false) {
                ArPlacementScreen(arCoreManager = arCoreManager, modelPath = modelPath) {
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        arCoreManager.start()
    }

    override fun onPause() {
        arCoreManager.stop()
        super.onPause()
    }

    companion object {
        const val EXTRA_MODEL_PATH = "model_path"
    }
}

@Composable
private fun ArPlacementScreen(
    arCoreManager: ARCoreManager,
    modelPath: String?,
    onDone: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text("AR Placement", style = MaterialTheme.typography.headlineSmall)
        Text("Tap to place the model in the world", style = MaterialTheme.typography.bodyMedium)

        ARPlacementViewer(arCoreManager = arCoreManager, modelPath = modelPath, modifier = Modifier.weight(1f))

        Button(onClick = onDone, modifier = Modifier.padding(top = 8.dp)) {
            Text("Done")
        }
    }
}
