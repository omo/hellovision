package es.flakiness.hellocam

import android.app.Activity
import android.hardware.camera2.params.OutputConfiguration
import android.os.Bundle
import android.util.Size
import android.view.SurfaceHolder
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.kamera.KameraSession
import es.flakiness.hellocam.kamera.KameraSurface
import es.flakiness.hellocam.rx.DisposableRef
import es.flakiness.hellocam.rx.Disposer
import es.flakiness.hellocam.rx.addThen
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.android.synthetic.main.layout_main.*


class Shooter(session: KameraSession, viewfinder: KameraSurface) : Disposable by session {
    // TODO(morrita): Consider publishing its own close() as failure as well.
    val maybeFail = session.maybeFail.ambWith(viewfinder.maybeFail)

    init {
        maybeFail.subscribe({}, { e -> dispose() })
        session.startPreview(viewfinder)
    }
}

class Camera(device: Single<KameraDevice>,
             viewfinderSurfaces: Observable<KameraSurface>,
             private val onError: (Throwable) -> Unit)
    : Disposable {

    private val disposables = CompositeDisposable()
    private val disposer = Disposer {
        disposables.dispose()
    }

    override fun dispose() = disposer.dispose()
    override fun isDisposed() = disposer.isDisposed

    // TODO(morrita): Consider subscribe eagerly.
    private val cachedDevice = device.cache()
    private val shooters : Observable<Shooter> = cachedDevice.doOnSuccess{
        disposables.add(it)
    }.toObservable().let {
        val last = DisposableRef<Shooter>()
        disposables.add(last)
        Observable.combineLatest(it, viewfinderSurfaces, BiFunction { d: KameraDevice, s: KameraSurface ->
            Pair(d, s)
        }).flatMap { ds ->
            if (!ds.second.surface.isValid) {
                // Got abandoned Surface (which was cached). This can happen right after onStart().
                // Expect another Surface is coming up from the stream.
                log("Ignoring abandoned Surface.")
                return@flatMap Observable.empty<Shooter>()
            }

            val vfConfig = OutputConfiguration(ds.second.surface)
            KameraSession.create(ds.first, listOf(vfConfig)).toObservable().map { sess ->
                Shooter(sess, ds.second).apply {
                    last.ref = this
                }
            }
        }.doOnDispose {
            disposables.delete(last)
        }
    }

    fun open() : Disposable =
        Completable.fromObservable(shooters).subscribe({}, onError)
}


class MainActivity : Activity() {

    private val disposables = CompositeDisposable()
    private lateinit var camera: Camera
    private lateinit var lastOpen : Disposable

    init {
        RxJavaPlugins.setErrorHandler {
            error(it, "Uncaught Error!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)
        // TODO(morrita): Dispose appropriately

        val device = KameraDevice.create(this).doOnSuccess { d ->
            disposables.addThen(viewfinder.viewRects.subscribe {
                viewfinder.previewSize = d.sizeFor(Size(it.width(), it.height()), SurfaceHolder::class.java)
            })
        }

        fun handleError(e: Throwable) : Unit {
            error(e)
            finish()
        }

        camera = disposables.addThen(Camera(device, viewfinder.surfaces, ::handleError))
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onStart() {
        super.onStart()
        lastOpen = camera.open()
    }

    override fun onStop() {
        super.onStop()
        lastOpen.dispose()
    }
}