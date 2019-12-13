package ke.tang.refresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.ViewCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import java.util.Observable;
import java.util.Observer;

/**
 * 利用嵌套滚动机制实现下拉刷新
 */

public class RefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {
    private final static int STATE_IDLE = 0;
    private final static int STATE_DRAG_FROM_TOP = 1;
    private final static int STATE_DRAG_FROM_BOTTOM = 2;
    private final static int STATE_REFRESHING = 3;
    private final static int STATE_SETTLE = 4;

    private final static int TARGET_NONE = 0;
    private final static int TARGET_HEADER = 1;
    private final static int TARGET_FOOTER = 2;

    private final static float DRAG_THRESHOLD = 0.5f;

    private Scroller mScroller;

    private Refreshable mHeaderRefreshable;
    private Refreshable mFooterRefreshable;

    private View mHeader;
    private View mFooter;
    private View mContent;

    private int mState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;

    private int mContentOffset;

    private Refreshable mTarget;
    private Refreshable mLastTarget;

    private OnRefreshListener mOnRefreshListener;
    private OnRefreshListener mInternalOnRefreshListener;
    private OnRefreshStateChangeListener mOnRefreshStateChangeListener;

    private float mTouchSlop;
    private float mPressedY;
    private float mLastY;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];

    private NestedScrollingChildHelper mChildHelper = new NestedScrollingChildHelper(this);

    private Observer mObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            completeRefreshImmediately();
        }
    };

    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.refreshLayoutStyle);
    }

    public RefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScroller = new Scroller(context, new FastOutSlowInInterpolator());
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int count = getChildCount();

        int targetWidth = 0, targetHeight = 0;
        for (int index = 0; index < count; index++) {
            View child = getChildAt(index);
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            switch (layoutParams.role) {
                case LayoutParams.ROLE_CONTENT:
                    targetHeight = child.getMeasuredHeight();
                case LayoutParams.ROLE_FOOTER:
                    break;
                case LayoutParams.ROLE_HEADER:
                    break;
                default:
                    targetWidth = Math.max(targetWidth, child.getMeasuredWidth());
                    break;
            }
        }
        setMeasuredDimension(resolveSize(targetWidth, widthMeasureSpec), resolveSize(targetHeight, heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int count = getChildCount();
        for (int index = 0; index < count; index++) {
            View child = getChildAt(index);

            LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
            switch (layoutParams.role) {
                case LayoutParams.ROLE_CONTENT:
                    layoutContent(child);
                    break;
                case LayoutParams.ROLE_HEADER:
                    layoutHeader(child);
                    break;
                case LayoutParams.ROLE_FOOTER:
                    layoutFooter(child);
                    break;
            }
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        LayoutParams layoutParams = (LayoutParams) params;
        switch (layoutParams.role) {
            case LayoutParams.ROLE_CONTENT:
                mContent = child;
                break;
            case LayoutParams.ROLE_FOOTER:
                mFooter = child;
                if (child instanceof Refreshable) {
                    mFooterRefreshable = (Refreshable) child;
                }
                break;
            case LayoutParams.ROLE_HEADER:
                mHeader = child;
                if (child instanceof Refreshable) {
                    mHeaderRefreshable = (Refreshable) child;
                }
                break;
        }
    }

    private void layoutFooter(View footer) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        final int childMeasuredWidth = footer.getMeasuredWidth();
        final int childMeasuredHeight = footer.getMeasuredHeight();
        LayoutParams layoutParams = (LayoutParams) footer.getLayoutParams();

        int left = 0, top = 0, right = 0, bottom = 0;
        switch (layoutParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            default:
            case Gravity.LEFT:
                left = paddingLeft + layoutParams.leftMargin;
                right = left + childMeasuredWidth;
                break;
            case Gravity.CENTER_HORIZONTAL:
                left = (getWidth() - paddingLeft - paddingRight - childMeasuredWidth - layoutParams.leftMargin - layoutParams.rightMargin) / 2;
                right = left + childMeasuredWidth;
                break;
            case Gravity.RIGHT:
                right = getWidth() - paddingRight - layoutParams.rightMargin;
                left = right - childMeasuredWidth;
                break;
        }

        top = getHeight() - paddingBottom + layoutParams.topMargin;
        bottom = top + childMeasuredHeight;

        footer.layout(left, top, right, bottom);
    }

    private void layoutHeader(View header) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        final int childMeasuredWidth = header.getMeasuredWidth();
        final int childMeasuredHeight = header.getMeasuredHeight();
        LayoutParams layoutParams = (LayoutParams) header.getLayoutParams();

        int left = 0, top = 0, right = 0, bottom = 0;
        switch (layoutParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            default:
            case Gravity.LEFT:
                left = paddingLeft + layoutParams.leftMargin;
                right = left + childMeasuredWidth;
                break;
            case Gravity.CENTER_HORIZONTAL:
                left = (getWidth() - paddingLeft - paddingRight - childMeasuredWidth - layoutParams.leftMargin - layoutParams.rightMargin) / 2;
                right = left + childMeasuredWidth;
                break;
            case Gravity.RIGHT:
                right = getWidth() - paddingRight - layoutParams.rightMargin;
                left = right - childMeasuredWidth;
                break;
        }

        bottom = paddingTop - layoutParams.bottomMargin;
        top = bottom - childMeasuredHeight;

        header.layout(left, top, right, bottom);
    }

    private void layoutContent(View content) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();
        final int childMeasuredWidth = content.getMeasuredWidth();
        final int childMeasuredHeight = content.getMeasuredHeight();
        LayoutParams layoutParams = (LayoutParams) content.getLayoutParams();

        int left = 0, top = 0, right = 0, bottom = 0;
        switch (layoutParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            default:
            case Gravity.LEFT:
                left = paddingLeft + layoutParams.leftMargin;
                right = left + childMeasuredWidth;
                break;
            case Gravity.CENTER_HORIZONTAL:
                left = (getWidth() - paddingLeft - paddingRight - childMeasuredWidth - layoutParams.leftMargin - layoutParams.rightMargin) / 2;
                right = left + childMeasuredWidth;
                break;
            case Gravity.RIGHT:
                right = getWidth() - paddingRight - layoutParams.rightMargin;
                left = right - childMeasuredWidth;
                break;
        }

        switch (layoutParams.gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            default:
            case Gravity.TOP:
                top = paddingTop + layoutParams.topMargin;
                bottom = top + childMeasuredHeight;
                break;
            case Gravity.CENTER_VERTICAL:
                top = (getHeight() - paddingTop - paddingBottom - layoutParams.topMargin - layoutParams.bottomMargin) / 2;
                bottom = top + childMeasuredHeight;
                break;
            case Gravity.BOTTOM:
                bottom = getHeight() - paddingBottom - layoutParams.bottomMargin;
                top = bottom - childMeasuredHeight;
                break;
        }

        content.layout(left, top, right, bottom);
    }

    //TODO
    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
                                        int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) == ViewCompat.SCROLL_AXIS_VERTICAL && child == mContent;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
    }

    @Override
    public void onStopNestedScroll(View target) {
        //正在刷新状态或者即将处于刷新状态
        if (mState == STATE_REFRESHING || mState == STATE_SETTLE) {
            return;
        }
        if (null != mTarget && mTarget.getContentSize() > 0) {

            if (Math.abs(mContentOffset) >= mLastTarget.getContentSize()) {
                //满足刷新条件
                mLastTarget.onRelease(true);
                if (!mTarget.isIndicator()) {
                    mTargetState = STATE_REFRESHING;
                    animateContentToPosition(mContentOffset > 0 ? mLastTarget.getContentSize() : -mLastTarget.getContentSize());
                } else {
                    animateResetContent();
                }
            } else {
                //不满足刷新条件
                mLastTarget.onRelease(false);
                animateResetContent();
            }
        } else {
            //仅仅展示
            animateResetContent();
        }
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
                               int dxUnconsumed, int dyUnconsumed) {
        if (0 == dyUnconsumed) {
            return;
        }
        mScroller.abortAnimation();
        int fixedOffset = (int) (mContentOffset + resolveOffset(dyUnconsumed));

        setState(dyUnconsumed > 0 ? STATE_DRAG_FROM_BOTTOM : STATE_DRAG_FROM_TOP);
        prepareTarget(0 > dyUnconsumed);

        offsetContent(fixedOffset);
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        switch (mState) {
            case STATE_DRAG_FROM_BOTTOM:
            case STATE_DRAG_FROM_TOP:
                if (STATE_DRAG_FROM_BOTTOM == mState) {
                    //>0
                    if (0 > dy && mContentOffset + resolveOffset(dy) < 0) {
                        offsetContent(0);
                        setState(STATE_IDLE);
                        return;
                    }
                } else {
                    //<0
                    if (0 < dy && mContentOffset + resolveOffset(dy) > 0) {
                        offsetContent(0);
                        setState(STATE_IDLE);
                        return;
                    }
                }
                offsetContent((int) (mContentOffset + resolveOffset(dy)));
            case STATE_REFRESHING:
            case STATE_SETTLE:
                consumed[1] = dy;
                break;
        }
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return mState == STATE_REFRESHING || mState == STATE_DRAG_FROM_TOP || mState == STATE_DRAG_FROM_BOTTOM || mState == STATE_SETTLE;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return mState == STATE_REFRESHING || mState == STATE_DRAG_FROM_TOP || mState == STATE_DRAG_FROM_BOTTOM || mState == STATE_SETTLE;
    }

    @Override
    public int getNestedScrollAxes() {
        return ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        final float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPressedY = y;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaY = (int) (mLastY - y);
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1];
                }

                if (mState == STATE_IDLE && Math.abs(mPressedY - y) > mTouchSlop) {
                    mState = deltaY < 0 ? STATE_DRAG_FROM_TOP : STATE_DRAG_FROM_BOTTOM;
                    prepareTarget(deltaY < 0);
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                if (mState == STATE_DRAG_FROM_TOP || mState == STATE_DRAG_FROM_BOTTOM) {
                    onNestedScroll(this, 0, 0, 0, deltaY);
                    //仅通知, 默认全部消耗
                    mScrollOffset[1] = mContentOffset;
                    dispatchNestedScroll(0, deltaY, 0, 0, mScrollOffset);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_UP:
                stopNestedScroll();
                onStopNestedScroll(this);
                break;
        }
        mLastY = y;
        return true;
    }

    @Override
    public void computeScroll() {
        if (mState == STATE_SETTLE) {
            if (mScroller.computeScrollOffset()) {
                offsetContent(mScroller.getCurrY());
            } else {
                mState = mTargetState;
                switch (mState) {
                    case STATE_IDLE:
                        if (null != mLastTarget) {
                            mLastTarget.onReset();
                        }
                        break;
                    case STATE_REFRESHING:
                        if (null != mInternalOnRefreshListener) {
                            mInternalOnRefreshListener.onRefreshStart(mContentOffset < 0);
                        }

                        if (null != mOnRefreshListener) {
                            mOnRefreshListener.onRefreshStart(mContentOffset < 0);
                        }
                        break;
                }
                mTargetState = STATE_IDLE;
            }
        }
    }

    private float resolveOffset(float offset) {
        return offset * DRAG_THRESHOLD;
    }

    /**
     * 完成刷新操作
     */
    public void completeRefresh() {
        RefreshObservable observable = new RefreshObservable(mObserver);

        boolean intercept = false;
        if (null != mInternalOnRefreshListener) {
            intercept = mInternalOnRefreshListener.onRefreshComplete(observable);
        }

        if (!intercept && null != mOnRefreshListener) {
            intercept = mOnRefreshListener.onRefreshComplete(observable);
        }

        if (!intercept) {
            completeRefreshImmediately();
        }
    }

    /**
     * 从代码触发刷新操作
     *
     * @param isFromHeader
     */
    public void setRefresh(boolean isFromHeader) {
        if (mState != STATE_REFRESHING) {
            setState(STATE_SETTLE);
            mTargetState = STATE_REFRESHING;
            prepareTarget(isFromHeader);
            if (null != mLastTarget && !mLastTarget.isIndicator()) {
                setState(STATE_REFRESHING);
                mLastTarget.onRelease(true);
                animateContentToPosition(isFromHeader ? -mTarget.getContentSize() : mTarget.getContentSize());
            }
        }
    }

    private void prepareTarget(boolean isFromHeader) {
        if (isFromHeader) {
            if (null == mTarget) {
                mTarget = mLastTarget = null != mHeaderRefreshable ? mHeaderRefreshable : (null == mHeader ? null : new ViewRefreshable(mHeader));
            }
        } else {
            if (null == mTarget) {
                mTarget = mLastTarget = null != mFooterRefreshable ? mFooterRefreshable : (null == mFooter ? null : new ViewRefreshable(mFooter));
            }
        }
    }

    private void completeRefreshImmediately() {
        if (mState == STATE_REFRESHING || mState == STATE_DRAG_FROM_BOTTOM || mState == STATE_DRAG_FROM_TOP) {
            animateResetContent();
        }
    }

    private void animateResetContent() {
        mTarget = null;
        mTargetState = STATE_IDLE;
        animateContentToPosition(0);
    }

    private void animateContentToPosition(int targetOffset) {
        if (targetOffset != mContentOffset) {
            setState(STATE_SETTLE);
            mScroller.abortAnimation();
            mScroller.startScroll(0, mContentOffset, 0, targetOffset - mContentOffset, 300);
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    private void offsetContent(int offset) {
        if (null != mLastTarget) {
            float offsetDelta = -offset * 1.0f / mLastTarget.getContentSize();
            mLastTarget.onOffset(offsetDelta);
        }
        if (null != mOnRefreshStateChangeListener) {
            mOnRefreshStateChangeListener.onContentOffset(offset);
        }
        scrollTo(0, offset);
        mContentOffset = offset;
        ViewCompat.postInvalidateOnAnimation(this);
    }

    private void setState(int state) {
        mState = state;
    }

    /**
     * 设置刷新监听
     *
     * @param onRefreshListener
     */
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    /**
     * 设置刷新状态监听
     *
     * @param onRefreshStateChangeListener
     */
    public void setOnRefreshStateChangeListener(OnRefreshStateChangeListener onRefreshStateChangeListener) {
        mOnRefreshStateChangeListener = onRefreshStateChangeListener;
    }

    /**
     * 设置内部刷新监听
     *
     * @param onRefreshListener
     * @hide
     */
    void setInternalOnRefreshListener(OnRefreshListener onRefreshListener) {
        mInternalOnRefreshListener = onRefreshListener;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        mState = savedState.mState;
        mContentOffset = savedState.mContentOffset;
        switch (savedState.mLastTarget) {
            case TARGET_HEADER:
                mLastTarget = mHeaderRefreshable;
                break;
            case TARGET_FOOTER:
                mLastTarget = mFooterRefreshable;
                break;
        }
        offsetContent(mContentOffset);

        if (STATE_REFRESHING == mState) {
            prepareTarget(0 > mContentOffset);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.mState = mState;
        state.mContentOffset = mContentOffset;
        state.mLastTarget = null == mLastTarget ? TARGET_NONE : (mLastTarget == mHeader ? TARGET_HEADER : TARGET_FOOTER);
        return state;
    }

    /**
     * 布局参数
     */
    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        public static final int ROLE_HEADER = 1;
        public static final int ROLE_FOOTER = 2;
        public static final int ROLE_CONTENT = 3;

        /**
         * 角色，可选值{@link #ROLE_CONTENT}，{@link #ROLE_FOOTER}，{@link #ROLE_HEADER}
         */
        public int role;
        /**
         * 重力，对于Header和Footer只支持{@link Gravity#LEFT}，{@link Gravity#RIGHT}，{@link Gravity#CENTER_HORIZONTAL}
         */
        public int gravity;

        public LayoutParams(int width, int height, int role, int gravity) {
            super(width, height);
            this.role = role;
            this.gravity = gravity;
        }

        public LayoutParams(int width, int height, int role) {
            super(width, height);
            this.role = role;
        }

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.RefreshLayout_LayoutParams);
            role = a.getInt(R.styleable.RefreshLayout_LayoutParams_layout_refresh_role, 0);
            gravity = a.getInt(R.styleable.RefreshLayout_LayoutParams_android_layout_gravity, Gravity.NO_GRAVITY);
            a.recycle();
            if (role <= 0 && role > 3) {
                throw new IllegalArgumentException("You must set a layout_role attribute to your view");
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

    /**
     * 当View需要作为下拉刷新的Header或者Footer的时候同时需要根据下拉事件变化作出相应调整的时候需要实现该接口
     */
    public interface Refreshable {
        /**
         * 用于告知是否是指示器, 如果返回true不会触发刷新动作, 只会在用户下拉/上拉的情况展示
         *
         * @return
         */
        boolean isIndicator();

        /**
         * 在内容偏移有位置变化的时候触发
         *
         * @param delta 取值从0-1+, 如果大于1, 则表明用户下拉已经超过本身尺寸
         */
        void onOffset(float delta);

        /**
         * 当用户释放双手的时候触发
         *
         * @param isTrigger 如果偏移量超过尺寸, 为true, 如果偏移量小于尺寸, 为false
         */
        void onRelease(boolean isTrigger);

        /**
         * 当内容归位的时候触发
         */
        void onReset();

        /**
         * 告知尺寸
         *
         * @return
         */
        int getContentSize();
    }

    /**
     * 下拉刷新状态变化回调
     */
    public interface OnRefreshListener {
        /**
         * 当触发刷新时调用该方法
         *
         * @param isFromTop true表明是下拉，false表明上拉
         */
        void onRefreshStart(boolean isFromTop);

        /**
         * 在刷新完成后回调该方法，可以做延迟完成刷新操作，比如：还需要播放一段动画，或者提示一段通知后再关闭刷新
         *
         * @param observable 如果需要拦截，可以保留该引用，在适当的时机调用{@link RefreshObservable#completeRefresh()}方法完成被拦截的关闭刷新行为
         * @return true拦截，false不拦截
         */
        boolean onRefreshComplete(RefreshObservable observable);
    }

    /**
     * 状态变化回调
     */
    public interface OnRefreshStateChangeListener {
        /**
         * 当下拉或者上拉偏移量变化会调用该方法
         *
         * @param offset 偏移的像素值
         */
        void onContentOffset(int offset);
    }

    /**
     * 刷新操作
     */
    public class RefreshObservable extends Observable {
        RefreshObservable(Observer observer) {
            addObserver(observer);
        }

        /**
         * 完成刷新操作
         */
        public void notifyRefreshComplete() {
            setChanged();
            notifyObservers();
        }
    }

    private static class SavedState extends BaseSavedState {
        private int mState;
        private int mContentOffset;
        private int mLastTarget;

        public SavedState(Parcel source) {
            super(source);
            mState = source.readInt();
            mContentOffset = source.readInt();
            mLastTarget = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mState);
            out.writeInt(mContentOffset);
            out.writeInt(mLastTarget);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
