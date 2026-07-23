package fi.attenka.VisualMessage.player

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class FlashlightController(context: Context) {

    private val cameraManager = context.applicationContext
        .getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val torchCameraId: String? by lazy { findTorchCameraId() }
    private var currentState: Boolean? = null

    val isAvailable: Boolean
        get() = torchCameraId != null

    fun setEnabled(enabled: Boolean) {
        if (enabled == currentState) return
        val cameraId = torchCameraId ?: return
        runCatching {
            cameraManager.setTorchMode(cameraId, enabled)
            currentState = enabled
        }.onFailure { error ->
            if (error !is CameraAccessException && error !is SecurityException) throw error
        }
    }

    fun turnOff() {
        setEnabled(false)
    }

    private fun findTorchCameraId(): String? = runCatching {
        cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
        } ?: cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()
}
