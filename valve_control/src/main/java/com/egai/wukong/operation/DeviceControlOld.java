package com.egai.wukong.operation;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.egai.ble.valveControl.R;
import com.egai.wukong.MyPreferences;
import com.egai.wukong.WheelView.NumberWheel;
import com.egai.wukong.service.BleService;

import java.util.ArrayList;
import java.util.List;

import ademar.phasedseekbar.PhasedInteractionListener;
import ademar.phasedseekbar.PhasedListener;
import ademar.phasedseekbar.PhasedSeekBar;
import ademar.phasedseekbar.SimplePhasedAdapter;

public class DeviceControlOld extends AppCompatActivity {


    BleDevice mBleDevice;
    private Toolbar toolbar;
    private String Tag=this.getClass().getSimpleName();
    private TextView deviceName,deviceMac;
    private Switch aSwitch;
    private int close_delay=0,open_speed=1000;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control_old);
        toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        this.getSupportActionBar().setDisplayShowTitleEnabled(true);
        mBleDevice=getIntent().getParcelableExtra("bleDevice");
        deviceMac=findViewById(R.id.device_address);
        deviceMac.setText(mBleDevice.getMac());
        deviceName=findViewById(R.id.device_name);
        deviceName.setText(mBleDevice.getName());
        aSwitch=findViewById(R.id.device_switch);
        progressDialog = new ProgressDialog(DeviceControlOld.this);//1.创建一个ProgressDialog的实例
        progressDialog.setTitle(getResources().getString(R.string.processDialog_title));//2.设置标题
        progressDialog.setMessage(getResources().getString(R.string.processDialog_message_wait));//3.设置显示内容
        progressDialog.setCancelable(true);//4.设置可否用back键关闭对话框
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    if(BleManager.getInstance().isConnected(mBleDevice))
                    {
                        return;
                    }
                    new Handler().post(connectBleDevice);


                }
                else
                {
                    if(BleManager.getInstance().isConnected(mBleDevice))
                    {
                       BleManager.getInstance().disconnect(mBleDevice);
                    }
                }
            }
        });

        initPhasedSeekBar();
        new Handler().postDelayed(connectBleDevice,200);


        Log.d(Tag,mBleDevice.getMac());




    }

    private Runnable connectBleDevice=new Runnable() {
        @Override
        public void run() {
            progressDialog.show();

            BleManager.getInstance().connect(mBleDevice, new BleGattCallback() {
                @Override
                public void onStartConnect() {
                    Log.d(Tag,"on start");
                }

                @Override
                public void onConnectFail(BleDevice bleDevice, BleException exception) {
                    Log.d(Tag,"on fail");
                    aSwitch.setChecked(false);
                    Toast.makeText(DeviceControlOld.this,"连接失败",Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                    psbHorizontal.setVisibility(View.INVISIBLE);

                }

                @Override
                public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                    Log.d(Tag,"on success");
                    progressDialog.dismiss();
                    Toast.makeText(DeviceControlOld.this,R.string.connected,Toast.LENGTH_LONG).show();
                    BleManager.getInstance().notify(bleDevice, BleService.Service_uuid, BleService.Characteristic_uuid_TX, new BleNotifyCallback() {
                        @Override
                        public void onNotifySuccess() {
                            aSwitch.setChecked(true);
                            psbHorizontal.setVisibility(View.VISIBLE);
                            new MyPreferences(getApplicationContext()).putString("savedMac","");

                        }

                        @Override
                        public void onNotifyFailure(BleException exception) {

                            Toast.makeText(DeviceControlOld.this,"不能在这个设备上找到相应服务，可能是错误的设备",Toast.LENGTH_LONG).show();

                        }

                        @Override
                        public void onCharacteristicChanged(byte[] data) {

                            Log.d(Tag,"data:"+new String(data));

                        }
                    });

                }

                @Override
                public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                    Log.d(Tag,"disconnected");
                    aSwitch.setChecked(false);
                    Toast.makeText(DeviceControlOld.this,R.string.disconnected,Toast.LENGTH_LONG).show();
                    psbHorizontal.setVisibility(View.INVISIBLE);

                }
            });

        }
    };


    private void sendCmd(byte[] data){
        BleManager.getInstance().write(mBleDevice, BleService.Service_uuid, BleService.Characteristic_uuid_TX, data, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                Log.d(Tag,"write successful");
            }

            @Override
            public void onWriteFailure(BleException exception) {
                Log.d(Tag,"write fail");

            }
        });
    }

    PhasedSeekBar psbHorizontal;
    // 三状态开关
    private void initPhasedSeekBar() {
       // masterSwitchLayout = (LinearLayout) findViewById(R.id.master_switch_layout);

        final Resources resources = getResources();
        psbHorizontal = findViewById(R.id.psb_hor);
        psbHorizontal.setAdapter(new SimplePhasedAdapter(resources, new int[]{
                R.drawable.device_on,
                R.drawable.device_off,
                R.drawable.device_auto}));
        psbHorizontal.setVisibility(View.INVISIBLE);

        psbHorizontal.setListener(new PhasedListener() {
            @Override
            public void onPositionSelected(int position) {
                Log.i(Tag, String.format("onPositionSelected: %d", position));

                switch (position)
                {
                    case 0:
                        sendCmd("ON".getBytes());
                        break;
                    case 1:
                        sendCmd("OFF".getBytes());
                        break;
                    case 2:
                        sendCmd("AUTO".getBytes());
                        break;
                        default:
                            break;
                }




            }
        });



        psbHorizontal.setPosition(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BleManager.getInstance().disconnect(mBleDevice);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_old,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home)
        {
            finish();
        }
        else if(item.getItemId()==R.id.delaySetting)
        {
            Log.d(Tag,"delay click");

            List<String> datas=new ArrayList<>();
            for(int i=0;i<255;i++)
            {
                datas.add(String.valueOf(i/10f));
            }
            final NumberWheel numberWheel=new NumberWheel(DeviceControlOld.this, getResources().getString(R.string.dialog_title_set_delay), datas,close_delay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==-2)
                    {
                        Log.d(Tag,"item:"+((NumberWheel)dialog).getSelectedItem());
                        close_delay=(int)(Double.parseDouble(((NumberWheel)dialog).getSelectedItem())*10);
                        byte[] cmd=" TIME".getBytes();
                        cmd[0]=(byte)close_delay;

                        sendCmd(cmd);
                    }
                }
            });
            numberWheel.show();
        }
        else if(item.getItemId()==R.id.autoOnSpeedSetting)
        {
            Log.d(Tag,"speed click");
            List<String> datas=new ArrayList<>();
            for(int i=1000;i<10000;i+=50)
            {
                datas.add(String.valueOf(i));
            }
            NumberWheel numberWheel=new NumberWheel(DeviceControlOld.this, getResources().getString(R.string.dialog_title_set_speed), datas,(open_speed-1000)/50, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==-2)
                    {
                        Log.d(Tag,"item:"+((NumberWheel)dialog).getSelectedItem());
                        open_speed=Integer.parseInt(((NumberWheel)dialog).getSelectedItem());
                        byte[] cmd=" SET".getBytes();
                        cmd[0]=(byte)(open_speed/50);
                        sendCmd(cmd);
                    }



                }
            });
            numberWheel.show();
        }







        return super.onOptionsItemSelected(item);
    }
}
