package es.flakiness.hellocam.kamera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.view.Surface
import es.flakiness.hellocam.errorThen
import es.flakiness.hellocam.log
import es.flakiness.hellocam.logThen
import io.reactivex.Single
import io.reactivex.disposables.Disposable

class KameraSession(val device : KameraDevice, val session: CameraCaptureSession) :
    Disposable {
    override fun dispose() {
        TODO("not implemented")
    }

    override fun isDisposed(): Boolean {
        TODO("not implemented")
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