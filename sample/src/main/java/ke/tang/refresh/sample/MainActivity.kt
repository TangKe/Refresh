package ke.tang.refresh.sample

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import ke.tang.refresh.OnRefreshListener
import ke.tang.refresh.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnRefreshListener, View.OnClickListener,
    OnItemSelectedListener {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.refresh.setOnRefreshListener(this)
        binding.headerAnimations.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.load_animations,
            android.R.layout.simple_dropdown_item_1line
        )
        binding.headerAnimations.onItemSelectedListener = this
        viewModel.data.observe(this, Observer {
            binding.refresh.completeRefresh()
        })
    }

    override fun onRefreshStart(isHeader: Boolean) {
        viewModel.requestData()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.trigger -> binding.refresh.setRefresh(binding.directions.checkedRadioButtonId == R.id.top)
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
        binding.header.apply {
            setPullAnimation(pull)
            setRefreshAnimation(refresh)
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }
}