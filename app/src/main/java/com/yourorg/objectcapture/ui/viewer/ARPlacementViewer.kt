package com.yourorg.objectcapture.ui.viewer

import android.content.Context
import android.view.Choreographer
import android.view.SurfaceView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.Camera
import com.google.android.filament.utils.ModelViewer
import com.google.ar.core.Pose
import com.yourorg.objectcapture.ar.ARCoreManager
import kotlin.math.atan

@Composable
fun ARPlacementViewer(
    arCoreManager: ARCoreManager,
    modelPath: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val controller = remember { ARPlacementController(context, arCoreManager) }

    AndroidView(
        factory = { controller.surfaceView },
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                controller.onTap(offset.x, offset.y)
            }
        }
    )

    LaunchedEffect(modelPath) {
        modelPath?.let { controller.loadModel(it) }
    }

    DisposableEffect(Unit) {
        controller.startRendering()
        onDispose { controller.stopRendering() }
    }
}

class ARPlacementController(
    context: Context,
    private val arCoreManager: ARCoreManager
) {
    val surfaceView: SurfaceView = SurfaceView(context)
    private val modelViewer = ModelViewer(surfaceView)
    private val choreographer = Choreographer.getInstance()
    private var anchorPose: Pose? = null

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            val frame = arCoreManager.update()
            if (frame != null) {
                updateCamera(frame)
                updateAnchorTransform()
            }
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

    fun loadModel(path: String) {
        val file = java.io.File(path)
        if (!file.exists()) return
        val buffer = java.nio.ByteBuffer.wrap(file.readBytes())
        when (file.extension.lowercase()) {
            "glb" -> modelViewer.loadModelGlb(buffer)
            "gltf" -> modelViewer.loadModelGltf(buffer)
            else -> return
        }
        modelViewer.transformToUnitCube()
    }

    fun onTap(x: Float, y: Float) {
        val pose = arCoreManager.hitTest(x, y, surfaceView.width, surfaceView.height)
        if (pose != null) {
            anchorPose = pose
        }
    }

    private fun updateCamera(frame: com.google.ar.core.Frame) {
        val view = FloatArray(16)
        val proj = FloatArray(16)
        val near = 0.1f
        val far = 100.0f
        frame.camera.getViewMatrix(view, 0)
        frame.camera.getProjectionMatrix(proj, 0, near, far)

        val aspect = if (surfaceView.height > 0) surfaceView.width.toDouble() / surfaceView.height.toDouble() else 1.0
        val f = proj[5].toDouble()
        val fov = Math.toDegrees(2.0 * atan(1.0 / f))

        modelViewer.camera.setProjection(fov, aspect, near.toDouble(), far.toDouble(), Camera.Fov.VERTICAL)
        val cameraModel = invertRigid(view)
        modelViewer.camera.setModelMatrix(cameraModel)
    }

    private fun updateAnchorTransform() {
        val pose = anchorPose ?: return
        val matrix = FloatArray(16)
        pose.toMatrix(matrix, 0)
        val asset = modelViewer.asset ?: return
        val tm = modelViewer.engine.transformManager
        val instance = tm.getInstance(asset.root)
        tm.setTransform(instance, matrix)
    }

    private fun invertRigid(view: FloatArray): DoubleArray {
        val r00 = view[0]; val r01 = view[4]; val r02 = view[8]
        val r10 = view[1]; val r11 = view[5]; val r12 = view[9]
        val r20 = view[2]; val r21 = view[6]; val r22 = view[10]
        val tx = view[12]; val ty = view[13]; val tz = view[14]

        val invTx = -(r00 * tx + r10 * ty + r20 * tz)
        val invTy = -(r01 * tx + r11 * ty + r21 * tz)
        val invTz = -(r02 * tx + r12 * ty + r22 * tz)

        return doubleArrayOf(
            r00.toDouble(), r01.toDouble(), r02.toDouble(), 0.0,
            r10.toDouble(), r11.toDouble(), r12.toDouble(), 0.0,
            r20.toDouble(), r21.toDouble(), r22.toDouble(), 0.0,
            invTx.toDouble(), invTy.toDouble(), invTz.toDouble(), 1.0
        )
    }
}
