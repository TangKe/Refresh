package ke.tang.refresh

import android.annotation.SuppressLint
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.*
import android.widget.Scroller
import androidx.core.view.*
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class RefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.refreshLayoutStyle
) : ViewGroup(context, attrs, defStyleAttr), NestedScrollingChild, NestedScrollingParent {
    private val mScroller by lazy { Scroller(context) }
    private val mTouchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop }
    private val mRefreshAxis: Int
    private val mRefreshVertical
        get() = mRefreshAxis == REFRESH_VERTICAL

    private var mHeader: View? = null
    private var mContent: View? = null
    private var mFooter: View? = null

    private var mHeaderRefreshable: Refreshable? = null
    private var mFooterRefreshable: Refreshable? = null

    private var mState = STATE_IDLE
    private var mTargetState = STATE_IDLE
    private var mContentOffset = 0

    private var mTarget: Refreshable? = null
    private var mLastTarget: Refreshable? = null
    private var mPressedY = 0f
    private var mPressedX = 0f
    private var mLastY = 0f
    private var mLastX = 0f
    private val mScrollOffset = IntArray(2)
    private val mScrollConsumed = IntArray(2)

    private val mChildHelper = NestedScrollingChildHelper(this)
    private val mObserver = Observer { _, _ -> completeRefreshImmediately() }

    private var mOnRefreshListener: OnRefreshListener? = null
    private var mInternalOnRefreshListener: OnRefreshListener? = null
    private var mOnRefreshStateChangeListener: OnRefreshStateChangeListener? = null

    init {
        with(context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout, defStyleAttr, 0)) {
            mRefreshAxis = getInt(R.styleable.RefreshLayout_refreshAxis, REFRESH_VERTICAL)
            recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var targetWidth = 0
        var targetHeight = 0
        forEach {
            when ((it.layoutParams as LayoutParams).role) {
                LayoutParams.ROLE_CONTENT -> {
                    targetHeight = it.measuredHeight
                }
                LayoutParams.ROLE_HEADER, LayoutParams.ROLE_FOOTER -> {

                }
                else -> {
                    targetWidth = max(targetWidth, it.measuredWidth)
                }
            }
            measureChildWithMargins(
                it,
                widthMeasureSpec,
                paddingLeft + paddingRight,
                heightMeasureSpec,
                paddingTop + paddingBottom
            )
        }
        setMeasuredDimension(
            View.resolveSize(targetWidth, widthMeasureSpec),
            View.resolveSize(targetHeight, heightMeasureSpec)
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        forEach { view ->
            (view.layoutParams as LayoutParams).let {
                when (it.role) {
                    LayoutParams.ROLE_FOOTER -> layoutFooter(view, it)
                    LayoutParams.ROLE_HEADER -> layoutHeader(view, it)
                    LayoutParams.ROLE_CONTENT -> layoutContent(view, it)
                    else -> {
                        //this view without role or layout_refresh_role will be discard when been added
                        view.layout(0, 0, 0, 0)
                    }
                }
            }
        }
    }

    private fun layoutFooter(footer: View, layoutParams: LayoutParams) {
        val childMeasuredWidth = footer.measuredWidth
        val childMeasuredHeight = footer.measuredHeight

        val left: Int
        val right: Int
        val bottom: Int
        val top: Int
        if (mRefreshAxis == REFRESH_VERTICAL) {
            computeLayoutHorizontalCoordinator(footer).let {
                left = it.first
                right = it.second
            }
            top = height - paddingBottom + layoutParams.topMargin
            bottom = top + childMeasuredHeight
        } else {
            computeLayoutVerticalCoordinator(footer).let {
                top = it.first
                bottom = it.second
            }
            left = width - paddingRight + layoutParams.leftMargin
            right = left + childMeasuredWidth
        }

        footer.layout(left, top, right, bottom)
    }

    private fun layoutHeader(header: View, layoutParams: LayoutParams) {
        val childMeasuredWidth = header.measuredWidth
        val childMeasuredHeight = header.measuredHeight

        val left: Int
        val top: Int
        val right: Int
        val bottom: Int
        if (mRefreshAxis == REFRESH_VERTICAL) {
            computeLayoutHorizontalCoordinator(header).let {
                left = it.first
                right = it.second
            }
            bottom = paddingTop - layoutParams.bottomMargin
            top = bottom - childMeasuredHeight
        } else {
            computeLayoutVerticalCoordinator(header).let {
                top = it.first
                bottom = it.second
            }
            right = paddingLeft - layoutParams.leftMargin
            left = right - childMeasuredWidth
        }

        header.layout(left, top, right, bottom)
    }

    private fun computeLayoutVerticalCoordinator(view: View): Pair<Int, Int> {
        val childMeasuredHeight = view.measuredHeight
        val layoutParams = view.layoutParams as LayoutParams
        val top: Int
        val bottom: Int
        when (layoutParams.gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.TOP -> {
                top = paddingTop + layoutParams.topMargin
                bottom = top + childMeasuredHeight
            }
            Gravity.CENTER_VERTICAL -> {
                top =
                    (height - paddingTop - paddingBottom - childMeasuredHeight - layoutParams.topMargin - layoutParams.bottomMargin) / 2
                bottom = top + childMeasuredHeight
            }
            Gravity.BOTTOM -> {
                bottom = height - paddingBottom - layoutParams.bottomMargin
                top = bottom - childMeasuredHeight
            }
            else -> {
                top = paddingTop + layoutParams.topMargin
                bottom = top + childMeasuredHeight
            }
        }
        return top to bottom
    }

    private fun computeLayoutHorizontalCoordinator(view: View): Pair<Int, Int> {
        val childMeasuredWidth = view.measuredWidth
        val layoutParams = view.layoutParams as LayoutParams
        val left: Int
        val right: Int
        when (layoutParams.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.START -> {
                left = paddingLeft + layoutParams.leftMargin
                right = left + childMeasuredWidth
            }
            Gravity.CENTER_HORIZONTAL -> {
                left =
                    (width - paddingLeft - paddingRight - childMeasuredWidth - layoutParams.leftMargin - layoutParams.rightMargin) / 2
                right = left + childMeasuredWidth
            }
            Gravity.END -> {
                right = width - paddingRight - layoutParams.rightMargin
                left = right - childMeasuredWidth
            }
            else -> {
                left = paddingLeft + layoutParams.leftMargin
                right = left + childMeasuredWidth
            }
        }
        return left to right
    }

    private fun layoutContent(content: View, layoutParams: LayoutParams) {
        val childMeasuredWidth = content.measuredWidth
        val childMeasuredHeight = content.measuredHeight

        val left: Int
        val top: Int
        val right: Int
        val bottom: Int
        when (layoutParams.gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.START -> {
                left = paddingLeft + layoutParams.leftMargin
                right = left + childMeasuredWidth
            }
            Gravity.CENTER_HORIZONTAL -> {
                left =
                    (width - paddingLeft - paddingRight - childMeasuredWidth - layoutParams.leftMargin - layoutParams.rightMargin) / 2
                right = left + childMeasuredWidth
            }
            Gravity.END -> {
                right = width - paddingRight - layoutParams.rightMargin
                left = right - childMeasuredWidth
            }
            else -> {
                left = paddingLeft + layoutParams.leftMargin
                right = left + childMeasuredWidth
            }
        }

        when (layoutParams.gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.TOP -> {
                top = paddingTop + layoutParams.topMargin
                bottom = top + childMeasuredHeight
            }
            Gravity.CENTER_VERTICAL -> {
                top =
                    (height - paddingTop - paddingBottom - layoutParams.topMargin - layoutParams.bottomMargin) / 2
                bottom = top + childMeasuredHeight
            }
            Gravity.BOTTOM -> {
                bottom = height - paddingBottom - layoutParams.bottomMargin
                top = bottom - childMeasuredHeight
            }
            else -> {
                top = paddingTop + layoutParams.topMargin
                bottom = top + childMeasuredHeight
            }
        }

        content.layout(left, top, right, bottom)
    }

    override fun onViewAdded(child: View) {
        super.onViewAdded(child)
        when ((child.layoutParams as LayoutParams).role) {
            LayoutParams.ROLE_HEADER -> {
                mHeader = child
                mHeaderRefreshable = child as? Refreshable
            }
            LayoutParams.ROLE_FOOTER -> {
                mFooter = child
                mFooterRefreshable = child as? Refreshable
            }
            LayoutParams.ROLE_CONTENT -> {
                mContent = child
            }
        }
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        when ((child.layoutParams as LayoutParams).role) {
            LayoutParams.ROLE_HEADER -> {
                mHeader = null
                mHeaderRefreshable = null
            }
            LayoutParams.ROLE_FOOTER -> {
                mFooter = null
                mFooterRefreshable = null
            }
            LayoutParams.ROLE_CONTENT -> {
                mContent = null
            }
        }
    }

    override fun generateDefaultLayoutParams() =
        LayoutParams(MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet?) = LayoutParams(context, attrs)
    override fun generateLayoutParams(p: ViewGroup.LayoutParams?) = LayoutParams(p)

    override fun startNestedScroll(axes: Int) = mChildHelper.startNestedScroll(axes)
    override fun isNestedScrollingEnabled() = mChildHelper.isNestedScrollingEnabled
    override fun setNestedScrollingEnabled(enabled: Boolean) {
        mChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun stopNestedScroll() = mChildHelper.stopNestedScroll()
    override fun hasNestedScrollingParent() = mChildHelper.hasNestedScrollingParent()
    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ) = mChildHelper.dispatchNestedScroll(
        dxConsumed,
        dyConsumed,
        dxUnconsumed,
        dyUnconsumed,
        offsetInWindow
    )

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ) = mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)

    override fun dispatchNestedFling(velocityX: Float, velocityY: Float, consumed: Boolean) =
        mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float) =
        mChildHelper.dispatchNestedPreFling(velocityX, velocityY)

    override fun onStartNestedScroll(child: View, target: View, nestedScrollAxes: Int) =
        child === mContent && if (mRefreshAxis == REFRESH_VERTICAL) nestedScrollAxes and ViewCompat.SCROLL_AXIS_VERTICAL == ViewCompat.SCROLL_AXIS_VERTICAL else nestedScrollAxes and ViewCompat.SCROLL_AXIS_HORIZONTAL == ViewCompat.SCROLL_AXIS_HORIZONTAL

    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) {
        super.onNestedScrollAccepted(child, target, axes)
    }

    override fun onStopNestedScroll(target: View) {
        //正在刷新状态或者即将处于刷新状态
        if (mState == STATE_REFRESHING || mState == STATE_SETTLE) {
            return
        }
        if (mTarget?.getContentSize(mRefreshVertical) ?: 0 > 0) {
            val contentSize = mLastTarget?.getContentSize(mRefreshVertical) ?: 0
            if (abs(mContentOffset) >= contentSize && mTarget?.isIndicator() == false) {
                //满足刷新条件
                mLastTarget?.onRelease(true)
                mTargetState = STATE_REFRESHING
                animateContentToPosition(if (mContentOffset > 0) contentSize else -contentSize)
            } else {
                //不满足刷新条件
                mLastTarget?.onRelease(false)
                animateResetContent()
            }
        } else {
            //仅仅展示
            animateResetContent()
        }
    }


    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        if (mRefreshAxis == REFRESH_VERTICAL) {
            if (0 != dyUnconsumed) {
                mScroller.abortAnimation()
                mState = if (dyUnconsumed > 0) STATE_DRAG_FROM_BOTTOM else STATE_DRAG_FROM_TOP
                prepareTarget(0 > dyUnconsumed)
                offsetContent(mContentOffset + resolveOffset(dyConsumed))
            }
        } else {
            if (0 != dxUnconsumed) {
                mScroller.abortAnimation()
                mState = if (dxUnconsumed > 0) STATE_DRAG_FROM_RIGHT else STATE_DRAG_FROM_LEFT
                prepareTarget(0 > dxUnconsumed)
                offsetContent(mContentOffset + resolveOffset(dxUnconsumed))
            }
        }

    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        if (mState in STATE_DRAG) {
            offsetContent(mContentOffset + resolveOffset(if (mRefreshVertical) dy else dx))
        }

        if (mState in STATE_REFRESH_PROCESS) {
            if (mRefreshAxis == REFRESH_VERTICAL) {
                consumed[1] = dy
            } else {
                consumed[0] = dx
            }
        }
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ) = mState in STATE_REFRESH_PROCESS

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float) =
        mState in STATE_REFRESH_PROCESS

    override fun getNestedScrollAxes() =
        if (mRefreshAxis == REFRESH_VERTICAL) ViewCompat.SCROLL_AXIS_VERTICAL else ViewCompat.SCROLL_AXIS_HORIZONTAL

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        val y = event.y
        val x = event.x
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPressedY = y
                mPressedX = x
                startNestedScroll(if (mRefreshVertical) ViewCompat.SCROLL_AXIS_VERTICAL else ViewCompat.SCROLL_AXIS_HORIZONTAL)
            }
            MotionEvent.ACTION_MOVE -> {
                var deltaY = (mLastY - y).toInt()
                var deltaX = (mLastX - x).toInt()
                if (mRefreshVertical) {
                    if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                        deltaY -= mScrollConsumed[1]
                    }
                    if (mState == STATE_IDLE && abs(mPressedY - y) > mTouchSlop) {
                        mState = if (deltaY < 0) STATE_DRAG_FROM_TOP else STATE_DRAG_FROM_BOTTOM
                        prepareTarget(deltaY < 0)
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    if (mState == STATE_DRAG_FROM_TOP || mState == STATE_DRAG_FROM_BOTTOM) {
                        onNestedScroll(this, 0, 0, 0, deltaY)
                        //仅通知, 默认全部消耗
                        mScrollOffset[1] = mContentOffset
                        dispatchNestedScroll(0, deltaY, 0, 0, mScrollOffset)
                    }
                } else {
                    if (dispatchNestedPreScroll(deltaX, 0, mScrollConsumed, mScrollOffset)) {
                        deltaX -= mScrollConsumed[0]
                    }
                    if (mState == STATE_IDLE && abs(mPressedX - x) > mTouchSlop) {
                        mState = if (deltaX < 0) STATE_DRAG_FROM_LEFT else STATE_DRAG_FROM_RIGHT
                        prepareTarget(deltaX < 0)
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    if (mState == STATE_DRAG_FROM_LEFT || mState == STATE_DRAG_FROM_RIGHT) {
                        onNestedScroll(this, 0, 0, deltaX, 0)
                        //仅通知, 默认全部消耗
                        mScrollOffset[0] = mContentOffset
                        dispatchNestedScroll(0, deltaY, 0, 0, mScrollOffset)
                    }
                }
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_OUTSIDE, MotionEvent.ACTION_UP -> {
                stopNestedScroll()
                onStopNestedScroll(this)
            }
        }
        mLastY = y
        mLastX = x
        return true
    }

    override fun computeScroll() {
        if (mState == STATE_SETTLE) {
            if (mScroller.computeScrollOffset()) {
                offsetContent(mScroller.currY)
            } else {
                mState = mTargetState
                when (mState) {
                    STATE_IDLE -> mLastTarget?.onReset()
                    STATE_REFRESHING -> {
                        mInternalOnRefreshListener?.onRefreshStart(mContentOffset < 0)
                        mOnRefreshListener?.onRefreshStart(mContentOffset < 0)
                    }
                }
                mTargetState = STATE_IDLE
            }
        }
    }

    private fun offsetContent(offset: Int) {
        mLastTarget?.let {
            val offsetDelta: Float = -offset * 1.0f / it.getContentSize(mRefreshVertical)
            it.onOffset(if (offsetDelta.isInfinite()) 0f else offsetDelta)
        }
        mOnRefreshStateChangeListener?.onContentOffset(offset)
        if (mRefreshVertical) {
            scrollTo(0, offset)
        } else {
            scrollTo(offset, 0)
        }
        mContentOffset = offset
        ViewCompat.postInvalidateOnAnimation(this)
    }

    private fun prepareTarget(isFromHeader: Boolean) {
        if (isFromHeader) {
            if (null == mTarget) {
                mLastTarget = mHeaderRefreshable ?: mHeader?.let { ViewRefreshable(it) }
                mTarget = mLastTarget
            }
        } else {
            if (null == mTarget) {
                mLastTarget = mFooterRefreshable ?: mFooter?.let { ViewRefreshable(it) }
                mTarget = mLastTarget
            }
        }
    }

    private fun resolveOffset(offset: Int) = (offset * DRAG_THRESHOLD).toInt()

    private fun completeRefreshImmediately() {
        if (mState in STATE_DRAG_FROM_TOP..STATE_REFRESHING) {
            animateResetContent()
        }
    }

    /**
     * Complete the refresh action
     */
    fun completeRefresh() {
        val observable = RefreshObservable(mObserver)
        var intercept = mInternalOnRefreshListener?.onRefreshComplete(observable) ?: false
        if (!intercept) {
            intercept = mOnRefreshListener?.onRefreshComplete(observable) ?: intercept
        }
        if (!intercept) {
            completeRefreshImmediately()
        }
    }

    private fun animateResetContent() {
        mTarget = null
        mTargetState = STATE_IDLE
        animateContentToPosition(0)
    }

    private fun animateContentToPosition(targetOffset: Int) {
        if (targetOffset != mContentOffset) {
            mState = STATE_SETTLE
            mScroller.abortAnimation()
            mScroller.startScroll(0, mContentOffset, 0, targetOffset - mContentOffset, 300)
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    /**
     *  Trigger refresh action
     */
    fun setRefresh(isFromHeader: Boolean) {
        if (mState != STATE_REFRESHING) {
            prepareTarget(isFromHeader)
            mTarget?.let {
                if (!it.isIndicator()) {
                    mState = STATE_REFRESHING
                    mTargetState = STATE_REFRESHING
                    it.onRelease(true)
                    animateContentToPosition(
                        if (isFromHeader) -it.getContentSize(mRefreshVertical) else it.getContentSize(
                            mRefreshVertical
                        )
                    )
                }
            }
        }
    }

    fun setOnRefreshListener(onRefreshListener: OnRefreshListener?) {
        mOnRefreshListener = onRefreshListener
    }

    fun setOnRefreshStateChangeListener(onRefreshStateChangeListener: OnRefreshStateChangeListener?) {
        mOnRefreshStateChangeListener = onRefreshStateChangeListener
    }

    override fun onSaveInstanceState(): Parcelable? =
        SavedState(super.onSaveInstanceState()).apply {
            mState = this@RefreshLayout.mState
            mContentOffset = this@RefreshLayout.mContentOffset
            mLastTarget = when (this@RefreshLayout.mLastTarget) {
                mHeader -> TARGET_HEADER
                mFooter -> TARGET_FOOTER
                else -> TARGET_NONE
            }
        }

    override fun onRestoreInstanceState(state: Parcelable?) = with(state as SavedState) {
        super.onRestoreInstanceState(superState)
        this@RefreshLayout.mState = mState
        this@RefreshLayout.mContentOffset = mContentOffset
        this@RefreshLayout.mLastTarget = when (mLastTarget) {
            TARGET_HEADER -> mHeaderRefreshable
            TARGET_FOOTER -> mFooterRefreshable
            else -> null
        }
        offsetContent(mContentOffset)
        if (mState == STATE_REFRESHING) {
            prepareTarget(0 > mContentOffset)
        }
    }


    class LayoutParams : MarginLayoutParams {
        /**
         * element role in this layout
         * value options
         * [ROLE_HEADER]
         * [ROLE_CONTENT]
         * [ROLE_FOOTER]
         */
        var role = 0

        /**
         * elements gravity
         * for [ROLE_HEADER],[ROLE_FOOTER]
         * supported values
         * [Gravity.LEFT]
         * [Gravity.RIGHT]
         * [Gravity.START]
         * [Gravity.END]
         * [Gravity.TOP]
         * [Gravity.BOTTOM]
         * [Gravity.CENTER_HORIZONTAL]
         * [Gravity.CENTER_VERTICAL]
         *
         * for [ROLE_CONTENT]
         * supported values
         * [Gravity.LEFT]
         * [Gravity.RIGHT]
         * [Gravity.START]
         * [Gravity.END]
         * [Gravity.TOP]
         * [Gravity.BOTTOM]
         * [Gravity.CENTER_HORIZONTAL]
         * [Gravity.CENTER_VERTICAL]
         * [Gravity.CENTER]
         *
         * 重力，对于Header和Footer只支持[Gravity.LEFT]，[Gravity.RIGHT]，[Gravity.CENTER_HORIZONTAL]，[Gravity.TOP]，[Gravity.BOTTOM]，[Gravity.CENTER_VERTICAL]
         */
        var gravity = Gravity.NO_GRAVITY

        constructor(
            width: Int,
            height: Int,
            role: Int = 0,
            gravity: Int = Gravity.NO_GRAVITY
        ) : super(width, height) {
            this.role = role
            this.gravity = gravity
        }

        constructor(source: ViewGroup.LayoutParams?) : super(source)

        constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
            val a = c.obtainStyledAttributes(attrs, R.styleable.RefreshLayout_Layout)
            role = a.getInt(R.styleable.RefreshLayout_Layout_layout_refresh_role, 0)
            gravity = a.getInt(
                R.styleable.RefreshLayout_Layout_android_layout_gravity,
                Gravity.NO_GRAVITY
            )
            a.recycle()
            require(role in ROLE_HEADER..ROLE_CONTENT) { "You must set a layout_refresh_role attribute to your view" }
        }

        companion object {
            const val ROLE_HEADER = 1
            const val ROLE_FOOTER = 2
            const val ROLE_CONTENT = 3
        }
    }

    private class SavedState : BaseSavedState {
        var mState: Int = 0
        var mContentOffset: Int = 0
        var mLastTarget: Int = 0

        constructor(source: Parcel) : super(source) {
            with(source) {
                mState = readInt()
                mContentOffset = readInt()
                mLastTarget = readInt()
            }
        }

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel, flags: Int) = with(out) {
            super.writeToParcel(out, flags)
            writeInt(mState)
            writeInt(mContentOffset)
            writeInt(mLastTarget)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel) = SavedState(source)

                override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
            }
        }
    }

    companion object {
        const val DRAG_THRESHOLD = 0.5f

        const val STATE_IDLE = 0
        const val STATE_DRAG_FROM_TOP = 1
        const val STATE_DRAG_FROM_BOTTOM = 2
        const val STATE_DRAG_FROM_LEFT = 3
        const val STATE_DRAG_FROM_RIGHT = 4
        const val STATE_REFRESHING = 5
        const val STATE_SETTLE = 6

        val STATE_DRAG = STATE_DRAG_FROM_TOP..STATE_DRAG_FROM_RIGHT
        val STATE_REFRESH_PROCESS = STATE_DRAG_FROM_TOP..STATE_SETTLE

        const val TARGET_NONE = 0
        const val TARGET_HEADER = 1
        const val TARGET_FOOTER = 2

        private const val REFRESH_VERTICAL = 1
        private const val REFRESH_HORIZONTAL = 2
    }
}