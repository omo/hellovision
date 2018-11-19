package es.flakiness.hellocam.habit.rx

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

fun <T : Disposable> CompositeDisposable.addThen(d: T) : T = d.apply { add(d) }