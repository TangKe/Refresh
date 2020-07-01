package ke.tang.refresh

import android.view.View
import android.view.ViewGroup

internal class ViewRefreshable(private val mView: View) : Refreshable {
    override fun isIndicator() = false

    override fun onOffset(delta: Float) {
    }

    override fun onRelease(isTrigger: Boolean) {
    }

    override fun onReset() {
    }

    override fun getContentSize(isRefreshVertical: Boolean) = if (isRefreshVertical) {
        mView.height + with(mView.layoutParams as ViewGroup.MarginLayoutParams) { topMargin + bottomMargin }
    } else {
        mView.width + with(mView.layoutParams as ViewGroup.MarginLayoutParams) { leftMargin + rightMargin }
    }
}