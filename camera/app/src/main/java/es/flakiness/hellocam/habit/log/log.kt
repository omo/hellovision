package es.flakiness.hellocam.habit.log

import android.util.Log

val TAG = "HelloCam"

inline fun log(message: String) : Unit { Log.d(TAG, message) }
inline fun warn(message: String) : Unit { Log.w(TAG, message) }
inline fun <E : Throwable>error(error: E, message: String = "Error!") : Unit { Log.e(TAG, message, error) }

inline fun logIf(cond: Boolean, message: () -> String) {
    if (cond) {
        log(message())
    }
}

inline fun <T>logThen(message: String, result: T) : T = result.also { log(message) }
inline fun <T>logThen(message: String, action: () -> T) : T {
    log(message)
    return action()
}

inline fun <T>warnThen(message: String, result: T) : T = result.also { log(message) }
inline fun <E : Throwable>warnThen(e: E) : E = e.apply { warn(toString()) }
inline fun <E : Throwable>errorThen(message: String = "Error!", e: E) : E = e.apply {
    error(e, message)
}

inline fun <E : Throwable>errorThen(e: E) : E = e.apply { error(e) }

inline fun <E : Throwable>errorThenThrow(e: E) {
    error(e)
    throw e
}