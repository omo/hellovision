package es.flakiness.hellocam

import android.app.Activity
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Bundle
import android.util.Size
import android.view.SurfaceHolder
import android.view.View
import es.flakiness.hellocam.habit.rx.Disposer
import es.flakiness.hellocam.kamera.app.Kamera
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.habit.rx.addThen
import es.flakiness.hellocam.kamera.KameraSurface
import es.flakiness.hellocam.kamera.app.ImageSink
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.layout_main.*
import java.lang.RuntimeException


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
            if (!d.spec.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES).any { it == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW }) {
                throw RuntimeException("No Raw Support!")
            }

            viewfinder.previewSize = d.fitSizeFor(Size(rect.width(), rect.height()), ImageFormat.PRIVATE)

        }).subscribe().addTo(disposables)

        // https://developer.android.com/reference/android/graphics/ImageFormat.html#RAW_SENSOR
//        val imageSink = ImageSink.createFrom(device, ImageFormat.JPEG).doOnSuccess { disposables.add(it) }
        val imageSink = ImageSink.createFrom(device, ImageFormat.RAW_SENSOR).doOnSuccess { disposables.add(it) }

        val surfaceSources = listOf(viewfinder.surfaces, imageSink.map { it.surface }.toObservable())
        val surfaces : Observable<List<KameraSurface>> = Observable.combineLatest(surfaceSources) {
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

    private fun handleError(e: Throwable) {
        es.flakiness.hellocam.habit.log.error(e)
        finish()
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