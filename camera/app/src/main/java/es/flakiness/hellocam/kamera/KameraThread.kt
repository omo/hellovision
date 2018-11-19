package es.flakiness.hellocam.kamera

import android.os.Handler
import android.os.HandlerThread
import es.flakiness.hellocam.habit.log.logThen
import es.flakiness.hellocam.habit.rx.Disposer
import io.reactivex.disposables.Disposable
import java.util.concurrent.Executor

class KameraThread : Disposable {
    private val disposer = Disposer{ thread.quit() }
    val thread: HandlerThread = HandlerThread("camera").apply { start() }
    val handler: Handler = Handler(thread.looper)
    val executor: Executor = object: Executor {
        override fun execute(command: Runnable) { handler.post(command) }
    }

    override fun isDisposed(): Boolean = disposer.isDisposed
    override fun dispose() = logThen("KameraThread#dispose") { disposer.dispose() }
}