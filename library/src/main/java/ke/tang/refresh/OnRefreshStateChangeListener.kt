package ke.tang.refresh

/**
 * Listen to the refresh state change
 * set by [RefreshLayout.setOnRefreshStateChangeListener]
 */
interface OnRefreshStateChangeListener {
    /**
     * content offset changed
     */
    fun onContentOffset(offset: Int)
}