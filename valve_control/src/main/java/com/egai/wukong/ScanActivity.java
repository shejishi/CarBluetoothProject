package com.egai.wukong;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;

import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;


import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.data.BleScanState;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.egai.ble.valveControl.R;
import com.egai.wukong.EventBusMessage.BleEvent;
import com.egai.wukong.adapter.DeviceAdapter;
import com.egai.wukong.operation.DeviceControl;

import com.egai.wukong.operation.DeviceControlOld;
import com.egai.wukong.service.BleService;

import android.support.v7.widget.Toolbar;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.greenrobot.eventbus.ThreadMode.MAIN;

public class ScanActivity   extends AppCompatActivity {

    final String tag="TAG_MAIN";
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;



    public static String Service_uuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_E2 = "0000ffe2-0000-1000-8000-00805f9b34fb";
    private BluetoothAdapter mBluetoothAdapter;
    private Button btn_scan;
    private DeviceAdapter mDeviceAdapter;
    private ImageView img_loading;
    private Animation operatingAnim;
    private Context mContext;
    ProgressDialog progressDialog;
    private String savedMac;
    private boolean autoConnect;
    private Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mContext=this;
        EventBus.getDefault().register(this);
        savedMac=new MyPreferences(this).getString("savedMac","");
        autoConnect=new MyPreferences(this).getBoolean("autoConnect",true);

