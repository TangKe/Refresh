package ke.tang.refresh

import android.animation.ValueAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntegerRes
import androidx.annotation.RawRes
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import java.io.InputStream
import kotlin.math.abs
import kotlin.math.min

/**
 * Provide a refreshable view which contain lottie features
 * you can provide two animation resource
 * 1. refresh animation - a animation played when view in refreshing state, will play infinite until [RefreshLayout.completeRefresh] been invoked
 * 2. pull animation - a animation played when user pull, the animation process this controlled by pull distance
 */
class AnimationRefreshView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.animationRefreshHeaderViewStyle,
        defStyleRes: Int = R.style.Widget_AnimationRefreshHeaderView
) : LottieAnimationView(context, attrs, defStyleAttr), Refreshable {
    private var mStatus: Int = STATUS_IDLE
        set(value) {
            when (value) {
                STATUS_IDLE -> {
                    if (null != mPullComposition) {
                        setComposition(mPullComposition!!)
                    }
                    repeatCount = 0
                    pauseAnimation()
                }
                STATUS_DRAG -> {
                }
                STATUS_REFRESHING -> {
                    if (null != mRefreshComposition) {
                        setComposition(mRefreshComposition!!)
                    }
                    repeatCount = ValueAnimator.INFINITE
                    playAnimation()
                }
            }
            field = value
        }

    private var mRefreshComposition: LottieComposition? = null
        set(value) {
            field = value
            mStatus = mStatus
        }
    private var mPullComposition: LottieComposition? = null
        set(value) {
            field = value
            mStatus = mStatus
        }

    init {
        with(context.obtainStyledAttributes(attrs, R.styleable.AnimationRefreshView, defStyleAttr, defStyleRes)) {
            setPullAnimation(getResourceId(R.styleable.AnimationRefreshView_pullAnimation, View.NO_ID))
            setRefreshAnimation(getResourceId(R.styleable.AnimationRefreshView_refreshAnimation, View.NO_ID))
            recycle()
        }
    }

    /**
     * Set a pull animation resource
     */
    fun setPullAnimation(@RawRes res: Int) {
        if (NO_ID != res) {
            setPullAnimationStream(resources.openRawResource(res), Integer.toHexString(res))
        } else {
            mPullComposition = null
        }
    }

    /**
     * Set a refresh animation resource
     */
    fun setRefreshAnimation(@RawRes res: Int) {
        if (NO_ID != res) {
            setRefreshAnimationStream(resources.openRawResource(res), Integer.toHexString(res))
        } else {
            mRefreshComposition = null
        }
    }

    private fun setPullAnimationStream(stream: InputStream, cacheKey: String? = null) {
        LottieCompositionFactory.fromJsonInputStream(stream, cacheKey)?.addListener { mPullComposition = it }
    }

    private fun setRefreshAnimationStream(stream: InputStream, cacheKey: String? = null) {
        LottieCompositionFactory.fromJsonInputStream(stream, cacheKey).addListener { mRefreshComposition = it }
    }

    override fun isIndicator() = false

    override fun onOffset(delta: Float) {
        if (mStatus == STATUS_IDLE) {
            mStatus = STATUS_DRAG
        }

        if (mStatus == STATUS_DRAG) {
            progress = min(1f, abs(delta))
        }
    }

    override fun onRelease(isTrigger: Boolean) {
        if (isTrigger) {
            mStatus = STATUS_REFRESHING
        }
    }

    override fun onReset() {
        mStatus = STATUS_IDLE
    }

    override fun getContentSize(isRefreshVertical: Boolean) = if (isRefreshVertical) {
        height + with(layoutParams as ViewGroup.MarginLayoutParams) { topMargin + bottomMargin }
    } else {
        width + with(layoutParams as ViewGroup.MarginLayoutParams) { leftMargin + rightMargin }
    }

    override fun onSaveInstanceState(): Parcelable? = SavedState(super.onSaveInstanceState()).apply {
        mStatus = this@AnimationRefreshView.mStatus
    }

    override fun onRestoreInstanceState(state: Parcelable?) = with(state as SavedState) {
        super.onRestoreInstanceState(superState)
        this@AnimationRefreshView.mStatus = mStatus
    }

    private class SavedState : BaseSavedState {
        var mStatus: Int = 0

        constructor(superState: Parcelable?) : super(superState)
        constructor(source: Parcel) : super(source) {
            with(source) {
                mStatus = source.readInt()
            }
        }

        override fun writeToParcel(out: Parcel, flags: Int) = with(out) {
            super.writeToParcel(out, flags)
            writeInt(mStatus)
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
        const val STATUS_IDLE = 0
        const val STATUS_DRAG = 1
        const val STATUS_REFRESHING = 2
    }
}