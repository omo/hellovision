package es.flakiness.hellocam.kamera

import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import es.flakiness.hellocam.coll.Cell
import es.flakiness.hellocam.logThen
import es.flakiness.hellocam.warnThen
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class KameraSurface(val surface: Surface, val size: Size, val maybeFail: Completable) {
    companion object {
        // Note that this stream never completes.
        fun createFrom(holder: SurfaceHolder) : Observable<KameraSurface> = PublishSubject.create<KameraSurface>().apply {
            val lastCompletion: Cell<PublishSubject<Unit>> =
                Cell()
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
                    lastCompletion.ref?.onError(
                        warnThen(
                            KameraRuntimeException(
                                "Surface destroyed"
                            )
                        )
                    ) ?: Unit
                override fun surfaceCreated(holder: SurfaceHolder?) = Unit
            })
        }.cache()
    }
}