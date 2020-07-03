package com.egai.wukong.operation;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.egai.ble.valveControl.R;
import com.egai.wukong.CircleTransform;
import com.egai.wukong.EventBusMessage.AppUpdataEvent;
import com.egai.wukong.EventBusMessage.FirmWareUpdataEvent;
import com.egai.wukong.MyPreferences;
import com.egai.wukong.listsettingview.LSettingItem;
import com.egai.wukong.tools.APKVersionCodeUtils;
import com.egai.wukong.tools.DownloadUtil;
import com.egai.wukong.tools.FirmWareUpdata;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;


public class Settings extends AppCompatActivity {

    private final String Tag="Settings";
    private LSettingItem mSettingitemAutoConnect;
    private LSettingItem mSettingItemFirmWareUpdata;
    private LSettingItem mSettingItemAppUpdata;
    private LSettingItem mSettingItemAbout;
    private ImageView mIvHead;
    private MyPreferences myPreferences;
    private String mDeviceMac;
    private String serverAddr="http://server.egai.com:8080";




    private ProgressDialog progressDialog;
    private ProgressDialog checkDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        setContentView(R.layout.activity_settings);
       mSettingitemAutoConnect=  findViewById(R.id.item_auto_contect);
        myPreferences=new MyPreferences(this);
       boolean autoConnect=myPreferences.getBoolean("autoConnect",true);
       mSettingitemAutoConnect.setItemCheck(autoConnect);
       mSettingItemFirmWareUpdata = findViewById(R.id.item_firmUpdata);
       mSettingItemAppUpdata = findViewById(R.id.item_app_updata);
        mIvHead =  findViewById(R.id.headimage);
        checkDialog = new ProgressDialog(Settings.this);//1.创建一个ProgressDialog的实例
        mSettingitemAutoConnect.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                    myPreferences.putBoolean("autoConnect",isChecked);
            }
        });
        mSettingItemFirmWareUpdata.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {


                mDeviceMac=myPreferences.getString("savedMac","");



                if(mDeviceMac.isEmpty())
                {
                    new AlertDialog.Builder(Settings.this).setTitle(R.string.notifyTitle)
                            .setMessage(R.string.needMac)
                            .setPositiveButton(R.string.OK, null)
                            .show();
                    return;
                }
                if(myPreferences.getString(MyPreferences.firmWareVersion,"").isEmpty())
                {
                    new AlertDialog.Builder(Settings.this).setTitle(R.string.notifyTitle)
                            .setMessage(R.string.noVersionInfo)
                            .setPositiveButton(R.string.OK, null)
                            .show();
                    return;
                }


                checkDialog.setTitle(getResources().getString(R.string.notifyTitle));//2.设置标题
                checkDialog.setMessage(getResources().getString(R.string.checkDialog_content));//3.设置显示内容
                checkDialog.setCancelable(false);//4.设置可否用back键关闭对话框
                checkDialog.setCanceledOnTouchOutside(false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkDialog.dismiss();
                    }
                }, 30000);
                checkDialog.show();

               final String url=serverAddr+"/updata/FirmWare/"+myPreferences.getString(MyPreferences.productName,"");


                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                       DownloadUtil.get().checkNewVersion(url, new DownloadUtil.onCheckListener() {
                           @Override
                           public void onCheckSuccess(String fileName,String newVersion,long versionCode) {
                               Log.d(Tag,"new Version:"+newVersion);


                               checkDialog.dismiss();
                              final String latestVersion=newVersion;

                              //名字示例:BleControl_v4.0.bin

                                   runOnUiThread(new Runnable() {
                                       @Override
                                       public void run() {


                                           String message=getResources().getString(R.string.firmUpdateMessage)+mDeviceMac+"\r\n" + "设备名称:"+myPreferences.getString(MyPreferences.productName,"")+"\r\n"+getResources().getString(R.string.currentVersion)+myPreferences.getString(MyPreferences.firmWareVersion,"")+"\r\n"+getResources().getString(R.string.newVersion)+latestVersion;
                                           new AlertDialog.Builder(Settings.this)
                                                   .setTitle(R.string.notifyTitle)
                                                   .setMessage(message)
                                                   .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                                       @Override
                                                       public void onClick(DialogInterface dialog, int which) {

                                                           Log.d(Tag,"url:"+url);
                                                           new Thread(new Runnable() {
                                                               @Override
                                                               public void run() {

                                                                   DownloadUtil.get().download(url, getFilesDir().getPath(), new DownloadUtil.OnDownloadListener() {
                                                                       @Override
                                                                       public void onDownloadSuccess(File file) {
                                                                           EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_download_successful,file.getName()));

                                                                       }

                                                                       @Override
                                                                       public void onDownloading(int progress) {

                                                                       }

                                                                       @Override
                                                                       public void onDownloadFailed(Exception e) {
                                                                           EventBus.getDefault().post(new FirmWareUpdataEvent(FirmWareUpdataEvent.cmd_updata_error,"固件下载失败!"));

                                                                       }
                                                                   });


                                                               }
                                                           }).start();

                                                           progressDialog.setProgress(0);
                                                           progressDialog.show();


                                                       }
                                                   })
                                                   .setNegativeButton(R.string.cancel,null)
                                                   .show();
                                       }
                                   });


                               }







                           @Override
                           public void onCheckFail(String message) {
                               Log.d(Tag,message);
                               checkDialog.dismiss();
                               final String reason=message;
                               runOnUiThread(new Runnable() {
                                   @Override
                                   public void run() {
                                       Toast.makeText(Settings.this,"更新检测失败:"+reason,Toast.LENGTH_LONG).show();
                                   }
                               });


                           }
                       });
                    }
                }).start();




            }
        });

        mSettingItemAppUpdata.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(boolean isChecked) {
                final String appVerName=APKVersionCodeUtils.getVerName(Settings.this);
                final long appVerCode=APKVersionCodeUtils.getVersionCode(Settings.this);

                final String url=serverAddr+"/updata/App/valve_control";
                Log.d(Tag,"versionname:"+APKVersionCodeUtils.getVerName(Settings.this));
                Log.d(Tag,"versionCode:"+APKVersionCodeUtils.getVersionCode(Settings.this));
                checkDialog.setTitle(getResources().getString(R.string.notifyTitle));//2.设置标题
                checkDialog.setMessage(getResources().getString(R.string.checkDialog_content));//3.设置显示内容
                checkDialog.setCancelable(false);//4.设置可否用back键关闭对话框
                checkDialog.setCanceledOnTouchOutside(false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkDialog.dismiss();
                    }
                }, 30000);
                checkDialog.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        DownloadUtil.get().checkNewVersion(url, new DownloadUtil.onCheckListener() {
                            @Override
                            public void onCheckSuccess(String fileName,String newVersion,long newversionCode) {
                                checkDialog.dismiss();
                                Log.d(Tag,"new Version:"+newVersion);

                                if(newversionCode>appVerCode )
                                {
                                    //可以更新

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new AlertDialog.Builder(Settings.this)
                                                    .setTitle(R.string.notifyTitle)
                                                    .setMessage("有更新版本的APP,是否下载？")
                                                    .setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            final Intent intent=new Intent();
                                                            intent.setAction(Intent.ACTION_VIEW);
                                                            intent.setData(Uri.parse(url));
                                                            if (intent.resolveActivity(Settings.this.getPackageManager()) != null) {
                                                                final ComponentName componentName = intent.resolveActivity(Settings.this.getPackageManager());
                                                                // 打印Log   ComponentName到底是什么

                                                                Settings.this.startActivity(Intent.createChooser(intent, "请选择浏览器下载更新"));
                                                            } else {
                                                                Toast.makeText(Settings.this.getApplicationContext(), "请下载浏览器", Toast.LENGTH_SHORT).show();
                                                            }


                                                        }
                                                    })
                                                    .setNegativeButton(R.string.cancel,null)
                                                    .show();
                                        }
                                    });










                                }
                                else
                                {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(Settings.this,"已经是最新版了!",Toast.LENGTH_LONG).show();
                                        }
                                    });
                                }








                            }

                            @Override
                            public void onCheckFail(String message) {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(Settings.this,"更新检查失败",Toast.LENGTH_LONG).show();
                                    }
                                });
                                checkDialog.dismiss();
                            }
                        });
                    }
                }).start();








            }
        });

        Picasso.with(this).load(R.drawable.logo).transform(new CircleTransform()).into(mIvHead);

        progressDialog = new ProgressDialog(Settings.this);

        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

        progressDialog.setCancelable(false);

        progressDialog.setCanceledOnTouchOutside(false);


        progressDialog.setTitle("正在更新");

        progressDialog.setMessage("请稍后...");

        progressDialog.setMax(100);





    }


    @Subscribe (threadMode=ThreadMode.MAIN)
    public void handFirmWareUpdataMessage(FirmWareUpdataEvent firmWareUpdataEvent)
    {
        switch (firmWareUpdataEvent.getCmd_type())
        {

            case FirmWareUpdataEvent.cmd_download_successful:

                new FirmWareUpdata(mDeviceMac,firmWareUpdataEvent.getData().toString(),Settings.this).start();

                break;
             case FirmWareUpdataEvent.cmd_process_change:
                   progressDialog.setProgress(Integer.parseInt(firmWareUpdataEvent.getData().toString()));
                    break;
            case FirmWareUpdataEvent.cmd_updata_error:
                Object data=firmWareUpdataEvent.getData();
                data=data==null?"":firmWareUpdataEvent.getData().toString();
                checkDialog.dismiss();

                Log.d(Tag,"updata error:"+data);
                progressDialog.dismiss();
                new AlertDialog.Builder(Settings.this).setTitle(R.string.notifyTitle)
                        .setMessage(data.toString())
                        .setPositiveButton(R.string.OK, null)
                        .show();


            break;
            case FirmWareUpdataEvent.cmd_updata_successful:
                Log.d(Tag,"升级成功!");
                progressDialog.dismiss();
                new AlertDialog.Builder(Settings.this).setTitle(R.string.notifyTitle)
                        .setMessage(firmWareUpdataEvent.getData().toString())
                        .setPositiveButton(R.string.OK, null)
                        .show();
                break;


        }


    }

    @Subscribe (threadMode=ThreadMode.MAIN)
    public void handAppUpdataMessage(AppUpdataEvent appUpdataEvent){



    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

}
