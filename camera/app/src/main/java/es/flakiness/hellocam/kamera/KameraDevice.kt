package es.flakiness.hellocam.kamera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import es.flakiness.hellocam.rx.errorAndComplete
import es.flakiness.hellocam.log
import es.flakiness.hellocam.logThen
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.atomic.AtomicBoolean

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
                    override fun onClosed(camera: CameraDevice) =
                        log("Device onClosed")

                    override fun onOpened(camera: CameraDevice) {
                        it.onSuccess(logThen(Pair(camera, maybeFail), "Device onOpened"))
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        val error = logThen(
                            KameraRuntimeException("Disconnected"),
                            "Device Disconnected!"
                        )
                        errorSubject.errorAndComplete(error)
                        it.onError(error)
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        logThen(
                            KameraRuntimeException("Error: ${error}"),
                            "Device Error"
                        ).apply {
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