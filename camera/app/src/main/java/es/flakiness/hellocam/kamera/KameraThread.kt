package es.flakiness.hellocam.kamera

import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.Executor

class KameraThread {
    val thread: HandlerThread = HandlerThread("camera").apply { start() }
    val handler: Handler = Handler(thread.looper)
    val executor: Executor = object: Executor {
        override fun execute(command: Runnable) { handler.post(command) }
    }
}