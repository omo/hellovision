package es.flakiness.hellocam

import android.app.Activity
import android.graphics.Rect
import android.os.Bundle
import android.util.Size
import android.view.SurfaceHolder
import android.view.View
import es.flakiness.hellocam.habit.rx.Disposer
import es.flakiness.hellocam.kamera.app.Kamera
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.habit.rx.addThen
import es.flakiness.hellocam.kamera.KameraSurface
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.layout_main.*


fun View.clicks() : Observable<Unit> = Observable.create {
    val listener = View.OnClickListener {

    }

    this@clicks.setOnClickListener(listener)
    it.setDisposable(Disposer{ this@clicks.setOnClickListener(null) })
}

class MainActivity : Activity() {

    private val disposables = CompositeDisposable()
    private lateinit var camera: Kamera
    private lateinit var lastOpen : Disposable

    init {
        RxJavaPlugins.setErrorHandler {
            es.flakiness.hellocam.habit.log.error(it, "Uncaught Error!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)

        // TODO(morrita): Consider retry.
        val device = KameraDevice.create(this).cache()

        // Configure the preview size: This enables preview Surface
        Observable.combineLatest(device.toObservable(), viewfinder.viewRects, BiFunction<KameraDevice, Rect, Unit> { d, rect ->
            viewfinder.previewSize = d.sizeFor(Size(rect.width(), rect.height()), SurfaceHolder::class.java)
        }).subscribe().addTo(disposables)

        fun handleError(e: Throwable) {
            es.flakiness.hellocam.habit.log.error(e)
            finish()
        }

        val surfaces : Observable<List<KameraSurface>> = Observable.combineLatest(listOf(viewfinder.surfaces)) {
            it.fold(ArrayList<KameraSurface>()) { a, i -> a.apply { add(i as KameraSurface) } }
        }

        camera = disposables.addThen(
            Kamera(
                device,
                surfaces,
                ::handleError
            )
        )
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