        init_ble();
        init_view();
        if(!savedMac.isEmpty())
        {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startScan();
                }
            },1000);
        }
    }



    private  void init_view()
    {

        progressDialog = new ProgressDialog(mContext);//1.创建一个ProgressDialog的实例
        progressDialog.setTitle(getResources().getString(R.string.processDialog_title));//2.设置标题
        progressDialog.setMessage(getResources().getString(R.string.processDialog_message_wait));//3.设置显示内容
        progressDialog.setCancelable(true);//4.设置可否用back键关闭对话框
        btn_scan=findViewById(R.id.btn_scan);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                btn_scan.performClick();
            }
        },1000);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(tag,"click!");
                if(BleManager.getInstance().getScanSate()==BleScanState.STATE_SCANNING)
                {
                    BleManager.getInstance().cancelScan();
                    return;
                }
                checkPermissions();
            }
        });
        mDeviceAdapter=new DeviceAdapter(getApplicationContext());

        img_loading = findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        ListView listView_device = findViewById(R.id.list_device);
        listView_device.setAdapter(mDeviceAdapter);
        listView_device.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(tag,"item click!");
                BleManager.getInstance().cancelScan();
                BleDevice bleDevice=mDeviceAdapter.getItem(position);
               if(!bleDevice.getName().contains("V4"))
               {



                   Intent intent=new Intent(getApplicationContext(),DeviceControlOld.class);

                   intent.putExtra("bleDevice",bleDevice);

                   startActivity(intent);
                   return;
               }
                Intent intent=new Intent(ScanActivity.this,BleService.class);
                startService(intent);



                progressDialog.setMessage(getResources().getString(R.string.processDialog_message_wait));
                progressDialog.show();//5.将ProgessDialog显示出来

                EventBus.getDefault().post(new BleEvent(BleEvent.CMD_START_CONNECT,bleDevice));
            }
        });
    }

    private void init_ble()
    {
        BleManager.getInstance().init(getApplication());
        BleManager.getInstance()
                .setReConnectCount(1,5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(5000);

    }
    public static boolean isServiceRunning(Context mContext, String className) {

        boolean isRunning = false;
        ActivityManager activityManager = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager
                .getRunningServices(30);

        if (!(serviceList.size() > 0)) {
            return false;
        }

        for (int i = 0; i < serviceList.size(); i++) {
            if (serviceList.get(i).service.getClassName().equals(className) == true) {
                isRunning = true;
                break;
            }
        }
        return isRunning;
    }



    private void setScanRule() {
        String[] uuids;
        // String str_uuid = et_uuid.getText().toString();
        String str_uuid=Service_uuid;



        if (TextUtils.isEmpty(str_uuid)) {
            uuids = null;
        } else {
            uuids = str_uuid.split(",");
        }
        UUID[] serviceUuids = null;
        if (uuids != null && uuids.length > 0) {
            serviceUuids = new UUID[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                String name = uuids[i];
                String[] components = name.split("-");
                if (components.length != 5) {
                    serviceUuids[i] = null;
                } else {
                    serviceUuids[i] = UUID.fromString(uuids[i]);
                }
            }
        }

//        String[] names;
//        //String str_name = et_name.getText().toString();
//        String str_name=null;
//        if (TextUtils.isEmpty(str_name)) {
//            names = null;
//        } else {
//            names = str_name.split(",");
//        }

        //String mac = et_mac.getText().toString();
        String mac = null;

        boolean isAutoConnect = true;

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
                //.setDeviceName(true, names)   // 只扫描指定广播名的设备，可选
                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(isAutoConnect)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);


    }

    @Subscribe(threadMode = MAIN)
    public void handleBleEvent(BleEvent bleEvent)
    {
        switch (bleEvent.getCmd())
        {
            case BleEvent.BLE_EVENT_CONNECTED:
                startActivity(new Intent(getApplicationContext(),DeviceControl.class));
                progressDialog.dismiss();

                if(!savedMac.equals(bleEvent.getData().toString()))
                {
                    new MyPreferences(this).putString("savedMac",bleEvent.getData().toString());
                }

                break;
            case BleEvent.BLE_EVENT_CONNECTION_FAIL:
                progressDialog.setMessage(getResources().getString(R.string.processDiaolog_message_fail));

                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!isServiceRunning(this,BleService.class.getName()))
        {
            Intent intent=new Intent(this,BleService.class);
            startService(intent);
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnectAllDevice();
        BleManager.getInstance().destroy();
        Intent intent=new Intent(this,BleService.class);
        stopService(intent);
        Log.d(tag,"activity destroy");
        EventBus.getDefault().unregister(this);
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }

        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        List<String> permissionDeniedList = new ArrayList<>();
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, permission);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission);
            } else {
                permissionDeniedList.add(permission);
            }
        }
        if (!permissionDeniedList.isEmpty()) {
            String[] deniedPermissions = permissionDeniedList.toArray(new String[permissionDeniedList.size()]);
            ActivityCompat.requestPermissions(this, deniedPermissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }
    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null)
            return false;
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }
    private void startScan() {
        BleManager.getInstance().scan(new BleScanCallback() {
            @Override
            public void onScanStarted(boolean success) {
                mDeviceAdapter.clearScanDevice();
                mDeviceAdapter.notifyDataSetChanged();
                img_loading.startAnimation(operatingAnim);
                img_loading.setVisibility(View.VISIBLE);
                btn_scan.setText(getString(R.string.stop_scan));
                Log.d(tag,"scan started!");

            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
                Log.d(tag,"device"+bleDevice.getMac());
                if(autoConnect==true && savedMac.equals(bleDevice.getMac()))
                {
                    //只在开始的时候自动连接，用户操作后，要手动连接
                    savedMac="";
                    BleManager.getInstance().cancelScan();
                    progressDialog.show();
                    EventBus.getDefault().post(new BleEvent(BleEvent.CMD_START_CONNECT,bleDevice));

                }

            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                mDeviceAdapter.addDevice(bleDevice);
                mDeviceAdapter.notifyDataSetChanged();
                Log.d(tag,"device_filter"+bleDevice.getMac());
            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {
                img_loading.clearAnimation();
                img_loading.setVisibility(View.INVISIBLE);
                btn_scan.setText(getString(R.string.start_scan));
                Log.d(tag,"scan finished!");
            }
        });
    }
    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    setScanRule();
                    startScan();
                }
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                setScanRule();
                startScan();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        switch (item.getItemId()){
            case R.id.settings:


               startActivity(new Intent(this,com.egai.wukong.operation.Settings.class));

                break;
            case R.id.about:

                break;


        }
        return true;
    }






}
