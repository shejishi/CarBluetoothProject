package com.egai.wukong.tools;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.egai.wukong.EventBusMessage.FirmWareUpdataEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedReader;
import java.io.PipedWriter;

public class FirmWareUpdata {
    private String deviceMac;
    private final String Tag="FirmWareUpdata";
    public static String Service_uuid = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_TX = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static String Characteristic_uuid_E2 = "0000ffe2-0000-1000-8000-00805f9b34fb";
    private BleDevice mBleDevice;
    private String binFileName;
    FileInputStream  fileInputStream;

    private PipedInputStream pipedInputStream;
    private PipedOutputStream pipedOutputStream;
    private Thread rcvThread;
    private Context context;


    public FirmWareUpdata(@NonNull String mac, @NonNull String binFileName,@NonNull Context context) {
        this.deviceMac=mac;
        this.binFileName=binFileName;
        this.context=context;
        try {
            fileInputStream=new FileInputStream(context.getFilesDir()+"/"+binFileName);
            Log.d(Tag,"fileinputStream OK");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }
    private void send_data(byte[] data)
    {



        BleManager.getInstance().write(mBleDevice, Service_uuid, Characteristic_uuid_TX, data, new BleWriteCallback() {
            @Override
            public void onWriteSuccess(int current, int total, byte[] justWrite) {
                Log.d(Tag,"write successful"+"current:"+current+"total:"+total);

            }

            @Override
            public void onWriteFailure(BleException exception) {
                Log.d(Tag,"write fail");

            }
        });
    }



    public void start()
    {
        BleManager.getInstance().connect(deviceMac, new BleGattCallback() {
            @Override
            public void onStartConnect() {
                Log.d(Tag,"connect start");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                Log.d(Tag,"connect fail");

                EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_updata_error,"设备连接失败，请确认设备是否打开"));

            }

            @Override
            public void onConnectSuccess(final BleDevice bleDevice, BluetoothGatt gatt, int status) {

                Log.d(Tag,"connect successful");
                BleManager.getInstance().notify(bleDevice, Service_uuid, Characteristic_uuid_TX, new BleNotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        mBleDevice=bleDevice;
                        Log.d(Tag,"notify success");
                        rcvThread=new Thread(new Runnable() {

                            @Override
                            public void run() {
                                int timeout=0;

                                EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_process_change,5));






                                try{
                                    Log.d(Tag,"rcv thread start");
                                    byte[] read_buf=new byte[1024];
                                    send_data("ATUPDATA\r\n".getBytes());
                                    Thread.sleep(1000);



                                    while(true)
                                    {

                                        //1,发送同步指令





                                        send_data(new Frame(Frame.CMD_TYPE_Start,0xb5,null,0).make());
                                        Thread.sleep(2000);
                                        //检测超时
                                        if(timeout++>5)
                                        {
                                            EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_updata_error,"超时"));
                                            throw new Exception("时间超时");
                                        }
                                        int len=pipedInputStream.available();




                                        if(len>0)
                                        {

                                            pipedInputStream.read(read_buf,0,len);
                                            Log.d(Tag,"len:"+len);
                                            Log.d(Tag,"len:"+read_buf[0]+" "+read_buf[1]+" "+read_buf[2]+" "+read_buf[3]);



                                            if(read_buf[0]==Frame.CMD_TYPE_Start_ACK && read_buf[1]==(byte) Frame.Protocol_Pwd)
                                            {
                                                Log.d(Tag,"检测到同步指令");
                                                break;
                                            }
                                        }
                                    }

                                    //2，发送本次传输类型



                                    send_data(new Frame(Frame.CMD_TYPE_Transport_type,1,null,0).make());
                                    int len =0;
                                    timeout=0;
                                    while(true)
                                    {
                                        Thread.sleep(100);
                                        if(timeout++>20)
                                        {
                                            EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_updata_error,"超时"));
                                            throw new Exception("时间超时");
                                        }
                                        len = pipedInputStream.available();
                                        if(len>0) break;

                                    }

                                    pipedInputStream.read(read_buf,0,len);

                                    if(read_buf[0]!=Frame.CMD_TYPE_Transport_type_ack)
                                    {
                                        EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_updata_error,"不支持此操作"));
                                        throw new Exception("设备不支持设备更新操作");
                                    }


                                    //3，发送本次传输的文件大小。

                                    int binFileLen= fileInputStream.available();
                                    Log.d(Tag,"fileLen:"+binFileLen);
                                    send_data(new Frame(Frame.CMD_TYPE_File_Info,0,new byte[]{(byte)binFileLen,(byte) (binFileLen>>8),(byte) (binFileLen>>16),(byte) (binFileLen>>24),},4).make());
                                    Thread.sleep(800);
                                    len=pipedInputStream.available();
                                    if(len>0)
                                    {
                                        pipedInputStream.read(read_buf,0,len);
                                        if(!(read_buf[0]==6))
                                        {
                                            throw new Exception("时间超时");
                                        }
                                    }
                                    Log.d(Tag,"开始发送数据包");
                                    int send_cnt=0;

