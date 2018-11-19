package es.flakiness.hellocam.kamera.app

import es.flakiness.hellocam.kamera.KameraSession
import es.flakiness.hellocam.kamera.KameraSurface
import io.reactivex.Completable
import io.reactivex.disposables.Disposable

class Shooter(val session: KameraSession, val surfaces: List<KameraSurface>) : Disposable by session {
    // TODO(morrita): Consider publishing its own close() as failure as well.
    private val anyFail = Completable.amb(surfaces.map { it.maybeFail })
    val maybeFail = session.maybeFail.ambWith(anyFail)

    init {
        maybeFail.subscribe({}, { e -> dispose() })
        session.startPreview(surfaces)
    }
}