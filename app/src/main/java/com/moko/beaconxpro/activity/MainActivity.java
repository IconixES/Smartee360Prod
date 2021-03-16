package com.moko.beaconxpro.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import com.moko.beaconxpro.AppConstants;
import com.moko.beaconxpro.BeaconXListAdapter;
import com.moko.beaconxpro.R;
import com.moko.beaconxpro.dialog.AlertMessageDialog;
import com.moko.beaconxpro.dialog.LoadingDialog;
import com.moko.beaconxpro.dialog.LoadingMessageDialog;
import com.moko.beaconxpro.dialog.PasswordDialog;
import com.moko.beaconxpro.dialog.ScanFilterDialog;
import com.moko.beaconxpro.entity.BeaconXInfo;
import com.moko.beaconxpro.utils.BeaconXInfoParseableImpl;
import com.moko.beaconxpro.utils.ToastUtils;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import com.moko.support.MokoBleScanner;
import com.moko.support.MokoSupport;
import com.moko.support.OrderTaskAssembler;
import com.moko.support.callback.MokoScanDeviceCallback;
import com.moko.support.entity.DeviceInfo;
import com.moko.support.entity.OrderCHAR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends BaseActivity implements MokoScanDeviceCallback, BaseQuickAdapter.OnItemChildClickListener {

    @BindView(R.id.iv_refresh)
    ImageView ivRefresh;
    @BindView(R.id.rv_devices)
    RecyclerView rvDevices;
    @BindView(R.id.tv_device_num)
    TextView tvDeviceNum;
    @BindView(R.id.rl_edit_filter)
    RelativeLayout rl_edit_filter;
    @BindView(R.id.rl_filter)
    RelativeLayout rl_filter;
    @BindView(R.id.tv_filter)
    TextView tv_filter;
    private boolean mReceiverTag = false;
    private ConcurrentHashMap<String, BeaconXInfo> beaconXInfoHashMap;
    private ArrayList<BeaconXInfo> beaconXInfos;
    private BeaconXListAdapter adapter;
    private boolean mInputPassword;
    private MokoBleScanner mokoBleScanner;
    private Handler mHandler;
    private boolean isPasswordError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        beaconXInfoHashMap = new ConcurrentHashMap<>();
        beaconXInfos = new ArrayList<>();
        adapter = new BeaconXListAdapter();
        adapter.replaceData(beaconXInfos);
        adapter.setOnItemChildClickListener(this);
        adapter.openLoadAnimation();
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        DividerItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(ContextCompat.getDrawable(this, R.drawable.shape_recycleview_divider));
        rvDevices.addItemDecoration(itemDecoration);
        rvDevices.setAdapter(adapter);

        mHandler = new Handler(Looper.getMainLooper());
        mokoBleScanner = new MokoBleScanner(this);
        EventBus.getDefault().register(this);
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);
        mReceiverTag = true;
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
        } else {
            if (animation == null) {
                startScan();
            }
        }
    }

    private String unLockResponse;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            if (animation != null) {
                                mHandler.removeMessages(0);
                                mokoBleScanner.stopScanDevice();
                                mokoBleScanner.stopScanDevice();
                                onStopScan();
                            }
                            break;
                        case BluetoothAdapter.STATE_ON:
                            if (animation == null) {
                                startScan();
                            }
                            break;

                    }
                }
            }
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnectStatusEvent(ConnectStatusEvent event) {
        String action = event.getAction();
        if (MokoConstants.ACTION_DISCONNECTED.equals(action)) {
            mPassword = "";
            // 设备断开，通知页面更新
            dismissLoadingProgressDialog();
            dismissLoadingMessageDialog();
            if (!mInputPassword && animation == null) {
                if (isPasswordError) {
                    isPasswordError = false;
                } else {
                    ToastUtils.showToast(MainActivity.this, "Connection failed");
                }
                startScan();
            } else {
                mInputPassword = false;
            }
        }
        if (MokoConstants.ACTION_DISCOVER_SUCCESS.equals(action)) {
            // 设备连接成功，通知页面更新
            dismissLoadingProgressDialog();
            BluetoothGattCharacteristic characteristic = MokoSupport.getInstance().getCharacteristic(OrderCHAR.CHAR_DEVICE_TYPE);
            if (characteristic != null) {
                showLoadingMessageDialog();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (TextUtils.isEmpty(mPassword)) {
                            ArrayList<OrderTask> orderTasks = new ArrayList<>();
                            orderTasks.add(OrderTaskAssembler.getLockState());
                            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
                        } else {
                            XLog.i("锁定状态，获取unLock，解锁");
                            ArrayList<OrderTask> orderTasks = new ArrayList<>();
                            orderTasks.add(OrderTaskAssembler.getUnLock());
                            MokoSupport.getInstance().sendOrder(orderTasks.toArray(new OrderTask[]{}));
                        }
                    }
                }, 500);

            } else {
                MokoSupport.getInstance().disConnectBle();
                AlertMessageDialog dialog = new AlertMessageDialog();
                dialog.setMessage("Oops!Something went wrong. Please check the device version.");
                dialog.setConfirm(R.string.ok);
                dialog.show(getSupportFragmentManager());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOrderTaskResponseEvent(OrderTaskResponseEvent event) {
        final String action = event.getAction();
        if (MokoConstants.ACTION_ORDER_TIMEOUT.equals(action)) {
        }
        if (MokoConstants.ACTION_ORDER_FINISH.equals(action)) {
        }
        if (MokoConstants.ACTION_ORDER_RESULT.equals(action)) {
            OrderTaskResponse response = event.getResponse();
            OrderCHAR orderCHAR = (OrderCHAR) response.orderCHAR;
            int responseType = response.responseType;
            byte[] value = response.responseValue;
            switch (orderCHAR) {
                case CHAR_LOCK_STATE:
                    String valueStr = MokoUtils.bytesToHexString(value);
                    dismissLoadingMessageDialog();
                    if ("00".equals(valueStr)) {
                        MokoSupport.getInstance().disConnectBle();
                        if (TextUtils.isEmpty(unLockResponse)) {
                            mInputPassword = true;
                            // 弹出密码框
                            PasswordDialog dialog = new PasswordDialog();
                            dialog.setPassword(mSavedPassword);
                            dialog.setOnPasswordClicked(new PasswordDialog.PasswordClickListener() {
                                @Override
                                public void onEnsureClicked(String password) {
                                    if (!MokoSupport.getInstance().isBluetoothOpen()) {
                                        // 蓝牙未打开，开启蓝牙
                                        MokoSupport.getInstance().enableBluetooth();
                                        return;
                                    }
                                    XLog.i(password);
                                    mPassword = password;
                                    if (animation != null) {
                                        mHandler.removeMessages(0);
                                        mokoBleScanner.stopScanDevice();
                                    }
                                    showLoadingProgressDialog();
                                    ivRefresh.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            MokoSupport.getInstance().connDevice(mSelectedBeaconXMac);
                                        }
                                    }, 500);
                                }

                                @Override
                                public void onDismiss() {
                                    unLockResponse = "";
                                    if (animation == null) {
                                        startScan();
                                    }
                                }
                            });
                            dialog.show(MainActivity.this.getSupportFragmentManager());
                        } else {
                            isPasswordError = true;
                            unLockResponse = "";
                            ToastUtils.showToast(MainActivity.this, "Password Incorrect");
                        }
                    } else if ("02".equals(valueStr)) {
                        // 不需要密码验证
                        Intent deviceInfoIntent = new Intent(MainActivity.this, DeviceInfoActivity.class);
                        deviceInfoIntent.putExtra(AppConstants.EXTRA_KEY_PASSWORD, mPassword);
                        startActivityForResult(deviceInfoIntent, AppConstants.REQUEST_CODE_DEVICE_INFO);
                    } else {
                        // 解锁成功
                        XLog.i("解锁成功");
                        unLockResponse = "";
                        mSavedPassword = mPassword;
                        Intent deviceInfoIntent = new Intent(MainActivity.this, DeviceInfoActivity.class);
                        deviceInfoIntent.putExtra(AppConstants.EXTRA_KEY_PASSWORD, mPassword);
                        startActivityForResult(deviceInfoIntent, AppConstants.REQUEST_CODE_DEVICE_INFO);
                    }
                    break;
                case CHAR_UNLOCK:
                    if (responseType == OrderTask.RESPONSE_TYPE_READ) {
                        unLockResponse = MokoUtils.bytesToHexString(value);
                        XLog.i("返回的随机数：" + unLockResponse);
                        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.setUnLock(mPassword, value));
                    }
                    if (responseType == OrderTask.RESPONSE_TYPE_WRITE) {
                        MokoSupport.getInstance().sendOrder(OrderTaskAssembler.getLockState());
                    }
                    break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case AppConstants.REQUEST_CODE_DEVICE_INFO:
                    mPassword = "";
                    if (animation == null) {
                        startScan();
                    }
                    break;

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiverTag) {
            mReceiverTag = false;
            // 注销广播
            unregisterReceiver(mReceiver);
        }
        EventBus.getDefault().unregister(this);
    }


    @Override
    public void onStartScan() {
        beaconXInfoHashMap.clear();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (animation != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.replaceData(beaconXInfos);
                            tvDeviceNum.setText(String.format("DEVICE(%d)", beaconXInfos.size()));
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    updateDevices();
                }
            }
        }).start();
    }

    private BeaconXInfoParseableImpl beaconXInfoParseable;

    @Override
    public void onScanDevice(DeviceInfo deviceInfo) {
        BeaconXInfo beaconXInfo = beaconXInfoParseable.parseDeviceInfo(deviceInfo);
        if (beaconXInfo == null)
            return;
        beaconXInfoHashMap.put(beaconXInfo.mac, beaconXInfo);
    }

    @Override
    public void onStopScan() {
        findViewById(R.id.iv_refresh).clearAnimation();
        animation = null;
    }

    private void updateDevices() {
        beaconXInfos.clear();
        if (!TextUtils.isEmpty(filterName)
                || !TextUtils.isEmpty(filterMac)
                || filterRssi != -100) {
            ArrayList<BeaconXInfo> beaconXInfosFilter = new ArrayList<>(beaconXInfoHashMap.values());
            Iterator<BeaconXInfo> iterator = beaconXInfosFilter.iterator();
            while (iterator.hasNext()) {
                BeaconXInfo beaconXInfo = iterator.next();
                if (beaconXInfo.rssi > filterRssi) {
                    if (TextUtils.isEmpty(filterName) && TextUtils.isEmpty(filterMac)) {
                        continue;
                    } else {
                        if (TextUtils.isEmpty(beaconXInfo.name) && TextUtils.isEmpty(beaconXInfo.mac)) {
                            iterator.remove();
                        } else if (TextUtils.isEmpty(beaconXInfo.name) && beaconXInfo.mac.toLowerCase().replaceAll(":", "").contains(filterMac.toLowerCase())) {
                            continue;
                        } else if (TextUtils.isEmpty(beaconXInfo.mac) && beaconXInfo.name.toLowerCase().contains(filterName.toLowerCase())) {
                            continue;
                        } else if (!TextUtils.isEmpty(beaconXInfo.name) && !TextUtils.isEmpty(beaconXInfo.mac)
                                && (!TextUtils.isEmpty(filterName) && beaconXInfo.name.toLowerCase().contains(filterName.toLowerCase()))
                                || (!TextUtils.isEmpty(filterMac) && beaconXInfo.mac.toLowerCase().replaceAll(":", "").contains(filterMac.toLowerCase()))) {
                            continue;
                        } else {
                            iterator.remove();
                        }
                    }
                } else {
                    iterator.remove();
                }
            }
            beaconXInfos.addAll(beaconXInfosFilter);
        } else {
            beaconXInfos.addAll(beaconXInfoHashMap.values());
        }
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        Collections.sort(beaconXInfos, new Comparator<BeaconXInfo>() {
            @Override
            public int compare(BeaconXInfo lhs, BeaconXInfo rhs) {
                if (lhs.rssi > rhs.rssi) {
                    return -1;
                } else if (lhs.rssi < rhs.rssi) {
                    return 1;
                }
                return 0;
            }
        });
    }

    private Animation animation = null;
    public String filterName;
    public String filterMac;
    public int filterRssi = -100;

    @OnClick({R.id.iv_refresh, R.id.iv_about, R.id.rl_edit_filter, R.id.rl_filter, R.id.iv_filter_delete})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.iv_refresh:
                if (isWindowLocked())
                    return;
                if (!MokoSupport.getInstance().isBluetoothOpen()) {
                    // 蓝牙未打开，开启蓝牙
                    MokoSupport.getInstance().enableBluetooth();
                    return;
                }
                if (animation == null) {
                    startScan();
                } else {
                    mHandler.removeMessages(0);
                    mokoBleScanner.stopScanDevice();
                }
                break;
            case R.id.iv_about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.rl_edit_filter:
            case R.id.rl_filter:
                if (animation != null) {
                    mHandler.removeMessages(0);
                    mokoBleScanner.stopScanDevice();
                }
                ScanFilterDialog scanFilterDialog = new ScanFilterDialog(this);
                scanFilterDialog.setFilterName(filterName);
                scanFilterDialog.setFilterMac(filterMac);
                scanFilterDialog.setFilterRssi(filterRssi);
                scanFilterDialog.setOnScanFilterListener((filterName, filterMac, filterRssi) -> {
                    MainActivity.this.filterName = filterName;
                    MainActivity.this.filterMac = filterMac;
                    MainActivity.this.filterRssi = filterRssi;
                    if (!TextUtils.isEmpty(filterName)
                            || !TextUtils.isEmpty(filterMac)
                            || filterRssi != -100) {
                        rl_filter.setVisibility(View.VISIBLE);
                        rl_edit_filter.setVisibility(View.GONE);
                        StringBuilder stringBuilder = new StringBuilder();
                        if (!TextUtils.isEmpty(filterName)) {
                            stringBuilder.append(filterName);
                            stringBuilder.append(";");
                        }
                        if (!TextUtils.isEmpty(filterMac)) {
                            stringBuilder.append(filterMac);
                            stringBuilder.append(";");
                        }
                        if (filterRssi != -100) {
                            stringBuilder.append(String.format("%sdBm", filterRssi + ""));
                            stringBuilder.append(";");
                        }
                        tv_filter.setText(stringBuilder.toString());
                    } else {
                        rl_filter.setVisibility(View.GONE);
                        rl_edit_filter.setVisibility(View.VISIBLE);
                    }
                    if (isWindowLocked())
                        return;
                    if (animation == null) {
                        startScan();
                    }
                });
                scanFilterDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (isWindowLocked())
                            return;
                        if (animation == null) {
                            startScan();
                        }
                    }
                });
                scanFilterDialog.show();
                break;
            case R.id.iv_filter_delete:
                if (animation != null) {
                    mHandler.removeMessages(0);
                    mokoBleScanner.stopScanDevice();
                }
                rl_filter.setVisibility(View.GONE);
                rl_edit_filter.setVisibility(View.VISIBLE);
                filterName = "";
                filterMac = "";
                filterRssi = -100;
                if (isWindowLocked())
                    return;
                if (animation == null) {
                    startScan();
                }
                break;
        }
    }

    private void startScan() {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
            return;
        }
        animation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        findViewById(R.id.iv_refresh).startAnimation(animation);
        beaconXInfoParseable = new BeaconXInfoParseableImpl();
        mokoBleScanner.startScanDevice(this);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mokoBleScanner.stopScanDevice();
            }
        }, 1000 * 60);
    }


    private LoadingDialog mLoadingDialog;

    private void showLoadingProgressDialog() {
        mLoadingDialog = new LoadingDialog();
        mLoadingDialog.show(getSupportFragmentManager());

    }

    private void dismissLoadingProgressDialog() {
        if (mLoadingDialog != null)
            mLoadingDialog.dismissAllowingStateLoss();
    }

    private LoadingMessageDialog mLoadingMessageDialog;

    private void showLoadingMessageDialog() {
        mLoadingMessageDialog = new LoadingMessageDialog();
        mLoadingMessageDialog.setMessage("Verifying..");
        mLoadingMessageDialog.show(getSupportFragmentManager());

    }

    private void dismissLoadingMessageDialog() {
        if (mLoadingMessageDialog != null)
            mLoadingMessageDialog.dismissAllowingStateLoss();
    }

    @Override
    public void onBackPressed() {
        AlertMessageDialog dialog = new AlertMessageDialog();
        dialog.setMessage(R.string.main_exit_tips);
        dialog.setOnAlertConfirmListener(new AlertMessageDialog.OnAlertConfirmListener() {
            @Override
            public void onClick() {
                MainActivity.this.finish();
            }
        });
        dialog.show(getSupportFragmentManager());
    }

    private String mPassword;
    private String mSavedPassword;
    private String mSelectedBeaconXMac;

    @Override
    public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
        if (!MokoSupport.getInstance().isBluetoothOpen()) {
            // 蓝牙未打开，开启蓝牙
            MokoSupport.getInstance().enableBluetooth();
            return;
        }
        final BeaconXInfo beaconXInfo = (BeaconXInfo) adapter.getItem(position);
        if (beaconXInfo != null && !isFinishing()) {
            if (animation != null) {
                mHandler.removeMessages(0);
                mokoBleScanner.stopScanDevice();
            }
            mSelectedBeaconXMac = beaconXInfo.mac;
            showLoadingProgressDialog();
            ivRefresh.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MokoSupport.getInstance().connDevice(beaconXInfo.mac);
                }
            }, 500);
        }
    }
}