                                    while(true)
                                    {


                                        len=fileInputStream.read(read_buf,0,100);
                                        if(len>0)
                                        {



                                            send_data(new Frame(Frame.CMD_TYPE_Pack,0,read_buf,len).make());


                                            timeout=0;
                                            while (true)
                                            {
                                                int ackLen=0;
                                                Thread.sleep(10);
                                                ackLen=pipedInputStream.available();
                                                if (ackLen>0)
                                                {
                                                    byte[] tempbuf=new byte[ackLen];
                                                    pipedInputStream.read(tempbuf,0,ackLen);
                                                    if(tempbuf[0]==Frame.CMD_TYPE_Pack_ack)
                                                    {
                                                        break;
                                                    }
                                                    else if(tempbuf[0]==12)//数据包校验出错
                                                    {
                                                        //
                                                        Log.d(Tag,"数据包接收出错，重新发送一次。");
                                                        //throw new Exception("校验出错");
                                                        send_data(new Frame(Frame.CMD_TYPE_Pack,0,read_buf,len).make());
                                                    }


                                                }
                                                if(timeout++>300)
                                                {
                                                    EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_updata_error,"数据包ACK等待超时"));
                                                    throw new Exception("时间超时");
                                                }
                                            }
                                            send_cnt+=len;
                                            EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_process_change,(int)(send_cnt/(float)binFileLen*94+5)));
                                            Log.d(Tag,"total send:"+send_cnt);
                                            //







                                        }
                                        else
                                        {
                                            break;
                                        }





                                    }

                                    //发送end包

                                    Thread.sleep(100);
                                    send_data(new Frame(Frame.CMD_TYPE_End,0,null,0).make());

                                    Thread.sleep(500);
                                    len=pipedInputStream.available();
                                    if(len>0)
                                    {
                                        pipedInputStream.read(read_buf,0,len);
                                        if(read_buf[0]!=Frame.CMD_TYPE_End_ack)
                                        {
                                            throw new Exception("末尾出错!");
                                        }
                                    }
                                    else
                                    {
                                        throw new Exception("ack超时!");
                                    }



                                    EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_updata_successful,"升级成功!"));













                                }
                                catch (Exception e)
                                {
                                    e.printStackTrace();
                                    EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_updata_error,e.toString()));

                                }
                                finally {
                                    BleManager.getInstance().disconnect(mBleDevice);
                                    try {
                                        pipedOutputStream.close();
                                        pipedInputStream.close();
                                        fileInputStream.close();
                                    } catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }



                            }
                        });

                        pipedOutputStream=new PipedOutputStream();
                        try {
                            pipedInputStream=new PipedInputStream(pipedOutputStream);
                            rcvThread.start();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                    }

                    @Override
                    public void onNotifyFailure(BleException exception) {
                        Log.d(Tag,"notify Faile");

                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {


                        Log.d(Tag,"data change");
                        StringBuilder sb=new StringBuilder();
                        for(int i=0;i<data.length;i++)
                        {
                            sb.append(Integer.toHexString(0xff&data[i]));
                            sb.append(',');
                        }
                        Log.d(Tag,sb.toString());
                        try {

                            pipedOutputStream.write(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                });

            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                Log.d(Tag,"disConnected");

            }
        });
    }
    private static class Frame{


        public static final int CMD_TYPE_Start=1;
        public static final int CMD_TYPE_Start_ACK=2;
        public static final int CMD_TYPE_Transport_type=3;
        public static final int CMD_TYPE_Transport_type_ack=4;
        public static final int CMD_TYPE_File_Info=5;
        public static final int CMD_TYPE_File_Info_ack=6;
        public static final int CMD_TYPE_Pack=7;
        public static final int CMD_TYPE_Pack_ack=8;
        public static final int CMD_TYPE_End=9;
        public static final int CMD_TYPE_End_ack=10;
        public static final int CMD_TYPE_Error=11;
        public static final int CMD_TYPE_Check_Error=12;
        public static final int Protocol_Pwd=0xb5;



        private int cmd_type;
        private int cmd_param;
        private int data_len;
        private byte[]  data;

        public Frame(int cmd_type,int cmd_param,byte[] data,int data_len) {
            this.cmd_type=cmd_type;
            this.cmd_param=cmd_param;
            this.data=data;
            this.data_len=data_len;

        }

        public byte[] make()
        {
            int frame_len=5+data_len;
            byte[] buf=new byte[frame_len];
            buf[0]=(byte)cmd_type;
            buf[1]=(byte)cmd_param;
            buf[2]=(byte)(data_len>>8);//hight
            buf[3]=(byte)data_len;//low
            if(data_len==0)
            {
             buf[4]=0;
            }
            else
            {
                byte sum=0;
                for(int i=0;i<data_len;i++)
                {
                    buf[5+i]=data[i];
                    sum+=data[i];
                }
                buf[4]=sum;
            }

            return buf;
        }
    }


}
