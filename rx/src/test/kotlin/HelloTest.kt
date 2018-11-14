import com.google.common.truth.Truth
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.atomic.AtomicInteger

@RunWith(JUnit4::class)
class HelloTest {

    @Test
    fun hello() {
        Truth.assertThat(true).isTrue()
    }

    var fromNext : Int = 0

    @Test
    fun singleFromObservable() {
        val toNext = 123
        val single = Single.create<Int>({ src ->
            src.onSuccess(toNext)
        })

        single.doOnSuccess { fromNext = it }.subscribe()
        Truth.assertThat(fromNext).isEqualTo(toNext)
    }

    var err: Throwable? = null

    @Before
    fun clearError() {
        err = null
    }

    @Test
    fun cacheCompletableErrorBeforeSubscription() {
        val subj = PublishSubject.create<Unit>()
        val c = Completable.fromObservable(subj.cache())
        subj.onError(RuntimeException("XXX"))
        subj.onComplete()

        c.subscribe({}, { err = it })
        Truth.assertThat(err).isNotNull()
    }

    @Test
    fun cacheCompletableErrorAfterSubscription() {
        val subj = PublishSubject.create<Unit>()
        val c = Completable.fromObservable(subj.cache())

        c.subscribe({}, { err = it })
        Truth.assertThat(err).isNull()

        subj.onError(RuntimeException("XXX"))
        subj.onComplete()
        Truth.assertThat(err).isNotNull()
    }

    @Test
    fun singleToCacheRepeat_WRONG() {
        val subCount = AtomicInteger(0)
        val single = Single.create<Int> {
            it.onSuccess(subCount.getAndIncrement())
        }

        // This doesn't work as I expect. The immediate repeat() pulls the cache eargerly.
        val nextCount = AtomicInteger(0)
        val cached = single.cache().toObservable().repeat(10).doOnNext{ nextCount.getAndIncrement() }
        val subj = PublishSubject.create<Int>()

        subj.zipWith(cached, { x, y -> 1 }).subscribe()
        repeat(5) { subj.onNext(1) }

        Truth.assertThat(subCount.get()).isEqualTo(1)
//        Truth.assertThat(nextCount.get()).isEqualTo(5)
    }
}