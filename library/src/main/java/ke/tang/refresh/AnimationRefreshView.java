package ke.tang.refresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.RawRes;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.OnCompositionLoadedListener;

import java.io.InputStream;

/**
 * 支持Lottie动画的刷新View，可以用作Header或者Footer
 */
public class AnimationRefreshView extends LottieAnimationView implements RefreshLayout.Refreshable {
    private final static int STATUS_IDLE = 0;
    private final static int STATUS_DRAG = 1;
    private final static int STATUS_REFRESHING = 2;

    private int mStatus;

    private LottieComposition mRefreshComposition;
    private LottieComposition mPullComposition;

    public AnimationRefreshView(Context context) {
        this(context, null);
    }

    public AnimationRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.animationRefreshHeaderViewStyle);
    }

    public AnimationRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnimationRefreshView, defStyleAttr, R.style.Widget_AnimationRefreshHeaderView);
        setPullAnimation(a.getResourceId(R.styleable.AnimationRefreshView_pullAnimation, 0));
        setRefreshAnimation(a.getResourceId(R.styleable.AnimationRefreshView_refreshAnimation, 0));
        a.recycle();
    }

    @Override
    public boolean isIndicator() {
        return false;
    }

    @Override
    public void onOffset(float delta) {
        if (mStatus == STATUS_IDLE) {
            setStatus(STATUS_DRAG);
        }

        if (mStatus == STATUS_DRAG) {
            setProgress(Math.min(1, Math.abs(delta)));
        }
    }

    @Override
    public void onRelease(boolean isTrigger) {
        if (isTrigger) {
            setStatus(STATUS_REFRESHING);
        }
    }

    @Override
    public void onReset() {
        setStatus(STATUS_IDLE);
    }

    private void setStatus(int status) {
        switch (status) {
            case STATUS_IDLE:
                loop(false);
                if (null != mPullComposition) {
                    setComposition(mPullComposition);
                }
                pauseAnimation();
                break;
            case STATUS_DRAG:
                break;
            case STATUS_REFRESHING:
                if (null != mRefreshComposition) {
                    setComposition(mRefreshComposition);
                }
                loop(true);
                playAnimation();
                break;
        }
        mStatus = status;
    }

    @Override
    public int getContentSize() {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        return getHeight() + layoutParams.topMargin + layoutParams.bottomMargin;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState viewState = (SavedState) state;
        mStatus = viewState.mStatus;
        setStatus(mStatus);
        super.onRestoreInstanceState(viewState.getSuperState());
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);
        state.mStatus = mStatus;
        return state;
    }

    /**
     * 设置拖拽时的动画
     *
     * @param animation
     */
    public void setPullAnimation(LottieComposition animation) {
        setPullAnimationInternal(animation);
    }

    void setPullAnimationInternal(LottieComposition animation) {
        mPullComposition = animation;
        setStatus(mStatus);
    }

    /**
     * 设置刷新时动画
     *
     * @param animation
     */
    public void setRefreshAnimation(LottieComposition animation) {
        setRefreshAnimationInternal(animation);
    }

    void setRefreshAnimationInternal(LottieComposition animation) {
        mRefreshComposition = animation;
        setStatus(mStatus);
    }

    void setPullAnimationStream(InputStream animation) {
        LottieComposition.Factory.fromInputStream(animation, new OnCompositionLoadedListener() {
            @Override
            public void onCompositionLoaded(LottieComposition composition) {
                setPullAnimationInternal(composition);
            }
        });
    }

    void setRefreshAnimationStream(InputStream animation) {
        LottieComposition.Factory.fromInputStream(animation, new OnCompositionLoadedListener() {
            @Override
            public void onCompositionLoaded(LottieComposition composition) {
                setRefreshAnimationInternal(composition);
            }
        });
    }

    /**
     * 设置拖拽时的动画
     *
     * @param res
     */
    public void setPullAnimation(@RawRes int res) {
        if (0 < res) {
            setPullAnimationStream(getResources().openRawResource(res));
        } else {
            setPullAnimationInternal(null);
        }
    }

    /**
     * 设置刷新时动画
     *
     * @param res
     */
    public void setRefreshAnimation(@RawRes int res) {
        if (0 < res) {
            setRefreshAnimationStream(getResources().openRawResource(res));
        } else {
            setRefreshAnimationInternal(null);
        }
    }

    public static class SavedState implements Parcelable {
        private int mStatus;
        private Parcelable superState;

        public SavedState(Parcelable superState) {
            this.superState = superState != BaseSavedState.EMPTY_STATE ? superState : null;
        }

        public Parcelable getSuperState() {
            return superState;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mStatus);
            dest.writeParcelable(this.superState, flags);
        }

        protected SavedState(Parcel in) {
            this.mStatus = in.readInt();
            this.superState = in.readParcelable(LottieAnimationView.class.getClassLoader());
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
