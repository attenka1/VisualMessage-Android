@file:OptIn(ExperimentalCamera2Interop::class)

package fi.attenka.VisualMessage.receive

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.Range
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

/**
 * Configures CameraX for morse reception: fixed analysis resolution, optional high fps,
 * and manual exposure (no AE hunting while decoding flashes).
 */
object MorseCameraConfigurator {

    private const val ANALYSIS_WIDTH = 640
    private const val ANALYSIS_HEIGHT = 480
    private const val STANDARD_FPS = 30
    private const val WARMUP_MS = 900L
    // Fixed exposure for bright-on-dark flash detection once warmup completes.
    private const val MANUAL_ISO = 400
    private const val MANUAL_EXPOSURE_NS = 8_333_333L

    fun selectFpsRange(context: Context, preferHigh: Boolean): Range<Int> {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: return Range(STANDARD_FPS, STANDARD_FPS)

        val ranges = manager.getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?: return Range(STANDARD_FPS, STANDARD_FPS)

        if (preferHigh) {
            val bestHigh = ranges
                .filter { it.upper >= 60 }
                .maxByOrNull { it.upper }
                ?: ranges.maxByOrNull { it.upper }
            if (bestHigh != null) return bestHigh
        }

        return ranges.firstOrNull { it.lower <= STANDARD_FPS && it.upper >= STANDARD_FPS }
            ?: ranges.minByOrNull { abs(it.upper - STANDARD_FPS) }
            ?: Range(STANDARD_FPS, STANDARD_FPS)
    }

    @ExperimentalCamera2Interop
    fun buildPreview(fpsRange: Range<Int>): Preview {
        val builder = Preview.Builder()
        Camera2Interop.Extender(builder)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
        return builder.build()
    }

    @ExperimentalCamera2Interop
    fun buildAnalysis(fpsRange: Range<Int>): ImageAnalysis {
        val builder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT))
        Camera2Interop.Extender(builder)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
        return builder.build()
    }

    /** Brief default-AE warmup, then lock to fixed ISO/shutter with AE off. */
    @ExperimentalCamera2Interop
    suspend fun applyManualExposure(camera: Camera) {
        delay(WARMUP_MS)
        val camera2 = Camera2CameraControl.from(camera.cameraControl)
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, MANUAL_ISO)
            .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, MANUAL_EXPOSURE_NS)
            .build()
        withTimeout(CAMERA_CONTROL_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val future = camera2.setCaptureRequestOptions(options)
                continuation.invokeOnCancellation { future.cancel(true) }
                future.addListener(
                    {
                        runCatching { future.get() }
                            .onSuccess { continuation.resume(Unit) }
                            .onFailure(continuation::resumeWithException)
                    },
                    DIRECT_EXECUTOR,
                )
            }
        }
    }

    private val DIRECT_EXECUTOR = Executor(Runnable::run)
    private const val CAMERA_CONTROL_TIMEOUT_MS = 3_000L
}
