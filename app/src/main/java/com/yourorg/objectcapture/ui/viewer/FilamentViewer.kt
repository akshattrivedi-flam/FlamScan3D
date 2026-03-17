package com.yourorg.objectcapture.ui.viewer

import android.content.Context
import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.utils.ModelViewer
import java.io.File
import java.nio.ByteBuffer

@Composable
fun FilamentViewer(
    modelFile: File?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember { FilamentViewerController(context) }

    AndroidView(factory = { controller.surfaceView }, modifier = modifier)

    LaunchedEffect(modelFile) {
        modelFile?.let { controller.loadModel(it) }
    }

    DisposableEffect(Unit) {
        controller.startRendering()
        onDispose { controller.stopRendering() }
    }
}

class FilamentViewerController(context: Context) {
    val surfaceView: SurfaceView = SurfaceView(context)
    private val modelViewer = ModelViewer(surfaceView)
    private val choreographer = Choreographer.getInstance()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            modelViewer.render(frameTimeNanos)
            choreographer.postFrameCallback(this)
        }
    }

    fun startRendering() {
        choreographer.postFrameCallback(frameCallback)
    }

    fun stopRendering() {
        choreographer.removeFrameCallback(frameCallback)
    }

    fun loadModel(file: File) {
        if (!file.exists()) return
        val extension = file.extension.lowercase()
        val data = file.readBytes()
        val buffer = ByteBuffer.wrap(data)
        when (extension) {
            "glb" -> modelViewer.loadModelGlb(buffer)
            "gltf" -> {
                val baseDir = file.parentFile
                modelViewer.loadModelGltf(buffer) { uri ->
                    val assetFile = if (baseDir != null) File(baseDir, uri) else File(uri)
                    ByteBuffer.wrap(assetFile.readBytes())
                }
            }
            else -> return
        }
        modelViewer.transformToUnitCube()
    }
}
