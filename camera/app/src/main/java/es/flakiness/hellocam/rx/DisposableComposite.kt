package es.flakiness.hellocam.rx

import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicBoolean

class DisposableComposite : Disposable {
    private var disposed : AtomicBoolean = AtomicBoolean(false)
    private val list : MutableList<Disposable> = ArrayList()

    override fun isDisposed(): Boolean = disposed.get()
    override fun dispose() {
        if (disposed.compareAndSet(false, true)) {
            list.forEach { it.dispose() }
        }
    }

    fun <T : Disposable> add(item: T) : T = item.apply { list.add(item) }
}