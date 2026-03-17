package com.yourorg.objectcapture.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.yourorg.objectcapture.ar.PoseUtils
import com.yourorg.objectcapture.colmap.CameraIntrinsics
import com.yourorg.objectcapture.colmap.ColmapImage
import com.yourorg.objectcapture.core.AppViewModel
import com.yourorg.objectcapture.di.CaptureEntryPoint
import com.yourorg.objectcapture.storage.ZipUtils
import dagger.hilt.android.EntryPointAccessors
import java.io.File

@Composable
fun ReviewScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(context, CaptureEntryPoint::class.java)
    val exporter = entryPoint.colmapExporter()
    val metadataReader = entryPoint.metadataReader()
    val intrinsicsProvider = entryPoint.intrinsicsProvider()

    val session = viewModel.getCurrentSession()
    var images by remember(session) {
        mutableStateOf(session?.imagesDir?.listFiles()?.sortedBy { it.name } ?: emptyList())
    }
    var metadata by remember(session) {
        mutableStateOf(session?.metadataDir?.let { metadataReader.readAll(it) } ?: emptyList())
    }
    var selected by remember { mutableStateOf(setOf<String>()) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Review", style = MaterialTheme.typography.headlineSmall)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(images) { file ->
                val isChecked = selected.contains(file.name)
                androidx.compose.foundation.layout.Row {
                    Checkbox(checked = isChecked, onCheckedChange = { checked ->
                        selected = if (checked) selected + file.name else selected - file.name
                    })
                    Text(file.name)
                }
            }
        }

        Button(onClick = {
            if (session != null) {
                val outDir = File(session.rootDir, "Colmap")
                val intrinsics = metadata.firstOrNull()?.intrinsics?.let {
                    CameraIntrinsics(it.width, it.height, it.fx, it.fy, it.cx, it.cy, it.distortion)
                } ?: intrinsicsProvider.getBackCameraIntrinsics()

                val colmapImages = metadata.mapIndexed { index, meta ->
                    val q = meta.pose.colmap_q
                    val t = meta.pose.colmap_t
                    ColmapImage(
                        id = index + 1,
                        name = meta.image,
                        pose = PoseUtils.ColmapPose(
                            qw = q.getOrElse(0) { 1.0 },
                            qx = q.getOrElse(1) { 0.0 },
                            qy = q.getOrElse(2) { 0.0 },
                            qz = q.getOrElse(3) { 0.0 },
                            tx = t.getOrElse(0) { 0.0 },
                            ty = t.getOrElse(1) { 0.0 },
                            tz = t.getOrElse(2) { 0.0 }
                        )
                    )
                }
                exporter.export(outDir, intrinsics, colmapImages)
                viewModel.prepareReconstruction()
            }
        }, modifier = Modifier.padding(bottom = 8.dp)) {
            Text("Export for COLMAP")
        }

        Button(onClick = {
            if (session != null) {
                val zipFile = File(session.rootDir, "capture_export.zip")
                ZipUtils.zipDirectory(session.rootDir, zipFile)
            }
        }, modifier = Modifier.padding(bottom = 8.dp)) {
            Text("Export ZIP")
        }

        Button(onClick = {
            if (session != null && selected.isNotEmpty()) {
                selected.forEach { name ->
                    File(session.imagesDir, name).delete()
                    File(session.metadataDir, name.replace(".jpg", ".json")).delete()
                }
                images = session.imagesDir.listFiles()?.sortedBy { it.name } ?: emptyList()
                metadata = session.metadataDir.let { metadataReader.readAll(it) }
                selected = emptySet()
            }
        }) {
            Text("Delete Selected")
        }
    }
}
