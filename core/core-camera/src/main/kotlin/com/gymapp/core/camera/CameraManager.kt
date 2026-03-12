package com.gymapp.core.camera

/**
 * Scaffold — CameraX wrapper for capturing workout card images.
 *
 * Implementation is deferred to a future milestone.
 * This interface will be backed by a CameraX [androidx.camera.core.ImageCapture]
 * use case exposed through a Compose-friendly API.
 */
interface CameraManager {

    /**
     * Capture a single JPEG image.
     * @return JPEG bytes on success.
     */
    suspend fun captureImage(): Result<ByteArray>
}
