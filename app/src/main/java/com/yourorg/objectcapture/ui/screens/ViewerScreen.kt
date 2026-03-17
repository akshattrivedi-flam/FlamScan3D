package com.yourorg.objectcapture.ui.screens

import android.content.Intent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yourorg.objectcapture.ArPlacementActivity
import com.yourorg.objectcapture.BuildConfig
import com.yourorg.objectcapture.core.AppViewModel
import com.yourorg.objectcapture.ui.viewer.FilamentViewer
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun ViewerScreen(viewModel: AppViewModel) {
    val session = viewModel.getCurrentSession()
    val modelFile = remember(session) { findModelFile(session?.modelsDir) }
    val uploadState by viewModel.uploadState.collectAsState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Viewer", style = MaterialTheme.typography.headlineSmall)

        val modelUrl = uploadState.modelUrl
        if (!modelUrl.isNullOrBlank()) {
            val viewerUrl = buildViewerUrl(modelUrl)
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webViewClient = WebViewClient()
                        loadUrl(viewerUrl)
                    }
                },
                update = { view -> view.loadUrl(viewerUrl) },
                modifier = Modifier.weight(1f)
            )
        } else if (modelFile != null && isSupported(modelFile)) {
            FilamentViewer(modelFile = modelFile, modifier = Modifier.weight(1f))
        } else {
            Text(
                "No model found yet. Upload a capture to the server or place a GLB/GLTF in Models/.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }

        if (modelFile != null && isSupported(modelFile)) {
            Button(onClick = {
                val intent = Intent(context, ArPlacementActivity::class.java)
                intent.putExtra(ArPlacementActivity.EXTRA_MODEL_PATH, modelFile.absolutePath)
                context.startActivity(intent)
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("AR Placement")
            }
        }

        Button(onClick = { viewModel.complete() }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Done")
        }
    }
}

private fun findModelFile(dir: File?): File? {
    val files = dir?.listFiles()?.sortedBy { it.name } ?: return null
    return files.firstOrNull { isSupported(it) } ?: files.firstOrNull()
}

private fun isSupported(file: File): Boolean {
    val ext = file.extension.lowercase()
    return ext == "glb" || ext == "gltf"
}

private fun buildViewerUrl(modelUrl: String): String {
    val encoded = URLEncoder.encode(modelUrl, StandardCharsets.UTF_8.toString())
    return "${BuildConfig.BASE_URL}/viewer?model_url=$encoded"
}
