package com.yourorg.objectcapture.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Size
import com.yourorg.objectcapture.colmap.CameraIntrinsics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraIntrinsicsProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getBackCameraIntrinsics(): CameraIntrinsics {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            val chars = manager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: manager.cameraIdList.first()

        val characteristics = manager.getCameraCharacteristics(cameraId)
        val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val sizes = configMap?.getOutputSizes(ImageFormat.YUV_420_888)?.toList().orEmpty()
        val size = sizes.maxByOrNull { it.width * it.height } ?: Size(1920, 1080)

        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)

        val focalMm = focalLengths?.firstOrNull() ?: 4.0f
        val sensorWidthMm = sensorSize?.width ?: 4.8f
        val sensorHeightMm = sensorSize?.height ?: 3.6f

        val fx = (focalMm / sensorWidthMm) * size.width
        val fy = (focalMm / sensorHeightMm) * size.height

        val cx = if (activeArray != null) {
            ((activeArray.exactCenterX() / activeArray.width()) * size.width).toDouble()
        } else {
            size.width / 2.0
        }
        val cy = if (activeArray != null) {
            ((activeArray.exactCenterY() / activeArray.height()) * size.height).toDouble()
        } else {
            size.height / 2.0
        }

        return CameraIntrinsics(
            width = size.width,
            height = size.height,
            fx = fx.toDouble(),
            fy = fy.toDouble(),
            cx = cx,
            cy = cy,
            distortion = null
        )
    }
}
