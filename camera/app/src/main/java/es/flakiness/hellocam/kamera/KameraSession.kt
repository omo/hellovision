package es.flakiness.hellocam.kamera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.view.Surface
import es.flakiness.hellocam.errorThen
import es.flakiness.hellocam.log
import es.flakiness.hellocam.logThen
import es.flakiness.hellocam.rx.Disposer
import es.flakiness.hellocam.warn
import io.reactivex.Single
import io.reactivex.disposables.Disposable

class KameraSession(val device : KameraDevice, val session: CameraCaptureSession) :
    Disposable by Disposer({ session.close() }){

    fun startPreview(surface: KameraSurface) {
        val req = device.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface.surface)
        }.build()

        session.setRepeatingRequest(req, object: CameraCaptureSession.CaptureCallback() {
            override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int)
                    = warn("Preview onCaptureSequenceAborted")
            override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure)
                    = warn("Preview onCaptureFailed")
            override fun onCaptureBufferLost(session: CameraCaptureSession, request: CaptureRequest, target: Surface, frameNumber: Long)
                    = warn("Preview onBufferLost")
        }, device.thread.handler)
    }

    companion object {
       fun create(device: KameraDevice, ocs: List<OutputConfiguration>): Single<KameraSession> =
            Single.create<KameraSession> { src ->
                log("Creating KameraSession")
                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    ocs,
                    device.thread.executor,
                    object : CameraCaptureSession.StateCallback() {
                        override fun onClosed(session: CameraCaptureSession) =
                            log("Session onClosed")

                        override fun onSurfacePrepared(
                            session: CameraCaptureSession,
                            surface: Surface
                        ) = log("Session onSurfacePrepared")

                        override fun onActive(session: CameraCaptureSession) =
                            log("Session onActive")

                        override fun onConfigured(session: CameraCaptureSession) =
                            log("Session onConfigured")

                        override fun onConfigureFailed(session: CameraCaptureSession) =
                            src.onError(
                                errorThen(
                                    KameraRuntimeException(
                                        "Failed to Configure CaptureSession"
                                    )
                                )
                            )

                        override fun onReady(session: CameraCaptureSession) =
                            src.onSuccess(
                                logThen(
                                    KameraSession(
                                        device,
                                        session
                                    ), "Session onReady"
                                )
                            )
                    })
                device.device.createCaptureSession(sessionConfig)
            }
    }
}