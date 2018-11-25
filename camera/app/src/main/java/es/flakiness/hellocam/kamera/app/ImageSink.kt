package es.flakiness.hellocam.kamera.app

import android.media.Image
import android.media.ImageReader
import android.util.Size
import es.flakiness.hellocam.habit.log.log
import es.flakiness.hellocam.habit.rx.Disposer
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.kamera.KameraOutput
import es.flakiness.hellocam.kamera.KameraThread
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicReference

class ImageSink(val reader: ImageReader, thread: KameraThread) :
    Disposable {
    override fun dispose() = disposer.dispose()
    override fun isDisposed() = disposer.isDisposed

    private val disposer = Disposer { reader.close() }
    private val image = AtomicReference<Image>(null);
    private var imageCount = 0

    val output: KameraOutput = KameraOutput(
        reader.surface,
        Size(reader.width, reader.height),
        "sink"
    )

    init {
        reader.setOnImageAvailableListener({
            set(it.acquireLatestImage())
        }, thread.handler)
    }

    fun take() : Image? = image.getAndSet(null)

    private fun set(nextImage: Image?) {
        if (nextImage == null) {
            log("Failed to acquire next image")
        }

        image.getAndSet(nextImage)?.close()
        imageCount++
    }

    companion object {
        fun createFrom(device: Single<KameraDevice>, format: Int) : Single<ImageSink> = device.map { d ->
            val size = d.largestFor(format)
            log("Creating image sink. Size=${size}")
            ImageSink(
                ImageReader.newInstance(
                    size.width,
                    size.height,
                    format,
                    10
                ), d.thread
            )
        }
    }
}