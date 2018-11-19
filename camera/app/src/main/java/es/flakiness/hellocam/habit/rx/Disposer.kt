package es.flakiness.hellocam.habit.rx

import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean

class Disposer(private val disposeUs: () -> Unit) : Disposable {
    private val closed = AtomicBoolean(false)

    override fun isDisposed(): Boolean = closed.get()

    override fun dispose() : Unit {
        if (closed.compareAndSet(false, true)) {
            disposeUs()
        }
    }
}