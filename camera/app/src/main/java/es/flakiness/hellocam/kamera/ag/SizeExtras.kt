package es.flakiness.hellocam.kamera.ag

import android.util.Size

val Size.area : Int get() = width*height
val Size.ratio : Float get() = height.toFloat()/width.toFloat()

fun Size.toPortrait() : Size {
    if (width < height)
        return this
    return Size(height, width)
}

fun Size.fit(target: Size) : Size {
    val thisR = this.ratio
    val thatR = target.ratio
    val scale = thisR/thatR
    if (thisR < thatR) {
        // If the target is taller, make it shorter
        return Size(target.width, (target.height*scale).toInt())
    } else {
        // If the target is wider, maket it slimer
        return Size((target.width/scale).toInt(), target.height)
    }
}