package es.flakiness.hellocam

import android.app.Activity
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.os.Bundle
import android.view.Surface
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.kamera.KameraSession
import es.flakiness.hellocam.kamera.KameraSurface
import es.flakiness.hellocam.rx.DisposableComposite
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.android.synthetic.main.layout_main.viewfinder


// XXX: Implement disposable
class Shooter(session: KameraSession, viewfinder: KameraSurface) {
    init {
        session.startPreview(viewfinder)
    }
}

class Camera(device: Single<KameraDevice>, viewfinderSurfaces: Observable<KameraSurface>) : Disposable {

    private var disposables = DisposableComposite()

    override fun dispose() = disposables.dispose()
    override fun isDisposed() = disposables.isDisposed

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