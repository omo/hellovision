package es.flakiness.hellocam.kamera

import android.util.Size
import android.view.Surface
import android.view.SurfaceHolder
import es.flakiness.hellocam.habit.coll.Cell
import es.flakiness.hellocam.habit.log.logThen
import es.flakiness.hellocam.habit.log.warnThen
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class KameraSurface(val surface: Surface, val size: Size, val name: String,  val ownerType: Class<out Any>, val maybeFail: Completable) {
    companion object {
        // Note that this stream never completes.
        fun createFrom(holder: SurfaceHolder, name: String) : Observable<KameraSurface> = PublishSubject.create<KameraSurface>().apply {
            val lastCompletion: Cell<PublishSubject<Unit>> =
                Cell()
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
                    lastCompletion.set(PublishSubject.create())
                    this@apply.onNext(
                        logThen(
                            "surfaceChanged　w=${width} h=${height}",
                            KameraSurface(
                                holder!!.surface,
                                Size(width, height),
                                name,
                                SurfaceHolder::class.java,
                                Completable.fromObservable(lastCompletion.ref!!).cache()
                            )
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