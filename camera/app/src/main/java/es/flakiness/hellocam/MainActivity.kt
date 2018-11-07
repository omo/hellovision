package es.flakiness.hellocam

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceHolder
import kotlinx.android.synthetic.main.layout_main.viewfinder

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_main)
        viewfinder.surfaceView.holder.addCallback(surfaceCallback())
    }

    private fun surfaceCallback(): SurfaceHolder.Callback {
        return object : SurfaceHolder.Callback {
            override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder?) {
            }

            override fun surfaceCreated(holder: SurfaceHolder?) {
            }
        }
    }
}