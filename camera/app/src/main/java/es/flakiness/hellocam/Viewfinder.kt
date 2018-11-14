package es.flakiness.hellocam

import android.content.Context
import android.util.AttributeSet
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject


class Viewfinder @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val surfaceSubject = PublishSubject.create<KameraSurface>()
    val surfaceView = SurfaceView(context)
    val surfaces: Observable<KameraSurface> get() = KameraSurface.createFrom(surfaceView.holder)

    init {
        addView(surfaceView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // TODO: Set SurfaceView size
    }
}