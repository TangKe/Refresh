package ke.tang.refresh

/**
 * Callback when refresh action is trigger
 * You should set it by [RefreshLayout.setOnRefreshListener]
 */
interface OnRefreshListener {
    /**
     * Called when refresh start
     * when [RefreshLayout] refreshAxis set as vertical, [isHeader] true means from top, otherwise means from bottom
     * when [RefreshLayout] refreshAxis set as horizontal, [isHeader] true means from lefe, otherwise means from right
     */
    fun onRefreshStart(isHeader: Boolean)

    /**
     * Called when refresh is complete
     * return true to intercept the default behavior of complete refresh(reset refresh header/footer state, and restore the content's position)
     * then you can use the [observable] object to trigger the complete refresh behavior at the right time
     */
    fun onRefreshComplete(observable: RefreshObservable): Boolean
}