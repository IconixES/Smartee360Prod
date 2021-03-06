package za.smartee.threesixty.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignOutOptions;
import com.amplifyframework.api.graphql.model.ModelMutation;
import com.amplifyframework.api.graphql.model.ModelQuery;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.datastore.generated.model.Assets;
import com.amplifyframework.datastore.generated.model.AuditLog;
import com.amplifyframework.datastore.generated.model.Locations;
import com.amplifyframework.datastore.generated.model.Users;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.elvishew.xlog.XLog;
import za.smartee.threesixty.AppConstants;
import za.smartee.threesixty.BeaconXListAdapter;
import za.smartee.threesixty.R;
import za.smartee.threesixty.dialog.AlertMessageDialog;
import za.smartee.threesixty.dialog.LoadingDialog;
import za.smartee.threesixty.dialog.LoadingMessageDialog;
import za.smartee.threesixty.dialog.PasswordDialog;
import za.smartee.threesixty.dialog.ScanFilterDialog;
import za.smartee.threesixty.entity.BeaconXInfo;
import za.smartee.threesixty.utils.BeaconXInfoParseableImpl;
import za.smartee.threesixty.utils.ToastUtils;
import com.moko.ble.lib.MokoConstants;
import com.moko.ble.lib.event.ConnectStatusEvent;
import com.moko.ble.lib.event.OrderTaskResponseEvent;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;
import za.smartee.support.MokoBleScanner;
import za.smartee.support.MokoSupport;
import za.smartee.support.OrderTaskAssembler;
import za.smartee.support.callback.MokoScanDeviceCallback;
import za.smartee.support.entity.DeviceInfo;
import za.smartee.support.entity.OrderCHAR;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class ScanConfirmActivity extends BaseActivity{


    private boolean mReceiverTag = false;
    private ConcurrentHashMap<String, BeaconXInfo> beaconXInfoHashMap;
    private ArrayList<BeaconXInfo> beaconXInfos;
    private BeaconXListAdapter adapter;
    private boolean mInputPassword;
    private MokoBleScanner mokoBleScanner;
    private Handler mHandler;
    private boolean isPasswordError;

    private boolean loadingChecked = false;
    private boolean locationInfoFlag = false;
    public static final int REQUEST_BT_PERMISSIONS = 0;
    public static final int REQUEST_BT_ENABLE = 1;
    public static final int REQUEST_COARSE_LOCATION = 2;
    public static final int REQUEST_FINE_LOCATION = 3;
    LocationManager locationManager;
    LocationListener locationListener;
    //Setup the device GPS variables
    final double[] devLat = new double[1];
    final double[] devLng = new double[1];
    //Setup Variables
    private boolean mScanning = false;
    private boolean GPSPermission = false;
    private boolean dsErrorFlag = false;
    private Button btnScan = null;
    private Button btnConfirm = null;
    private Button btnScanCancel = null;
    private Button btnLogout = null;
    private String calculatedLoc;
    private String text;

    private boolean readyFlag=false;
    private ProgressBar spinner;
    private String provider;
    String selectedLocID;
    String selectedLongitude;
    String selectedLatitude;
    String dispText = "";
    Double maxRssiCap;
    Double minRssiCap;
    Double avgRssiCap;
    Long scannerSetTime;
    Date scanTime;
    Date confirmTime;
    String user;
    String scanDurationType;
    private boolean missingLocFlag = false;


    //Array list for the scanned data
    ArrayList<String> devData = new ArrayList<String>();

    List<Map<String, String>> devDataDetail;

    //Declare array map to store the details of the locations
    List<Map<String, String>> locationDetailInfo;
    List<Map<String, String>> devData2;


    //Declare array map to store the details of the assets
    List<Map<String, String>> assetDetailInfo;
    ArrayList<String> assetItems = new ArrayList<String>();

    ArrayList<String> auditItems = new ArrayList<String>();


    ArrayList<String> locDdData = new ArrayList<String>();

    private BluetoothLeScanner mBluetoothLeScanner = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    String company;



    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(AuthActivity.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }




    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_confirm);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        btnScan = (Button) findViewById(R.id.btnScan);
        btnConfirm = (Button) findViewById(R.id.btnConfirm);
        btnScanCancel = (Button) findViewById(R.id.btnCancelScan);
        btnLogout = (Button) findViewById(R.id.btnSignOut);
        TextView missingLocation = (TextView) findViewById(R.id.textViewMissingLocation);
        Spinner locDD = (Spinner) findViewById(R.id.locationsSpinner);
        devData2 = new ArrayList<Map<String, String>>();
        devDataDetail = new ArrayList<Map<String, String>>();
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage("Please connect to the internet and retry");
        dlgAlert.setTitle("Internet Connection - Error");
        dlgAlert.setPositiveButton("OK", null);
        dlgAlert.setCancelable(true);

        dlgAlert.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("dialog msg","clicked");
                        finishAndRemoveTask();
                    }
                });

        Boolean loadingChecked = getIntent().getBooleanExtra("loadingFlag",false);


        //Check if Location is missing or incorrect click
        missingLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                missingLocFlag = true;
                if (missingLocFlag){
                    locDD.setVisibility(View.VISIBLE);
                    btnConfirm.setVisibility(View.VISIBLE);
                    ArrayAdapter<String> LocationDDAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, locDdData);
                    LocationDDAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    LocationDDAdapter.notifyDataSetChanged();
                    locDD.setAdapter(LocationDDAdapter);
                }
            }
        });


        btnScanCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                devData.clear();
                locationDetailInfo.clear();
                assetDetailInfo.clear();
                assetItems.clear();
                locDdData.clear();
                devData2.clear();
                devDataDetail.clear();
                calculatedLoc = "";
                locDD.setVisibility((View.INVISIBLE));
                btnConfirm.setVisibility(View.INVISIBLE);
                btnScanCancel.setVisibility(View.INVISIBLE);
                missingLocation.setVisibility(View.INVISIBLE);
                TextView selectedLocation = (TextView) findViewById(R.id.textViewSelectLocation);
                selectedLocation.setVisibility(View.INVISIBLE);
                Intent i = new Intent(ScanConfirmActivity.this, ScanActivity.class);
                startActivity(i);
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AWSMobileClient.getInstance().signOut(SignOutOptions.builder().signOutGlobally(true).build(), new Callback<Void>() {
                    @Override
                    public void onResult(final Void result) {
                        Log.d("Signout Msg", "signed-out");
                        devData.clear();
                        locationDetailInfo.clear();
                        assetDetailInfo.clear();
                        assetItems.clear();
                        locDdData.clear();
                        devData2.clear();
                        devDataDetail.clear();
                        calculatedLoc = "";
                        locDD.setVisibility((View.INVISIBLE));
                        btnConfirm.setVisibility(View.INVISIBLE);
                        btnScanCancel.setVisibility(View.INVISIBLE);
                        missingLocation.setVisibility(View.INVISIBLE);
                        TextView selectedLocation = (TextView) findViewById(R.id.textViewSelectLocation);
                        selectedLocation.setVisibility(View.INVISIBLE);
                        Intent i = new Intent(ScanConfirmActivity.this, AuthActivity.class);
                        startActivity(i);
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e("Signout Msg", "sign-out error", e);
                    }
                });
            }
        });

        //Confirm Button
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView selectedLocation = (TextView) findViewById(R.id.textViewSelectLocation);
                selectedLocation.setVisibility(View.INVISIBLE);
                TextView missingLoc = (TextView) findViewById(R.id.textViewMissingLocation);
                missingLoc.setVisibility(View.INVISIBLE);
                Object[] devDataTemp = devData.toArray();
                for (Object s : devDataTemp) {
                    if (devData.indexOf(s) != devData.lastIndexOf(s)) {
                        devData.remove(devData.lastIndexOf(s));
                    }
                }
                if (missingLocFlag){
                    text = locDD.getSelectedItem().toString();
                } else {
                    text = calculatedLoc;
                }

                if (loadingChecked){
                    text = locDD.getSelectedItem().toString();
                }


                for (int i = 0; i < locationDetailInfo.size(); i++) {
                    if (locationDetailInfo.get(i).get("Address").equals(text)) {
                        selectedLocID = locationDetailInfo.get(i).get("LocationID");
                        selectedLongitude = locationDetailInfo.get(i).get("Longitude");
                        selectedLatitude = locationDetailInfo.get(i).get("Latitude");
                    }
                }

                //Get the number of existing Assets

                Integer numberExistingAssets = 0;
                for (int m = 0; m < assetDetailInfo.size(); m++) {
                    if (selectedLocID.equals(assetDetailInfo.get(m).get("locationID"))) {
                        numberExistingAssets++;
                    }
                }

                // Variable to get the number of valid scanned Assets
                Integer numberNewAssets = 0;
                confirmTime = Calendar.getInstance().getTime();

                //Calculate the min, max and average RSSI per mac address
                for (int t = 0; t < devData.size(); t++){
                    Integer counter = 0;
                    Integer rssiSum = 0;
                    Integer rssi = 0;
                    Integer minRssi = 0;
                    Integer maxRssi = -1000;
                    for (int q = 0; q < devData2.size(); q++){
                        if (devData2.get(q).get("devMac").equals(devData.get(t))){
                            counter++;
                            rssi = Integer.parseInt(devData2.get(q).get("rssi"));
                            rssiSum = rssiSum + rssi;
                            if (rssi < minRssi){
                                minRssi = rssi;
                            }
                            if (rssi > maxRssi){
                                maxRssi = rssi;
                            }
                        }
                    }
                    Map<String, String> devDataDetailItems = new HashMap<String, String>();

                    devDataDetailItems.put("devMac", devData.get(t));
                    devDataDetailItems.put("maxRssi",maxRssi.toString());
                    devDataDetailItems.put("minRssi",minRssi.toString());
                    devDataDetailItems.put("avgRssi",String.valueOf(rssiSum/counter));
                    devDataDetail.add(devDataDetailItems);
                }

                //Compare the scanned devices to the database and if found update
                for (int y = 0; y < assetItems.size(); y++) {
                    for (int z = 0; z < devData.size(); z++) {
                        if (devData.get(z).equals(assetItems.get(y))) {
                            for (int m = 0; m < assetDetailInfo.size(); m++) {
                                if (devData.get(z).equals(assetDetailInfo.get(m).get("assetID"))) {
                                    for (int u = 0; u < devDataDetail.size(); u++){
                                        if (devDataDetail.get(u).get("devMac").equals(devData.get(z))){
                                            maxRssiCap = Double.parseDouble(devDataDetail.get(u).get("maxRssi"));
                                            minRssiCap = Double.parseDouble(devDataDetail.get(u).get("minRssi"));
                                            avgRssiCap = Double.parseDouble(devDataDetail.get(u).get("avgRssi"));
                                        }
                                    }

                                    if (selectedLocID.equals(assetDetailInfo.get(m).get("locationID"))) {
                                    } else {
                                        numberNewAssets++;
                                        Assets AssetItem = Assets.builder()
                                                .baseAssetType(assetDetailInfo.get(m).get("baseAssetType"))
                                                .assetName(assetDetailInfo.get(m).get("assetName"))
                                                .assetId(devData.get(z))
                                                .macAddress(devData.get(z))
                                                .locationId(selectedLocID)
                                                .latitude(Double.parseDouble(selectedLatitude))
                                                .longitude(Double.parseDouble(selectedLongitude))
                                                .owner(company)
                                                .id(assetDetailInfo.get(m).get("systemID"))
                                                .rssiAvg(avgRssiCap)
                                                .rssiMax(maxRssiCap)
                                                .rssiMin(minRssiCap)
                                                .locationName(text)
                                                .build();

                                        // Mutation Update Start
                                        Amplify.API.mutate(ModelMutation.update(AssetItem),
                                                response -> Log.i("Smartee", "Asset with id: " + response.getData().getId()),
                                                error -> Log.e("Smartee", "Create failed", error)
                                        );
                                        //Mutation Update end
                                    }
                                    AuditLog auditItems = AuditLog.builder()
                                            .baseActionType(assetDetailInfo.get(m).get("baseAssetType"))
                                            .confirmTime(String.valueOf(confirmTime))
                                            .device(devData.get(z))
                                            .deviceLatitude(devLat[0])
                                            .deviceLongitude(devLng[0])
                                            .scanTime(String.valueOf(scanTime))
                                            .storeName(calculatedLoc)
                                            .user(AWSMobileClient.getInstance().getUsername())
                                            .selectedStoreName(selectedLocID)
                                            .rssiAvg(avgRssiCap)
                                            .rssiMax(maxRssiCap)
                                            .rssiMin(minRssiCap)
                                            .owner(company)
                                            .build();
                                    Amplify.API.mutate(ModelMutation.create(auditItems),
                                            response -> Log.i("Smartee", "Audit with id: " + response.getData().getId()),
                                            error -> Log.e("Smartee", "Create failed", error)
                                    );
                                    //Mutation Update end
                                }
                            }
                        }
                    }
                }
                Intent i = new Intent(ScanConfirmActivity.this, ScanActivity.class);
                i.putExtra("selectedLocation",text);
                i.putExtra("scanTime",scanTime.toString());
                i.putExtra("assetsInStore",numberExistingAssets.toString());
                i.putExtra("scannedAssets",numberNewAssets.toString());

                i.putExtra("scanHistFlag",true);
                clearData();
                startActivity(i);
            }
        });

        //Setup location parameters
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                devLat[0] = location.getLatitude();
                devLng[0] = location.getLongitude();
                //locationManager.removeUpdates(this);
            }
            @Override
            public void onProviderEnabled(@NonNull String provider) {
                Log.i("location","provider");
            }

            @Override
            public void onProviderDisabled(@NonNull String provider) {
                Log.i("location","provider disabled");
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.i("location","status changed");
            }
        };


        //Get the current GPS Location
        if (ContextCompat.checkSelfPermission(ScanConfirmActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(ScanConfirmActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, locationListener);
        }

        scanTime = Calendar.getInstance().getTime();


        TextView answer1 = (TextView) findViewById(R.id.scanInfo);
        answer1.setVisibility(View.INVISIBLE);
        TextView answer2 = (TextView) findViewById(R.id.scanInfo2);
        answer2.setVisibility(View.INVISIBLE);
        TextView answer3 = (TextView) findViewById(R.id.scanInfo3);
        answer3.setVisibility(View.INVISIBLE);
        TextView answer4 = (TextView) findViewById(R.id.TextViewSelectedStore);
        answer4.setVisibility(View.INVISIBLE);
        TextView infoHeader = (TextView) findViewById(R.id.infoHeader);
        infoHeader.setVisibility(View.INVISIBLE);
        TextView selectedLocation = (TextView) findViewById(R.id.textViewSelectLocation);


        MokoBleScanner mokoBleScanner = new MokoBleScanner(this);
        mokoBleScanner.startScanDevice(new MokoScanDeviceCallback() {
            @Override
            public void onStartScan() {
            }

            @Override
            public void onScanDevice(DeviceInfo device) {
                Log.i("Test Mac",device.mac);
                Log.i("Test Mac", String.valueOf(device.rssi));
                Map<String, String> scanInfo = new HashMap<String, String>();
                scanInfo.put("devMac", String.valueOf(device.mac));
                scanInfo.put("rssi", String.valueOf(device.rssi));
                devData2.add(scanInfo);
                devData.add(device.mac);
            }

            @Override
            public void onStopScan() {
            }
        });



        Boolean finalLoadingChecked1 = loadingChecked;
        AuthUser currentUser = Amplify.Auth.getCurrentUser();


                if (Amplify.Auth.getCurrentUser() == null){
                    Intent i = new Intent(ScanConfirmActivity.this, AuthActivity.class);
                    startActivity(i);
                }
                else {
                    String user = currentUser.getUsername().toString();
                    //Start of query to find the Locations, assets and company details.
                    Amplify.API.query(
                            ModelQuery.list(Users.class, Users.COMPANY.contains(getResources().getString(R.string.customer))),
                            response -> {
                                for (Users users : response.getData()) {
                                    if (user.equals(users.getEmail())) {
                                        company = users.getCompany();
                                    }
                                }

                                Amplify.API.query(
                                        ModelQuery.list(Locations.class, Locations.OWNER.contains(company)),
                                        locResponse -> {
                                            locationDetailInfo = new ArrayList<Map<String, String>>();
                                            for (Locations locationDetail : locResponse.getData()) {
                                                if (locationDetail.getAddress() != null) {
                                                    Map<String, String> locationDetailInfo1 = new HashMap<String, String>();
                                                    locationDetailInfo1.put("Address", locationDetail.getAddress());
                                                    locationDetailInfo1.put("LocationID", locationDetail.getId());
                                                    locationDetailInfo1.put("Longitude", locationDetail.getLongitude().toString());
                                                    locationDetailInfo1.put("Latitude", locationDetail.getLatitude().toString());
                                                    locationDetailInfo1.put("baseLocationType", locationDetail.getBaseLocationType());
                                                    locationDetailInfo.add(locationDetailInfo1);
                                                }
                                            }
                                            for (int r = 0; r < locationDetailInfo.size(); r++) {
                                                String tempLoc = locationDetailInfo.get(r).get("baseLocationType");
                                                if (finalLoadingChecked1) {
                                                    if (tempLoc.equals("Transit")) {
                                                        locDdData.add(locationDetailInfo.get(r).get("Address"));
                                                    }
                                                } else {
                                                    locDdData.add(locationDetailInfo.get(r).get("Address"));
                                                }
                                            }
                                        },
                                        error -> {
                                            Log.e("Smartee", "Query failure", error);
                                            dlgAlert.create().show();
                                        }
                                );

                                Amplify.API.query(
                                        ModelQuery.list(Assets.class, Assets.OWNER.contains(company)),
                                        assetResponse -> {
                                            assetDetailInfo = new ArrayList<Map<String, String>>();
                                            for (Assets assetDetail : assetResponse.getData()) {
                                                if (assetDetail.getAssetId() != null) {
                                                    assetItems.add(assetDetail.getAssetId().toString());
                                                    Map<String, String> assetDetailInfo1 = new HashMap<String, String>();
                                                    assetDetailInfo1.put("systemID", assetDetail.getId());
                                                    assetDetailInfo1.put("assetID", assetDetail.getAssetId());
                                                    assetDetailInfo1.put("baseAssetType", assetDetail.getBaseAssetType());
                                                    assetDetailInfo1.put("assetName", assetDetail.getAssetName());
                                                    assetDetailInfo1.put("locationID", assetDetail.getLocationId());
                                                    assetDetailInfo.add(assetDetailInfo1);
                                                }
                                            }
                                        },
                                        error -> {
                                            Log.e("Smartee", "Query failure", error);
                                            dlgAlert.create().show();
                                        }
                                );
                            },
                            error -> Log.e("Smartee 360 Message", "Query failure", error)
                    );

                    Boolean finalLoadingChecked = loadingChecked;
                    scannerSetTime = (new Double(25000)).longValue();

                    new CountDownTimer(scannerSetTime, 1000) {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        public void onTick(long millisUntilFinished) {
                            btnScan.setVisibility(View.INVISIBLE);
                            TextView infoView = (TextView) findViewById(R.id.infoView);
                            TextView timeText = (TextView) findViewById(R.id.timerText);

                            infoView.setText("Scanning In Progress");
                            Log.i("Smartee360Msg-Timer", String.valueOf((millisUntilFinished / 1000)));
                        }

                        @RequiresApi(api = Build.VERSION_CODES.O)
                        public void onFinish() {
                            mokoBleScanner.stopScanDevice();
                            spinner = (ProgressBar) findViewById(R.id.progressBar);
                            spinner.setVisibility(View.GONE);
                            TextView answer = (TextView) findViewById(R.id.scanInfo);
                            selectedLocation.setVisibility(View.VISIBLE);

                            if (locationDetailInfo.equals(null)) {
                                dlgAlert.show();
                            }
                            Integer locationDetailInfoSize = locationDetailInfo.size();
                            if (locationDetailInfoSize == null) {
                                locationDetailInfoSize = 0;
                            }

                            for (int r = 0; r < locationDetailInfoSize; r++) {
                                locationInfoFlag = true;
                                String tempLoc;
                                tempLoc = locationDetailInfo.get(r).get("baseLocationType");
                                if (tempLoc.equals("DC") || tempLoc.equals("Store")) {
                                    double distResult = distance(Double.parseDouble(locationDetailInfo.get(r).get("Latitude")), Double.parseDouble(locationDetailInfo.get(r).get("Longitude")), devLat[0], devLng[0], 0, 0);
                                    if (distResult < 501) {
                                        calculatedLoc = locationDetailInfo.get(r).get("Address");
                                    }
                                }
                            }
                            TextView selectedLocation = (TextView) findViewById(R.id.textViewSelectLocation);

                            if (finalLoadingChecked) {
                                selectedLocation.setVisibility(View.VISIBLE);
                                selectedLocation.setText("Select Vehicle");
                                locDD.setVisibility(View.VISIBLE);
                                ArrayAdapter<String> LocationDDAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, locDdData);
                                LocationDDAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                LocationDDAdapter.notifyDataSetChanged();
                                locDD.setAdapter(LocationDDAdapter);
                                calculatedLoc = "Loading Checked";
                            } else {
                                missingLocation.setVisibility(View.VISIBLE);
                                if (missingLocFlag == false) {
                                    selectedLocation.setVisibility(View.VISIBLE);
                                    if (calculatedLoc == null || calculatedLoc.equals("")) {
                                        calculatedLoc = "No Location Available";
                                    }
                                    selectedLocation.setText("Current Location - " + calculatedLoc);
                                }
                            }

                            if (!locationInfoFlag) {
                                selectedLocation.setText("ERROR - User Data Not Available. Cancel and Rescan or contact your administrator!");
                            }

                            locationInfoFlag = false;

                            if (calculatedLoc.equals("No Location Available")) {
                                if (missingLocFlag != false) {
                                    btnConfirm.setVisibility(View.VISIBLE);
                                }
                            } else {
                                btnConfirm.setVisibility(View.VISIBLE);
                            }
                            btnScanCancel.setVisibility(View.VISIBLE);


                            TextView infoView = (TextView) findViewById(R.id.infoView);
                            TextView timeText = (TextView) findViewById(R.id.timerText);
                            timeText.setText("");
                            infoView.setText("");
                            ArrayList<String> devData = new ArrayList<String>();
                        }
                    }.start();
                }
    }

    //Check Permissions
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(ScanConfirmActivity.this, permission)
                == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(ScanConfirmActivity.this,
                    new String[]{permission},
                    requestCode);
        } else {
            Toast.makeText(ScanConfirmActivity.this,
                    "Permission already granted",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    //function to calculate distance bet co-ordinates
    private static double distance(double lat1, double lon1, double lat2, double lon2, double el1, double el2) {
        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    private void clearData() {
        devData.clear();
        locationDetailInfo.clear();
        assetDetailInfo.clear();
        assetItems.clear();
        locDdData.clear();
        devData2.clear();
        devDataDetail.clear();
        TextView selectedLocation = (TextView) findViewById(R.id.textViewSelectLocation);
        selectedLocation.setText("Select Location");
        calculatedLoc = "";
        Spinner locDD = (Spinner) findViewById(R.id.locationsSpinner);
        locDD.setVisibility((View.INVISIBLE));
        btnConfirm.setVisibility(View.INVISIBLE);
        btnScanCancel.setVisibility(View.INVISIBLE);
    }

}

