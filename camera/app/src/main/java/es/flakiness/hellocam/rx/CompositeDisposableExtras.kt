package es.flakiness.hellocam.rx

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun <T : Disposable> CompositeDisposable.addThen(d: T) : T = d.apply { add(d) }