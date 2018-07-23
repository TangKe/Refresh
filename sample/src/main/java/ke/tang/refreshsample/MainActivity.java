package ke.tang.refreshsample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import ke.tang.refresh.AnimationRefreshView;
import ke.tang.refresh.RefreshLayout;

public class MainActivity extends AppCompatActivity implements RefreshLayout.OnRefreshListener, View.OnClickListener, AdapterView.OnItemSelectedListener {

    private RefreshLayout mRefresh;
    private Spinner mHeaderAnimations;
    private RadioGroup mDirections;
    private AnimationRefreshView mHeader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        mRefresh = findViewById(R.id.refresh);
        mRefresh.setOnRefreshListener(this);
        mHeaderAnimations = findViewById(R.id.headerAnimations);
        mHeaderAnimations.setAdapter(ArrayAdapter.createFromResource(this, R.array.load_animations, android.R.layout.simple_dropdown_item_1line));
        mDirections = findViewById(R.id.directions);
        mHeader = findViewById(R.id.header);
        mHeaderAnimations.setOnItemSelectedListener(this);
    }

    @Override
    public void onRefreshStart(boolean isFromTop) {
        mRefresh.postDelayed(() -> {
            mRefresh.completeRefresh();
        }, 3000);
    }

    @Override
    public boolean onRefreshComplete(RefreshLayout.RefreshObservable observable) {
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.trigger:
                mRefresh.setRefresh(mDirections.getCheckedRadioButtonId() == R.id.top);
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mRefresh.completeRefresh();
        switch (position) {
            case 0:
                mHeader.setPullAnimation(R.raw.load_1);
                mHeader.setRefreshAnimation(R.raw.load_1);
                break;
            case 1:
                mHeader.setPullAnimation(R.raw.load_2);
                mHeader.setRefreshAnimation(R.raw.load_2);
                break;
            case 2:
                mHeader.setPullAnimation(R.raw.load_3);
                mHeader.setRefreshAnimation(R.raw.load_3);
                break;
            case 3:
                mHeader.setPullAnimation(R.raw.load_4);
                mHeader.setRefreshAnimation(R.raw.load_4);
                break;
            case 4:
                mHeader.setPullAnimation(R.raw.load_5);
                mHeader.setRefreshAnimation(R.raw.load_5);
                break;
            case 5:
                mHeader.setPullAnimation(R.raw.load_6);
                mHeader.setRefreshAnimation(R.raw.load_6);
                break;
            case 6:
                mHeader.setPullAnimation(R.raw.load_7);
                mHeader.setRefreshAnimation(R.raw.load_7);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
