package ke.tang.refresh;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.RawRes;

import com.airbnb.lottie.LottieComposition;

/**
 * 支持Lottie动画的刷新View，同时顶部包含一个可设置的文本，可以用作Header或者Footer
 */

public class TextAnimationRefreshView extends LinearLayout implements RefreshLayout.Refreshable {
    private TextView mText;
    private AnimationRefreshView mAnimation;

    public TextAnimationRefreshView(Context context) {
        this(context, null);
    }

    public TextAnimationRefreshView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.textAnimationRefreshHeaderViewStyle);
    }

    public TextAnimationRefreshView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.layout_text_animation_refresh_header_view, this);
        mText = findViewById(android.R.id.text1);
        mAnimation = findViewById(R.id.animation);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextAnimationRefreshView, defStyleAttr, R.style.Widget_TextAnimationRefreshHeaderView);
        setTextSize(TypedValue.COMPLEX_UNIT_PX, a.getDimension(R.styleable.TextAnimationRefreshView_android_textSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, context.getResources().getDisplayMetrics())));
        setTextColorInternal(a.getColor(R.styleable.TextAnimationRefreshView_android_textColor, Color.BLACK));
        setText(a.getText(R.styleable.TextAnimationRefreshView_android_text));
        mText.setSingleLine(a.getBoolean(R.styleable.TextAnimationRefreshView_android_singleLine, true));
        if (a.hasValue(R.styleable.TextAnimationRefreshView_pullAnimation)) {
            mAnimation.setPullAnimation(a.getResourceId(R.styleable.TextAnimationRefreshView_pullAnimation, 0));
        }
        if (a.hasValue(R.styleable.TextAnimationRefreshView_refreshAnimation)) {
            mAnimation.setPullAnimation(a.getResourceId(R.styleable.TextAnimationRefreshView_refreshAnimation, 0));
        }
        a.recycle();
    }


    /**
     * 设置拖拽时的动画
     *
     * @param animation
     */
    public void setPullAnimation(LottieComposition animation) {
        mAnimation.setPullAnimation(animation);
    }

    /**
     * 设置刷新时动画
     *
     * @param animation
     */
    public void setRefreshAnimation(LottieComposition animation) {
        mAnimation.setRefreshAnimation(animation);
    }

    /**
     * 设置拖拽时的动画
     *
     * @param res
     */
    public void setPullAnimation(@RawRes int res) {
        mAnimation.setPullAnimation(res);
    }

    /**
     * 设置拖拽时的动画
     *
     * @param res
     */
    public void setRefreshAnimation(@RawRes int res) {
        mAnimation.setRefreshAnimation(res);
    }

    public void setText(CharSequence text) {
        mText.setText(text);
    }

    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setTextSize(int unit, float size) {
        Context c = getContext();
        Resources r;

        if (c == null)
            r = Resources.getSystem();
        else
            r = c.getResources();

        setRawTextSize(TypedValue.applyDimension(
                unit, size, r.getDisplayMetrics()));
    }

    private void setRawTextSize(float size) {
        mText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    public void setTextColor(@ColorInt int color) {
        setTextColorInternal(color);
    }


    void setTextColorInternal(@ColorInt int color) {
        mText.setTextColor(color);
    }

    @Override
    public boolean isIndicator() {
        return false;
    }

    @Override
    public void onOffset(float delta) {
        mAnimation.onOffset(delta);
    }

    @Override
    public void onRelease(boolean isTrigger) {
        mAnimation.onRelease(isTrigger);
    }

    @Override
    public void onReset() {
        mAnimation.onReset();
    }

    @Override
    public int getContentSize() {
        return mAnimation.getContentSize();
    }

    public void setTextColorResource(@ColorRes int color) {
        setTextColor(getResources().getColorStateList(color));
    }

    public void setTextColor(ColorStateList color) {
        setTextColorInternal(color);
    }

    void setTextColorInternal(ColorStateList color) {
        mText.setTextColor(color);
    }
}
