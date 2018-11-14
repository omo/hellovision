package es.flakiness.hellocam

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

fun <T> Subject<T>.nextAndComplete(t : T) {
    log("next:" + t)
    onNext(t)
    onComplete()
}

fun <T, S : Throwable> Subject<T>.errorAndComplete(err: S) {
    onError(err)
    onComplete()
}

class KameraThread() {
    val thread: HandlerThread = HandlerThread("camera").apply { start() }
    val handler: Handler = Handler(thread.looper)
    val executor: Executor = object: Executor {
        override fun execute(command: Runnable) { handler.post(command) }
    }
}

class KameraRuntimeException(message: String) : RuntimeException(message) {}

class KameraDevice(val device: CameraDevice, val maybeFail: Completable, val spec: CameraCharacteristics, val thread: KameraThread) :
    Disposable {
    var disposed = AtomicBoolean(false)

    override fun isDisposed() = disposed.get()
    override fun dispose() = if (disposed.compareAndSet(false, true)) device.close() else Unit

    companion object {
        private data class DeviceParams(val thread: KameraThread, val manager: CameraManager, val id: String, val spec: CameraCharacteristics)

        @SuppressLint("MissingPermission")
        fun create(context: Context): Single<KameraDevice> = Single.create<DeviceParams> {
            log("Creating KameraDevice")
            val thread = KameraThread()
            val manager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val idAndSpec = selectDevice(manager)
            it.onSuccess(DeviceParams(thread, manager, idAndSpec.first, idAndSpec.second))
        }.flatMap { params ->
            Single.create<Pair<CameraDevice, Completable>> {
                val errorSubject = PublishSubject.create<Unit>()
                val maybeFail = Completable.fromObservable(errorSubject).cache()
                params.manager.openCamera(params.id, params.thread.executor, object : CameraDevice.StateCallback() {
                    override fun onClosed(camera: CameraDevice) = log("Device onClosed")

                    override fun onOpened(camera: CameraDevice) {
                        it.onSuccess(logThen(Pair(camera, maybeFail), "Device onOpened"))
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        val error = logThen(KameraRuntimeException("Disconnected"), "Device Disconnected!")
                        errorSubject.errorAndComplete(error)
                        it.onError(error)
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        logThen(KameraRuntimeException("Error: ${error}"), "Device Error").apply {
                            errorSubject.errorAndComplete(this)
                            it.onError(this)
                        }
                    }
                })
            }.map {
                KameraDevice(it.first, it.second, params.spec, params.thread)
            }
        }

        private fun selectDevice(manager: CameraManager) : Pair<String, CameraCharacteristics> =
            manager.cameraIdList.asSequence().map { it
                Pair(it, manager.getCameraCharacteristics(it))
            }.filter {
                it.second.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            }.first()
    }
}

class KameraSurface(val surface: Surface, val size: Size, val maybeFail: Completable) {
    companion object {
        // Note that this stream never completes.
        fun createFrom(holder: SurfaceHolder) : Observable<KameraSurface> = PublishSubject.create<KameraSurface>().apply {
            val lastCompletion: Cell<PublishSubject<Unit>> = Cell()
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                    lastCompletion.set(PublishSubject.create())
                    this@apply.onNext(
                        logThen(
                            KameraSurface(
                                holder!!.surface,
                                Size(width, height),
                                Completable.fromObservable(lastCompletion.ref!!).cache()
                            ),
                            "surfaceChanged"
                        )
                    )
                }

                override fun surfaceDestroyed(holder: SurfaceHolder?) : Unit =
                    lastCompletion.ref?.onError(warnThen(KameraRuntimeException("Surface destroyed"))) ?: Unit
                override fun surfaceCreated(holder: SurfaceHolder?) = Unit
            })
        }.cache()
    }
}

class KameraSession(val device : KameraDevice, val session: CameraCaptureSession) : Disposable {
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
                        override fun onClosed(session: CameraCaptureSession) = log("Session onClosed")
                        override fun onSurfacePrepared(session: CameraCaptureSession, surface: Surface) = log("Session onSurfacePrepared")
                        override fun onActive(session: CameraCaptureSession) = log("Session onActive")
                        override fun onConfigured(session: CameraCaptureSession) = log("Session onConfigured")
                        override fun onConfigureFailed(session: CameraCaptureSession) =
                            src.onError(errorThen(KameraRuntimeException("Failed to Configure CaptureSession")))
                        override fun onReady(session: CameraCaptureSession) =
                            src.onSuccess(logThen(KameraSession(device, session), "Session onReady"))
                    })
                device.device.createCaptureSession(sessionConfig)
            }
    }
}