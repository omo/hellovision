package es.flakiness.hellocam.kamera.app

import android.hardware.camera2.params.OutputConfiguration
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.kamera.KameraSession
import es.flakiness.hellocam.kamera.KameraOutput
import es.flakiness.hellocam.habit.log.log
import es.flakiness.hellocam.habit.rx.DisposableRef
import es.flakiness.hellocam.habit.rx.Disposer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction

class Kamera(device: Single<KameraDevice>,
             viewfinderSurfaces: Observable<List<KameraOutput>>,
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
        Observable.combineLatest(
            it,
            viewfinderSurfaces,
            BiFunction { d: KameraDevice, s: List<KameraOutput> ->
                Pair(d, s)
            }).flatMap { ds ->


            val outputs = ds.second
            if (outputs.any { !it.surface.isValid }) {
                // Got abandoned Surface (which was cached). This can happen right after onStart().
                // Expect another Surface is coming up from the stream.
                // TODO(morrita): Reject only the failure from the surface from SurfaceView.
                log("Ignoring abandoned Surface.")
                return@flatMap Observable.empty<Shooter>()
            }

            KameraSession.create(ds.first, outputs.map(KameraOutput::toOutput))
                .toObservable().map { sess ->
                Shooter(sess, outputs).apply {
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