package es.flakiness.hellocam.kamera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.util.Size
import es.flakiness.hellocam.kamera.ag.area
import es.flakiness.hellocam.habit.rx.errorAndComplete
import es.flakiness.hellocam.habit.log.log
import es.flakiness.hellocam.habit.log.logThen
import es.flakiness.hellocam.habit.rx.Disposer
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

class KameraDevice(val device: CameraDevice, val maybeFail: Completable, val spec: CameraCharacteristics, val thread: KameraThread) :
    Disposable by Disposer({ logThen("KameraDevice#dispose") { device.close() } }) {

    fun fitSizeFor(constraints: Size, format: Int) : Size {
        // XXX: Align the orientations of the rectangles.
        val candidates = spec.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(format)
        return candidates.fold(Size(0, 0)) { a, i ->
            if (i.height > constraints.height || i.width > constraints.width) { // Too big
                return@fold a
            }

            if (i.area < a.area) { // Smaller than the current match
                return@fold a
            }

            i
        }
    }

    fun largestFor(format: Int) : Size {
        val candidates = spec.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(format)
        return candidates.fold(Size(0, 0)) { a, i -> if (a.area < i.area) i else a }
    }

    companion object {
        private data class DeviceParams(val thread: KameraThread, val manager: CameraManager, val id: String, val spec: CameraCharacteristics)

        @SuppressLint("MissingPermission")
        fun create(context: Context): Single<KameraDevice> = Single.create<DeviceParams> {
            log("Creating KameraDevice")
            val thread = KameraThread()
            val manager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val idAndSpec = selectDevice(manager)
            if (it.isDisposed) {
                return@create thread.dispose()
            }

            it.onSuccess(DeviceParams(thread, manager, idAndSpec.first, idAndSpec.second))
        }.flatMap { params ->
            Single.create<Pair<CameraDevice, Completable>> {
                val errorSubject = PublishSubject.create<Unit>()
                val maybeFail = Completable.fromObservable(errorSubject).cache()
                params.manager.openCamera(params.id, params.thread.executor, object : CameraDevice.StateCallback() {
                    override fun onClosed(camera: CameraDevice) =
                        log("Device onClosed")

                    override fun onOpened(camera: CameraDevice) {
                        if (it.isDisposed) {
                            params.thread.dispose()
                            return camera.close()
                        }

                        it.onSuccess(
                            logThen(
                                "Device onOpened",
                                Pair(camera, maybeFail)
                            )
                        )
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        val error = logThen(
                            "Device Disconnected!",
                            KameraRuntimeException("Disconnected")
                        )
                        errorSubject.errorAndComplete(error)
                        it.onError(error)
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        logThen(
                            "Device Error",
                            KameraRuntimeException("Error: ${error}")
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