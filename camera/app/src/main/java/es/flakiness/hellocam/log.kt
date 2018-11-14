package es.flakiness.hellocam

import android.util.Log

private val TAG = "HelloCam"

fun log(message: String) : Unit { Log.d(TAG, message) }
fun warn(message: String) : Unit { Log.w(TAG, message) }
fun <E : Throwable>error(error: E, message: String = "Error!") : Unit { Log.e(TAG, message, error) }

fun <T>logThen(result: T, message: String) : T = result.also { log(message) }
fun <T>warnThen(result: T, message: String) : T = result.also { log(message) }
fun <E : Throwable>warnThen(e: E) : E = e.apply { warn(toString()) }
fun <E : Throwable>errorThen(e: E, message: String = "Error!") : E = e.apply { error(e, message) }

fun <E : Throwable>errorThenThrow(e: E) {
    error(e)
    throw e
}