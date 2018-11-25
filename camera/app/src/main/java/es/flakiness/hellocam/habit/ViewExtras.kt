package es.flakiness.hellocam.habit

import android.view.View
import es.flakiness.hellocam.habit.rx.Disposer
import io.reactivex.Observable

fun View.clicks() : Observable<Unit> = Observable.create { source ->
    val listener = View.OnClickListener { source.onNext(Unit) }
    this@clicks.setOnClickListener(listener)
        source.setDisposable(Disposer { this@clicks.setOnClickListener(null) })
}