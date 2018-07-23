package ke.tang.refresh;

import android.view.View;

class ViewRefreshable implements RefreshLayout.Refreshable {
    private View mTarget;

    public ViewRefreshable(View target) {
        mTarget = target;
    }

    @Override
    public boolean isIndicator() {
        return false;
    }

    @Override
    public void onOffset(float delta) {

    }

    @Override
    public void onRelease(boolean isTrigger) {

    }

    @Override
    public void onReset() {

    }

    @Override
    public int getContentSize() {
        return mTarget.getHeight();
    }
}
