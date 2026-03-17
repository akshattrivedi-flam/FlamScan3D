package com.yourorg.objectcapture.di

import com.yourorg.objectcapture.capture.CaptureController
import com.yourorg.objectcapture.camera.CameraController
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface CaptureEntryPoint {
    fun cameraController(): CameraController
    fun captureController(): CaptureController
    fun colmapExporter(): com.yourorg.objectcapture.colmap.ColmapExporter
    fun metadataReader(): com.yourorg.objectcapture.storage.MetadataReader
    fun intrinsicsProvider(): com.yourorg.objectcapture.camera.CameraIntrinsicsProvider
    fun draftManager(): com.yourorg.objectcapture.storage.DraftManager
}
