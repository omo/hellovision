package es.flakiness.hellocam

import android.app.Activity
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.os.Bundle
import android.view.Surface
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.android.synthetic.main.layout_main.viewfinder
import java.util.concurrent.atomic.AtomicBoolean


class DisposableComposite : Disposable {
    private var disposed : AtomicBoolean = AtomicBoolean(false)
    private val list : MutableList<Disposable> = ArrayList()

    override fun isDisposed(): Boolean = disposed.get()
    override fun dispose() {
        if (disposed.compareAndSet(false, true)) {
            list.forEach { it.dispose() }
        }
    }

    fun <T : Disposable> add(item: T) : T = item.apply { list.add(item) }
}

class Shooter(session: KameraSession, viewfinder: KameraSurface) {
    init {
        val req = session.device.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(viewfinder.surface)
        }.build()

        session.session.setRepeatingRequest(req, object: CameraCaptureSession.CaptureCallback() {
//            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult)
//                    = log("Preview onCaptureCompleted")
//            override fun onCaptureStarted(session: CameraCaptureSession, request: CaptureRequest, timestamp: Long, frameNumber: Long)
//                    = log("Preivew onCaptureStarted")
//            override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult)
//                    = log("Preivew onCaptureProgressed")
            override fun onCaptureSequenceAborted(session: CameraCaptureSession, sequenceId: Int)
                    = warn("Preview onCaptureSequenceAborted")
            override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure)
                    = warn("Preview onCaptureFailed")
            override fun onCaptureBufferLost(session: CameraCaptureSession, request: CaptureRequest, target: Surface, frameNumber: Long)
                    = warn("Preview onBufferLost")
        }, session.device.thread.handler)
    }
}

class Camera(device: Single<KameraDevice>, viewfinderSurfaces: Observable<KameraSurface>) : Disposable {

    private var disposables = DisposableComposite()

    override fun dispose() = disposables.dispose()
    override fun isDisposed() = disposables.isDisposed

    // XXX: Send capture request
    private val shooters : Observable<Shooter> = device.doOnSuccess{
        disposables.add(it)
    }.toObservable().let {
        Observable.combineLatest(it, viewfinderSurfaces, BiFunction { d: KameraDevice, s: KameraSurface ->
            Pair(d, s)
        }).flatMap {ds ->
            val vfConfig = OutputConfiguration(ds.second.surface)
            KameraSession.create(ds.first, listOf(vfConfig)).toObservable().map {
                Shooter(it, ds.second)
            }
        }
    }

    fun open() : Completable = Completable.fromObservable(shooters.map { Unit })
}



class MainActivity : Activity() {

    init {
        RxJavaPlugins.setErrorHandler {
            error(it, "Uncaught Error!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)
        // TODO(morrita): Dispose appropriately
        val camera = Camera(KameraDevice.create(this), viewfinder.surfaces)
        camera.open().subscribe(
            {},
            { e ->
                error(e)
                finish()
            })
    }
}