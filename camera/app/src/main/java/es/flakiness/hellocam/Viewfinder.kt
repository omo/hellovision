package es.flakiness.hellocam

import android.content.Context
import android.graphics.Rect
import android.hardware.camera2.CameraDevice
import android.util.AttributeSet
import android.util.Size
import android.view.Gravity
import android.view.SurfaceView
import android.widget.FrameLayout
import es.flakiness.hellocam.kamera.KameraSurface
import es.flakiness.hellocam.kamera.ag.fit
import es.flakiness.hellocam.kamera.ag.toPortrait
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject


class Viewfinder @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val surfaceView = SurfaceView(context)

    private val surfaceSubject = BehaviorSubject.create<KameraSurface>()
    val surfaces: Observable<KameraSurface> get() = KameraSurface.createFrom(surfaceView.holder).filter{ previewSize == it.size }
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


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        viewRectSubject.onNext(Rect(left, top, right, bottom))
    }
}