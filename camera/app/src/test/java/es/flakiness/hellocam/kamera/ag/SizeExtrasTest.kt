package es.flakiness.hellocam.kamera.ag

import android.util.Size
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FileExtras {

    @Test
    fun fitHello() {
        Assert.assertEquals("Shorter target", Size(10, 5).fit(Size(20, 15)), Size(20, 10))
        Assert.assertEquals("Taller target", Size(10, 5).fit(Size(50, 10)), Size(20, 10))
    }
}