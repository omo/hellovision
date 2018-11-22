package es.flakiness.hellocam

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.os.Bundle
import es.flakiness.hellocam.habit.clicks
import es.flakiness.hellocam.habit.log.log
import es.flakiness.hellocam.habit.rx.addThen
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.kamera.KameraOutput
import es.flakiness.hellocam.kamera.app.ImageSink
import es.flakiness.hellocam.kamera.app.Kamera
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_main.*
import java.io.File


class ImageSaver(private val directory: File, private val sink: ImageSink, private val sched: Scheduler) {
    fun save() {
        log("Save image on: ${directory} at ${Thread.currentThread().name}")
    }

    fun saveOn(events: Observable<Unit>) : Disposable =
        events.observeOn(sched).subscribe { save() }

    companion object {
        fun create(context: Context, sink: Single<ImageSink>, sched: Scheduler) : Single<ImageSaver> =
                sink.map { ImageSaver(context.getExternalFilesDir(null), it, sched) }
    }
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
        val imageSink = ImageSink.createFrom(device, ImageFormat.RAW10).doOnSuccess { disposables.add(it) }.cache()
        val outputSources = listOf(viewfinder.surfaces, imageSink.map { it.output }.toObservable())
        val outputs : Observable<List<KameraOutput>> = Observable.combineLatest(outputSources) {
                latests -> latests.toList().fold(listOf<KameraOutput>()) { a, i -> a.plus(i as KameraOutput) }
        }

        camera = disposables.addThen(
            Kamera(
                device,
                outputs,
                ::handleError
            )
        )

        viewfinder.resizeBy(device).subscribe().addTo(disposables)
        ImageSaver.create(this, imageSink, Schedulers.io()).subscribe{ saver ->
            saver.saveOn(shutter_button.clicks()).addTo(disposables)
        }.addTo(disposables)
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