package es.flakiness.hellocam.habit.rx

import io.reactivex.subjects.Subject

fun <T, S : Throwable> Subject<T>.errorAndComplete(err: S) {
    onError(err)
    onComplete()
}

