package es.flakiness.hellocam

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.os.Bundle
import es.flakiness.hellocam.habit.clicks
import es.flakiness.hellocam.habit.log.errorThen
import es.flakiness.hellocam.habit.log.log
import es.flakiness.hellocam.habit.log.warn
import es.flakiness.hellocam.habit.rx.addThen
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.kamera.KameraOutput
import es.flakiness.hellocam.kamera.app.ImageSink
import es.flakiness.hellocam.kamera.app.Kamera
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_main.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*


class ImageSaver(private val directory: File, private val sink: ImageSink, private val sched: Scheduler) {

    data class WriteSource(val bytes: ByteBuffer, val width: Int, val height: Int, val rowStrideBytes: Int) {

        @ExperimentalUnsignedTypes
        fun writeRaw10AsRaw16To(dest: OutputStream) {
            log("Writing: ${this}")
//            writeToAsIs(bytes, dest)

            // Unpack RAW10 to Array of shorts.
            // https://developer.android.com/reference/android/graphics/ImageFormat.html#RAW10
            for (i in 0.until(height)) {
                val rowHead = i * rowStrideBytes
                for (j in 0.until(width)) {
                    // Each 4 pixels are packed into 5-byte bundle.
                    val bundleIndex = j / 4
                    val bundleSlot = j % 4
                    val bundleHead = rowHead + (bundleIndex * 5)
                    // Use UInt instead of UByte because as of 2018-11-25,
                    // "Bit shifts are provided only for UInt and ULong, for the more narrow types, both for signed and unsigned, they are under consideration."
                    // https://github.com/Kotlin/KEEP/blob/master/proposals/unsigned-types.md
                    // It has to go through toUByte() so that the negative numbers round well.
                    val hi : UInt = bytes.get(bundleHead + bundleSlot).toUByte().toUInt()
                    val loSlot : UInt = bytes.get(bundleHead + 4).toUByte().toUInt()
                    val lo = loSlot.shr(bundleSlot).and(0x03u)
                    val pixel = hi.shl(2).or(lo).toUInt()
                    // Little endian
                    if (pixel >= 0x400u) // x**10 == 0x400
                        throw RuntimeException("Wrong pixel: ${pixel}")
                    dest.write(pixel.and(0xffu).toInt())
                    dest.write(pixel.shr(8).and(0xffu).toInt())
//                    // Only pass through hi byte.
//                    dest.write(hi.toInt())
//                    dest.write(0)
                }
            }
       }

        //
        // For diagnosis.
        private fun writeToAsIs(bytes: ByteBuffer, dest: OutputStream) {
            val b = ByteArray(1)
            while (bytes.hasRemaining()) {
                b[0] = bytes.get()
                dest.write(b)
            }
        }
    }

    private val format: SimpleDateFormat = SimpleDateFormat("yyyyMMdd-hhmmss-SSS")

    fun save() {
        val image = sink.take()
        if (image == null) {
            warn("ImageSaver: No image to write.")
            return
        }

        val file = fileToWrite()
        log("ImageSaver: Save image to: ${file} at ${Thread.currentThread().name}")

        try {
            if (image.planes.size != 1) {
                throw errorThen(RuntimeException("Doesn't look like a RAW iamge!"))
            }

            val out = BufferedOutputStream(FileOutputStream(file))
            val plane = image.planes[0]
            WriteSource(plane.buffer, image.width, image.height, plane.rowStride).writeRaw10AsRaw16To(out)
            out.close()
            log("ImageSaver: Saved.")
        } finally {
            image.close()
        }
    }

    fun fileToWrite() = File(directory, format.format(Date()) + ".raw16")

    fun saveOn(events: Observable<Unit>) : Disposable =
        events.observeOn(sched).subscribe { save() }

    companion object {
        fun create(context: Context, sink: Single<ImageSink>, sched: Scheduler) : Single<ImageSaver> =
                sink.map { ImageSaver(context.getExternalFilesDir(null), it, sched) }
    }
}

class MainActivity : Activity() {
    private val disposables = CompositeDisposable()
    private lateinit var camera: Kamera
    private lateinit var lastOpen : Disposable

    init {
        RxJavaPlugins.setErrorHandler {
            es.flakiness.hellocam.habit.log.error(it, "Uncaught Error!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)

        // TODO(morrita): Consider retry.
        val device = KameraDevice.create(this).cache()
        val imageSink = ImageSink.createFrom(device, ImageFormat.RAW10).doOnSuccess { disposables.add(it) }.cache()
        val outputSources = listOf(viewfinder.surfaces, imageSink.map { it.output }.toObservable())
        val outputs : Observable<List<KameraOutput>> = Observable.combineLatest(outputSources) {
                latests -> latests.toList().fold(listOf<KameraOutput>()) { a, i -> a.plus(i as KameraOutput) }
        }

        camera = disposables.addThen(
            Kamera(
                device,
                outputs,
                ::handleError
            )
        )

        viewfinder.resizeBy(device).subscribe().addTo(disposables)
        ImageSaver.create(this, imageSink, Schedulers.io()).subscribe{ saver ->
            saver.saveOn(shutter_button.clicks()).addTo(disposables)
        }.addTo(disposables)
    }

    private fun handleError(e: Throwable) {
        es.flakiness.hellocam.habit.log.error(e)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onStart() {
        super.onStart()
        lastOpen = camera.open()
    }

    override fun onStop() {
        super.onStop()
        lastOpen.dispose()
    }
}