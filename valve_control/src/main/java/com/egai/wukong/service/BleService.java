package com.egai.wukong.service;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.egai.ble.valveControl.R;
import com.egai.wukong.EventBusMessage.BleEvent;
import com.egai.wukong.EventBusMessage.DeviceControlEvent;
import com.egai.wukong.operation.AtCmdParser;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;

public class BleService extends Service {
    private String tag = "BleService";

    public static String Service_uuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_E2 = "0000ffe2-0000-1000-8000-00805f9b34fb";
    ProgressBar progressBar;
    private BleDevice mBleDevice;
    private PipedReader pipedReader;
    private PipedWriter pipedWriter;
    private BufferedReader bufferedReader;
    private Thread readThread;

    public BleService() {

    }
//    Intent intent=new Intent(MainActivity.this,DeviceControl.class);
//                intent.putExtra("bleDevice",mDeviceAdapter.getItem(position));
//    startActivity(intent);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(tag, "service create!");
        EventBus.getDefault().register(this);
        progressBar = new ProgressBar(getApplicationContext());


        pipedWriter = new PipedWriter();
        try {
            pipedReader = new PipedReader(pipedWriter);
            bufferedReader = new BufferedReader(pipedReader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {

                String line;


                while (true) {
                    try {

                        line = bufferedReader.readLine();
                        Log.d(tag, "line:" + line);
                        Object event = AtCmdParser.parse(line);

                        if (event instanceof DeviceControlEvent) {
                            EventBus.getDefault().post(event);
                        }


                    } catch (IOException e) {
                        Log.d(tag, "readThread exit!");
                        break;


                    }
                }


            }
        });


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BleManager.getInstance().isConnected(mBleDevice)) {
            BleManager.getInstance().disconnect(mBleDevice);
        }
        readThread.interrupt();
        Log.d(tag, "service destroy!");
        EventBus.getDefault().unregister(this);
        try {
            bufferedReader.close();
            pipedReader.close();
            pipedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Subscribe
    public void onMessage(BleEvent myEvent) {
        Log.d(tag, "cmd" + myEvent.getCmd());
        switch (myEvent.getCmd()) {
            case BleEvent.CMD_SEND_AT_CMD:
                if (mBleDevice != null) {
                    BleManager.getInstance().write(mBleDevice, Service_uuid, Characteristic_uuid_TX, (myEvent.getData().toString() + "\r\n").getBytes(), new BleWriteCallback() {
                        @Override
                        public void onWriteSuccess(int current, int total, byte[] justWrite) {
                            Log.d(tag, "write successful");
                        }

                        @Override
                        public void onWriteFailure(BleException exception) {
                            Log.d(tag, "write failed!");

                        }
                    });
                }
                break;
            case BleEvent.CMD_STOP_CONNECT:
                if (mBleDevice != null) {
                    if (BleManager.getInstance().isConnected(mBleDevice)) {
                        BleManager.getInstance().disconnect(mBleDevice);
                    }
                }
                break;
            case BleEvent.CMD_START_CONNECT:
                BleDevice newBleDevice = (BleDevice) myEvent.getData();
                if (mBleDevice != null) {

                    if (newBleDevice.getMac().equals(mBleDevice.getMac())) {
                        if (BleManager.getInstance().isConnected(mBleDevice)) {
                            break;
                        }

                    }
                    //是新的蓝牙设备，把原来的断开。
                    else {
                        BleManager.getInstance().disconnect(mBleDevice);

                    }


                }
                mBleDevice = newBleDevice;


                BleManager.getInstance().connect(mBleDevice, new BleGattCallback() {
                    @Override
                    public void onStartConnect() {
                        Log.d(tag, "connect started!");
                    }

                    @Override
                    public void onConnectFail(BleDevice bleDevice, BleException exception) {
                        Log.d(tag, "connect failed!");
                        EventBus.getDefault().post(new BleEvent(BleEvent.BLE_EVENT_CONNECTION_FAIL, null));

                    }

                    @Override
                    public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {

                        Log.d(tag, "connect successful!");

                        if (readThread.getState() == Thread.State.NEW || readThread.getState() == Thread.State.TERMINATED) {
                            readThread.start();
                        }


                        BleManager.getInstance().notify(bleDevice, Service_uuid, Characteristic_uuid_TX, new BleNotifyCallback() {
                            @Override
                            public void onNotifySuccess() {
                                Log.d(tag, "notify sucessful!");
                                EventBus.getDefault().post(new BleEvent(BleEvent.BLE_EVENT_CONNECTED, mBleDevice.getMac()));


                            }

                            @Override
                            public void onNotifyFailure(BleException exception) {
                                Log.d(tag, "notify failed!");
                                EventBus.getDefault().post(new BleEvent(BleEvent.BLE_EVENT_CONNECTION_FAIL, null));

                            }

                            @Override
                            public void onCharacteristicChanged(byte[] data) {
                                Log.d(tag, "character changed!" + new String(data));

                                try {
                                    pipedWriter.write(new String(data));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }
                        });

                    }

                    @Override
                    public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                        Log.d(tag, "disconnect!");
                        //连接非正常断开时，要把通知使用者。正常断开时，由使用者处理。
                        if (!isActiveDisConnected) {
                            EventBus.getDefault().post(new DeviceControlEvent(DeviceControlEvent.CMD_CONNECT_LOSE, null));
                        }
                        Toast.makeText(getApplicationContext(), R.string.active_disconnected, Toast.LENGTH_LONG).show();


                    }
                });
                break;


        }


    }

    @Override
    public IBinder onBind(Intent intent) {

        throw new UnsupportedOperationException("Not yet implemented");
    }
}
