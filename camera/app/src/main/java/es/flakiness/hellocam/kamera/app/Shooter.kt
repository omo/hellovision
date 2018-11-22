package es.flakiness.hellocam.kamera.app

import android.hardware.camera2.*
import android.view.Surface
import es.flakiness.hellocam.habit.log.log
import es.flakiness.hellocam.habit.log.warn
import es.flakiness.hellocam.kamera.KameraOutput
import es.flakiness.hellocam.kamera.KameraSession
import io.reactivex.Completable
import io.reactivex.disposables.Disposable

class FrameRateLogger(private val name: String, private val wrapMs: Int) {

    var count: Int = 0
    var lastTickMs = System.currentTimeMillis()

    fun tick() {
        count++
        val currentMs = System.currentTimeMillis()
        val delta = currentMs - lastTickMs
        if (delta > wrapMs) {
            val fps = count.toFloat() / (delta.toFloat()/1000.0f)
            log("${name}: ${fps.toInt()} fps")
            count = 0
            lastTickMs = currentMs
        }
    }
}

class Shooter(val session: KameraSession, val outputs: List<KameraOutput>) : Disposable by session {
    // TODO(morrita): Consider publishing its own close() as failure as well.
    private val anyFail = Completable.amb(outputs.map { it.maybeFail })
    private val fps = FrameRateLogger("Stream", 2000)
    val maybeFail = session.maybeFail.ambWith(anyFail)

    init {
        maybeFail.subscribe({}, { e -> dispose() })
        start()
    }

    fun start() {
        stream(outputs)
    }

    private fun stream(repeatingOutputs: List<KameraOutput>) {
        val device = session.device
        val session = session.session

        val repeatingReq = device.device.createCaptureRequest(CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG).apply {
            repeatingOutputs.forEach { addTarget(it.surface) }
            set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
        }.build()

        session.setRepeatingRequest(repeatingReq, object: CameraCaptureSession.CaptureCallback() {
            override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int)
                    = warn("Preview onCaptureSequenceAborted")
            override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure)
                    = warn("Preview onCaptureFailed")
            override fun onCaptureBufferLost(session: CameraCaptureSession, request: CaptureRequest, target: Surface, frameNumber: Long)
                    = warn("Preview onBufferLost")
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult) = fps.tick()
        }, device.thread.handler)
    }

}