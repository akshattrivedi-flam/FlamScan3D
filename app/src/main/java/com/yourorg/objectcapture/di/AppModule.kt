package com.yourorg.objectcapture.di

import com.yourorg.objectcapture.ar.DepthDistanceController
import com.yourorg.objectcapture.capture.FrameProcessor
import com.yourorg.objectcapture.capture.FeatureDetector
import com.yourorg.objectcapture.capture.PoseDeltaTracker
import com.yourorg.objectcapture.capture.ExposureTracker
import com.yourorg.objectcapture.colmap.CameraFileWriter
import com.yourorg.objectcapture.colmap.ImageListWriter
import com.yourorg.objectcapture.colmap.PoseFileWriter
import com.yourorg.objectcapture.core.FeedbackManager
import com.yourorg.objectcapture.core.OrbitManager
import com.yourorg.objectcapture.core.SessionScoreManager
import com.yourorg.objectcapture.core.CaptureMetricsStore
import com.yourorg.objectcapture.camera.CameraIntrinsicsProvider
import com.yourorg.objectcapture.storage.MetadataWriter
import com.yourorg.objectcapture.storage.MetadataReader
import com.yourorg.objectcapture.storage.DraftManager
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFeedbackManager(): FeedbackManager = FeedbackManager()

    @Provides
    @Singleton
    fun provideOrbitManager(): OrbitManager = OrbitManager()

    @Provides
    @Singleton
    fun provideSessionScoreManager(): SessionScoreManager = SessionScoreManager()

    @Provides
    @Singleton
    fun provideCaptureMetricsStore(): CaptureMetricsStore = CaptureMetricsStore()

    @Provides
    @Singleton
    fun provideFrameProcessor(): FrameProcessor = FrameProcessor()

    @Provides
    @Singleton
    fun provideMetadataWriter(): MetadataWriter = MetadataWriter()

    @Provides
    fun provideCameraFileWriter(): CameraFileWriter = CameraFileWriter()

    @Provides
    fun provideImageListWriter(): ImageListWriter = ImageListWriter()

    @Provides
    fun providePoseFileWriter(): PoseFileWriter = PoseFileWriter()

    @Provides
    @Singleton
    fun provideDepthDistanceController(): DepthDistanceController = DepthDistanceController()

    @Provides
    @Singleton
    fun providePoseDeltaTracker(): PoseDeltaTracker = PoseDeltaTracker()

    @Provides
    @Singleton
    fun provideExposureTracker(): ExposureTracker = ExposureTracker()

    @Provides
    @Singleton
    fun provideFeatureDetector(): FeatureDetector = FeatureDetector()

    @Provides
    @Singleton
    fun provideMetadataReader(): MetadataReader = MetadataReader()

    @Provides
    @Singleton
    fun provideCameraIntrinsicsProvider(@ApplicationContext context: Context): CameraIntrinsicsProvider =
        CameraIntrinsicsProvider(context)

    @Provides
    @Singleton
    fun provideDraftManager(): DraftManager = DraftManager()
}
