package ke.tang.refresh.sample

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import ke.tang.refresh.OnRefreshListener
import ke.tang.refresh.RefreshObservable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), OnRefreshListener, View.OnClickListener, OnItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        refresh?.setOnRefreshListener(this)
        headerAnimations.adapter = ArrayAdapter.createFromResource(this, R.array.load_animations, android.R.layout.simple_dropdown_item_1line)
        headerAnimations.onItemSelectedListener = this
    }

    override fun onRefreshComplete(observable: RefreshObservable) = false

    override fun onRefreshStart(isFromTop: Boolean) {
        sHandler.postDelayed({ refresh?.completeRefresh() }, 3000)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.trigger -> refresh.setRefresh(directions?.checkedRadioButtonId == R.id.top)
        }
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val (pull, refresh) = when (position) {
            0 -> R.raw.load_1 to R.raw.load_1
            1 -> R.raw.load_2 to R.raw.load_2
            2 -> R.raw.load_3 to R.raw.load_3
            3 -> R.raw.load_4 to R.raw.load_4
            4 -> R.raw.load_5 to R.raw.load_5
            5 -> R.raw.load_6 to R.raw.load_6
            6 -> R.raw.load_7 to R.raw.load_7
            else -> R.raw.load_1 to R.raw.load_1
        }
        header?.apply {
            setPullAnimation(pull)
            setRefreshAnimation(refresh)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }

    companion object {
        val sHandler = Handler(Looper.getMainLooper())
    }
}