package es.flakiness.hellocam

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.util.AttributeSet
import android.util.Size
import android.view.Gravity
import android.view.SurfaceView
import android.widget.FrameLayout
import es.flakiness.hellocam.habit.log.log
import es.flakiness.hellocam.habit.rx.DisposableRef
import es.flakiness.hellocam.habit.rx.Disposer
import es.flakiness.hellocam.kamera.KameraDevice
import es.flakiness.hellocam.kamera.KameraOutput
import es.flakiness.hellocam.kamera.ag.fit
import es.flakiness.hellocam.kamera.ag.toPortrait
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.addTo
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.layout_main.*
import java.lang.RuntimeException


class Viewfinder @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val surfaceView = SurfaceView(context)

    val surfaces: Observable<KameraOutput> get() = KameraOutput.createFrom(surfaceView.holder, SURFACE_NAME, true).filter{ previewSize == it.size }
    private val viewRectSubject = BehaviorSubject.create<Rect>()
    val viewRects: Observable<Rect> = viewRectSubject.distinctUntilChanged()

    init {
        addView(surfaceView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )
    }

    var previewSize: Size? = null
        set(size) {
            size?.let {
                log("previewSize: " + size)
                field = it
                post{
                    surfaceView.holder.setFixedSize(it.width, it.height)
                    // toPortrait() is fine as long as the ratio is match, since camera image stream automatically
                    // rotate to fit to the view.
                    it.toPortrait().fit(Size(width, height)).let {
                        surfaceView.layoutParams = LayoutParams(it.width, it.height, Gravity.CENTER)
                    }
                }
            }
        }

    fun resizeBy(device: Single<KameraDevice>) : Observable<Unit> =
        Observable.combineLatest(device.toObservable(), viewRects, BiFunction<KameraDevice, Rect, Unit> { d, rect ->
            previewSize = d.fitSizeFor(Size(rect.width(), rect.height()), ImageFormat.PRIVATE)
        }).map{ Unit }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        viewRectSubject.onNext(Rect(left, top, right, bottom))
    }

    companion object {
        val SURFACE_NAME = "viewfindner"
    }
}