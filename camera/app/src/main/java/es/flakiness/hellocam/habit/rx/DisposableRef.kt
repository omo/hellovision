package es.flakiness.hellocam.habit.rx

import io.reactivex.disposables.Disposable

class DisposableRef<T : Disposable>(initialValue: T? = null) :
    Disposable {
    var ref : T? = initialValue
        set(value) {
            field?.dispose()
            field = value
        }

    private val disposer = Disposer { ref?.dispose() }

    override fun dispose() = disposer.dispose()
    override fun isDisposed(): Boolean = disposer.isDisposed
}