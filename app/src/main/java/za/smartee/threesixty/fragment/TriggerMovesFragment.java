package za.smartee.threesixty.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import za.smartee.threesixty.R;
import za.smartee.threesixty.activity.SlotDataActivity;
import za.smartee.threesixty.utils.ToastUtils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TriggerMovesFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {

    private static final String TAG = "MovesFragment";


    @BindView(R.id.tv_trigger_tips)
    TextView tvTriggerTips;
    @BindView(R.id.rb_always_start)
    RadioButton rbAlwaysStart;
    @BindView(R.id.rb_start_advertising)
    RadioButton rbStartAdvertising;
    @BindView(R.id.rb_stop_advertising)
    RadioButton rbStopAdvertising;
    @BindView(R.id.rg_moves)
    RadioGroup rgMoves;
    @BindView(R.id.et_start)
    EditText etStart;
    @BindView(R.id.et_stop)
    EditText etStop;


    private SlotDataActivity activity;


    public TriggerMovesFragment() {
    }

    public static TriggerMovesFragment newInstance() {
        TriggerMovesFragment fragment = new TriggerMovesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_trigger_moves, container, false);
        ButterKnife.bind(this, view);
        activity = (SlotDataActivity) getActivity();
        tvTriggerTips.setText(getString(R.string.trigger_moved_tips_1, "broadcast"));
        if (mDuration == 0) {
            if (!mIsStart) {
                rbAlwaysStart.setChecked(true);
            }
        } else {
            if (mIsStart) {
                rbStartAdvertising.setChecked(true);
                etStop.setText(mDuration + "");
                etStop.setSelection((mDuration + "").length());
                tvTriggerTips.setText(getString(R.string.trigger_moved_tips_2, "start to broadcast", String.format("%ds", mDuration), "stops broadcasting"));
            } else {
                rbStopAdvertising.setChecked(true);
                etStart.setText(mDuration + "");
                etStart.setSelection((mDuration + "").length());
                tvTriggerTips.setText(getString(R.string.trigger_moved_tips_2, "stop broadcasting", String.format("%ds", mDuration), "starts to broadcast"));
            }
        }
        rgMoves.setOnCheckedChangeListener(this);
        etStart.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String duration = s.toString();
                if (rbStopAdvertising.isChecked() && !TextUtils.isEmpty(duration)) {
                    mDuration = Integer.parseInt(duration);
                    tvTriggerTips.setText(getString(R.string.trigger_moved_tips_2, "stop broadcasting", String.format("%ds", mDuration), "starts to broadcast"));
                }
            }
        });
        etStop.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String duration = s.toString();
                if (rbStartAdvertising.isChecked() && !TextUtils.isEmpty(duration)) {
                    mDuration = Integer.parseInt(duration);
                    tvTriggerTips.setText(getString(R.string.trigger_moved_tips_2, "start to broadcast", String.format("%ds", mDuration), "stops broadcasting"));
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause: ");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy: ");
        super.onDestroy();
    }

    private boolean mIsStart = true;
    private int mDuration = 30;

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.rb_always_start:
                mIsStart = false;
                mDuration = 0;
                tvTriggerTips.setText(getString(R.string.trigger_moved_tips_1, "broadcast"));
                break;
            case R.id.rb_start_advertising:
                mIsStart = true;
                String startDuration = etStop.getText().toString();
                if (TextUtils.isEmpty(startDuration)) {
                    mDuration = 0;
                } else {
                    mDuration = Integer.parseInt(startDuration);
                }
                tvTriggerTips.setText(getString(R.string.trigger_moved_tips_2, "start to broadcast", String.format("%ds", mDuration), "stops broadcasting"));
                break;
            case R.id.rb_stop_advertising:
                mIsStart = false;
                String stopDuration = etStart.getText().toString();
                if (TextUtils.isEmpty(stopDuration)) {
                    mDuration = 0;
                } else {
                    mDuration = Integer.parseInt(stopDuration);
                }
                tvTriggerTips.setText(getString(R.string.trigger_moved_tips_2, "stop broadcasting", String.format("%ds", mDuration), "starts to broadcast"));
                break;
        }
    }

    public void setStart(boolean isStart) {
        mIsStart = isStart;
    }

    public boolean isStart() {
        return mIsStart;
    }


    public void setData(int data) {
        mDuration = data;
    }

    public int getData() {
        String duration = "";
        if (rbStartAdvertising.isChecked()) {
            duration = etStop.getText().toString();
        } else if (rbStopAdvertising.isChecked()) {
            duration = etStart.getText().toString();
        } else {
            duration = "0";
        }
        if (TextUtils.isEmpty(duration)) {
            ToastUtils.showToast(getActivity(), "The advertising can not be empty.");
            return -1;
        }
        mDuration = Integer.parseInt(duration);
        if ((rbStartAdvertising.isChecked() || rbStopAdvertising.isChecked()) && (mDuration < 1 || mDuration > 65535)) {
            ToastUtils.showToast(activity, "The advertising range is 1~65535");
            return -1;
        }
        return mDuration;
    }
}
