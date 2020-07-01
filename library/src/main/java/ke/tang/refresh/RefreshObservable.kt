package ke.tang.refresh

import java.util.*

class RefreshObservable(observer: Observer) : Observable() {
    init {
        addObserver(observer)
    }

    fun notifyRefreshComplete() {
        setChanged()
        notifyObservers()
    }
}