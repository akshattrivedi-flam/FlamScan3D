package com.yourorg.objectcapture.camera

import android.content.Context
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.yourorg.objectcapture.capture.CaptureSession
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class VideoRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var recordingStartNs: Long? = null

    fun createUseCase(): VideoCapture<Recorder> {
        val recorder = Recorder.Builder().build()
        val useCase = VideoCapture.withOutput(recorder)
        videoCapture = useCase
        return useCase
    }

    fun startRecording(session: CaptureSession) {
        val videoCapture = videoCapture ?: return
        val file = File(session.videoDir, "scan_video.mp4")
        val outputOptions = FileOutputOptions.Builder(file).build()
        recordingStartNs = System.nanoTime()

        activeRecording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { event ->
                if (event is VideoRecordEvent.Finalize) {
                    // TODO: surface errors if needed.
                }
            }
    }

    fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
        recordingStartNs = null
    }

    fun videoTimeMsFromTimestampNs(imageTimestampNs: Long): Long {
        val start = recordingStartNs ?: return 0L
        return ((imageTimestampNs - start) / 1_000_000L).coerceAtLeast(0L)
    }
}
