package es.flakiness.hellocam.kamera.app

import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.util.Size
import es.flakiness.hellocam.habit.log.log
import es.flakiness.hellocam.habit.log.logIf
import es.flakiness.hellocam.habit.rx.Disposer
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.kamera.KameraSurface
import es.flakiness.hellocam.kamera.KameraThread
import io.reactivex.Completable
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

    val surface: KameraSurface = KameraSurface(
        reader.surface,
        Size(reader.width, reader.height),
        "sink",
        ImageReader::class.java,
        Completable.never()
    )

    init {
        reader.setOnImageAvailableListener({
            set(it.acquireLatestImage())
        }, thread.handler)
    }

    fun get() : Image? = image.get()

    private fun set(nextImage: Image?) {
        if (nextImage == null) {
            log("Failed to acquire next image")
        }

        image.getAndSet(nextImage)?.close()
        logIf(++imageCount % 10 == 0, { "Image count: ${imageCount}" })
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
                    3
                ), d.thread
            )
        }
    }
}