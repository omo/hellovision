package es.flakiness.hellocam.habit.coll

class Cell<T> {
    var ref : T? = null
    fun <S : T>set(newRef: S) = let {
        ref = newRef
        newRef
    }
}