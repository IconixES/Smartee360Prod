package za.smartee.threesixty.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import za.smartee.threesixty.R;
import za.smartee.threesixty.activity.THDataActivity;
import za.smartee.threesixty.dialog.BottomDialog;
import com.moko.ble.lib.utils.MokoUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StorageTempFragment extends Fragment {

    private static final String TAG = "StorageTempFragment";
    @BindView(R.id.tv_storage_temp_only)
    TextView tvStorageTempOnly;
    @BindView(R.id.tv_temp_only_tips)
    TextView tvTempOnlyTips;
    private ArrayList<String> mDatas;

    private THDataActivity activity;


    public StorageTempFragment() {
    }

    public static StorageTempFragment newInstance() {
        StorageTempFragment fragment = new StorageTempFragment();
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
        View view = inflater.inflate(R.layout.fragment_storage_temp, container, false);
        ButterKnife.bind(this, view);
        activity = (THDataActivity) getActivity();
        mDatas = new ArrayList<>();
        for (int i = 0; i <= 120; i++) {
            mDatas.add(MokoUtils.getDecimalFormat("0.0").format(i * 0.5));
        }
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

    @OnClick(R.id.tv_storage_temp_only)
    public void onViewClicked() {
        BottomDialog dialog = new BottomDialog();
        dialog.setDatas(mDatas, mSelected);
        dialog.setListener(value -> {
            mSelected = value;
            if (mSelected == 0) {
                tvTempOnlyTips.setText(R.string.temp_only_tips_0);
            } else {
                tvTempOnlyTips.setText(getString(R.string.temp_only_tips_1, mDatas.get(value)));
            }
            tvStorageTempOnly.setText(mDatas.get(value));
            activity.setSelectedTemp(value);
        });
        dialog.show(activity.getSupportFragmentManager());
    }

    private int mSelected;

    public void setTempData(int data) {
        mSelected = data;
        String tempStr = mDatas.get(mSelected);
        tvStorageTempOnly.setText(tempStr);
        if (mSelected == 0) {
            tvTempOnlyTips.setText(R.string.temp_only_tips_0);
        } else {
            tvTempOnlyTips.setText(getString(R.string.temp_only_tips_1, tempStr));
        }
    }
}
