package ke.tang.refresh

/**
 * You may use this interface to make your view be a refresh element
 */
interface Refreshable {
    /**
     * Tell the [RefreshLayout] this view is a indicator
     * that means your view will be shown when use pull, but can never trigger refresh action, after user release their figure, the [RefreshLayout] will restore initial state
     * this is useful when the list reach the end of page and no more data can be load more
     */
    fun isIndicator(): Boolean

    /**
     * Content offset change
     * [delta] from 0..1..N
     * 0 means the layout is in default state
     * 1 means the content distance moved has been reached the distance which [getContentSize] returned
     */
    fun onOffset(delta: Float)

    /**
     * Invoked when user release their figure
     * [isTrigger] true means the distance of content been pulled was greater than or equals to [getContentSize] returned
     */
    fun onRelease(isTrigger: Boolean)

    /**
     * Invoked when the layout is reset to default state
     *
     */
    fun onReset()

    /**
     * Return refresh element's size
     * if refreshAxis is set to vertical, it means height
     * if refreshAxis is set to horizontal, it means width
     */
    fun getContentSize(isRefreshVertical: Boolean): Int
}