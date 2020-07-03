package com.egai.wukong.operation;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.egai.ble.valveControl.R;
import com.egai.wukong.EventBusMessage.BleEvent;
import com.egai.wukong.EventBusMessage.DataStream;
import com.egai.wukong.EventBusMessage.DeviceControlEvent;
import com.egai.wukong.EventBusMessage.SingleValue;
import com.egai.wukong.MyPreferences;
import com.egai.wukong.WheelView.NumberWheel;
import com.egai.wukong.tools.ButtonUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import static org.greenrobot.eventbus.ThreadMode.MAIN;

public class DeviceControl  extends AppCompatActivity  {

    private String tag="DeviceControl";
    private Context context;



    ImageButton im_btn_auto,im_btn_on,im_btn_off,im_btn_delay,im_btn_speed,im_btn_back;
    ImageView img_view_calibration,img_view_background,img_view_zhizhen_motor_speed,img_view_zhizhen_car_speed;
    ProgressBar progressBar_water_temp,progressBar_oil_temp;
    TextView textView_car_speed,textView_motor_speed;

    private boolean isLayoutCompleted=false;

    private int car_speed=0,motor_speed=0,oil_tmp=0,water_tmp=0,close_delay=0,open_speed=1000;

     private static final int  animator_duration= 1000;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        EventBus.getDefault().register(this);
        context=this;
        Log.d(tag,"onCreate");
        init_view();

        Handler handler= new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        },1000);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        },1100);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        },1100);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                    sendAtCmd("ATGETRPM");
                    Thread.sleep(100);
                    sendAtCmd(("ATGETDELAY"));
                    Thread.sleep(100);
                    sendAtCmd("ATGETSTATA");
                    Thread.sleep(100);
                    sendAtCmd("ATGETVER");
                    Thread.sleep(1000);
                    sendAtCmd("ATRON");

                }catch (Exception e)
                {

                }



            }
        }).start();








    }
/*
把实际的转速或者车速转换成表盘的数值。
 */
    int watch_convert(int value,int which)
    {


        if(which==0)//车速
        {


            return value-150;
        }
        else if(which==1)//转速
        {

            return (int)(value*180/5000-180);
        }

        return 0;

    }

    public void handleSingleValue(SingleValue singleValue)
    {

        Log.d(tag,"singleValue:"+singleValue.getWhich());

        switch (singleValue.getWhich())
        {
            case SingleValue.DELAY:
                close_delay=Integer.parseInt(singleValue.getData().toString());

            break;
            case SingleValue.RPM:
                open_speed=Integer.parseInt(singleValue.getData().toString());
            break;
            case SingleValue.STATA:
                //
                int data=Integer.parseInt(singleValue.getData().toString());
                boolean isAuto=data%2==0?false:true;
                boolean isOpen=data>9?true:false;
                set_btn(im_btn_auto,isAuto);
                set_btn(im_btn_off,!isOpen);
                set_btn(im_btn_on,isOpen);

            break;
            case SingleValue.VER:
                //示例:$VER=BleCtrlCentr_WireUse_20181112_v1.0-beta,这里收到的是=后面的字串

                String[] info=singleValue.getData().toString().split("_");

                if(info.length>=3)
                {
                    String productName=info[0]+"_"+info[1];
                    String buildDate=info[2];
                    String version=info[3];
                    MyPreferences myPreferences= new MyPreferences(this);
                    myPreferences.putString(MyPreferences.productName,productName);
                    myPreferences.putString(MyPreferences.firmWareBuildDate,buildDate);
                    myPreferences.putString(MyPreferences.firmWareVersion,version);

                    Log.d("BleControl","deviceInfo:"+productName+buildDate+version);


                }






                break;
            default:
            break;
        }
    }
    @Subscribe(threadMode=MAIN )
    public void handleMessage(DeviceControlEvent deviceControlEvent)
    {

        switch (deviceControlEvent.getCMD())
        {
            case DeviceControlEvent.CMD_DATA_STREAM:
                DataStream dataStream=(DataStream)deviceControlEvent.getData();
                Log.d(tag,"dataEvent:"+dataStream.toString());
                int temp=0;


                temp=dataStream.getOil_tmp();
                if(temp>=0 &&oil_tmp!=temp )
                {
                    oil_tmp=dataStream.getOil_tmp();
                    oil_tmp=Math.min(oil_tmp,160);
                    ObjectAnimator objectAnimator=ObjectAnimator.ofInt(progressBar_oil_temp,"Progress",progressBar_oil_temp.getProgress(),oil_tmp);
                    objectAnimator.setDuration(animator_duration);
                    objectAnimator.start();



                }
                temp=dataStream.getWater_tmp();
                if(temp>=0 && water_tmp!=temp)
                {
                    water_tmp=dataStream.getWater_tmp();
                    water_tmp=Math.min(water_tmp,160);
                    ObjectAnimator objectAnimator=ObjectAnimator.ofInt(progressBar_water_temp,"Progress",progressBar_water_temp.getProgress(),water_tmp);
                    objectAnimator.setDuration(animator_duration);
                    objectAnimator.start();


                }
                temp= dataStream.getCar_speed();
                if(temp>=0 && car_speed!=temp)
                {
                    car_speed=dataStream.getCar_speed();
                    car_speed=Math.min(car_speed,350);

                    ValueAnimator valueAnimator=ValueAnimator.ofInt(Integer.parseInt(textView_car_speed.getText().toString()),car_speed);
                    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            textView_car_speed.setText(animation.getAnimatedValue().toString());
                        }
                    });

                    ObjectAnimator objectAnimator=ObjectAnimator.ofFloat(img_view_zhizhen_car_speed,"Rotation",img_view_zhizhen_car_speed.getRotation(),watch_convert(car_speed,0));

                    AnimatorSet animatorSet=new AnimatorSet();
                    animatorSet.play(valueAnimator).with(objectAnimator);
                    animatorSet.setDuration(animator_duration);
                    animatorSet.start();



                }
                temp=dataStream.getMotor_speed();
                if(temp>=0 && motor_speed!=temp)
                {

                    motor_speed=dataStream.getMotor_speed();
                    motor_speed=Math.min(motor_speed,9000);
                    ValueAnimator valueAnimator=ValueAnimator.ofInt(Integer.parseInt(textView_motor_speed.getText().toString()),motor_speed);
                    valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            textView_motor_speed.setText(animation.getAnimatedValue().toString());
                        }
                    });



                    ObjectAnimator objectAnimator=ObjectAnimator.ofFloat(img_view_zhizhen_motor_speed,"Rotation",img_view_zhizhen_motor_speed.getRotation(),watch_convert(motor_speed,1));

                    AnimatorSet animatorSet=new AnimatorSet();
                    animatorSet.play(valueAnimator).with(objectAnimator);
                    animatorSet.setDuration(animator_duration);
                    animatorSet.start();



                }



                break;
                case DeviceControlEvent.CMD_CONNECT_LOSE:
                    finish();
                    break;
                case DeviceControlEvent.CMD_SINGLE_VALUE:
                    handleSingleValue((SingleValue) deviceControlEvent.getData());
                    break;
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(tag,"onDestroy");
        sendAtCmd("ATROFF");
        EventBus.getDefault().post(new BleEvent(BleEvent.CMD_STOP_CONNECT,null));
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(tag,"onStop");
        finish();
    }

    private void set_btn(View v, boolean on_off)
    {
        ImageButton img_btn=(ImageButton)v;
        if(on_off){
          //  img_btn.setImageResource((int)img_btn.getTag(R.id.on));
            img_btn.setImageDrawable((Drawable) img_btn.getTag(R.id.on));

        }
        else {
            img_btn.setImageDrawable((Drawable)img_btn.getTag(R.id.off));
        }
        img_btn.setTag(on_off);

    }
    private boolean get_btn_stata(View v)
    {
        ImageButton btn=(ImageButton)v;
        if(btn.getTag()!=null)
        {
            return (boolean)btn.getTag();
        }
        return false;
    }

    View.OnClickListener control_btnClicked=new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            switch (v.getId())
            {
                case R.id.img_btn_AUTO:
                    if(!get_btn_stata(v))
                    {
                        sendAtCmd("ATAUTO");
                        set_btn(v,true);
                    }





                    break;
                case R.id.img_btn_on:
                    if(!get_btn_stata(v))
                    {
                        sendAtCmd("ATON");
                        set_btn(v,true);
                        set_btn(im_btn_off,false);
                        set_btn(im_btn_auto,false);
                    }




                    break;
                case R.id.img_btn_off:
                   if(!get_btn_stata(v))
                   {
                       sendAtCmd("ATOFF");
                       set_btn(v,true);
                       set_btn(im_btn_on,false);
                       set_btn(im_btn_auto,false);

                   }





                    break;

            }



        }
    };

    private void sendAtCmd(String cmd)
    {
        EventBus.getDefault().post(new BleEvent(BleEvent.CMD_SEND_AT_CMD,cmd));
    }



    private void init_view()
    {






        //初始化ImageButton
        im_btn_auto =findViewById(R.id.img_btn_AUTO);
        im_btn_on   =findViewById(R.id.img_btn_on);
        im_btn_off  =findViewById(R.id.img_btn_off);
        im_btn_delay=findViewById(R.id.img_btn_delay);
        im_btn_speed=findViewById(R.id.img_btn_speed);
        im_btn_back=findViewById(R.id.img_btn_back) ;
        progressBar_oil_temp=findViewById(R.id.progress_oli_tmp);
        progressBar_water_temp=findViewById(R.id.progress_water_tmp);
        img_view_calibration=findViewById(R.id.imageView_calibration);
        img_view_background=findViewById(R.id.imageView_background);
        img_view_zhizhen_motor_speed=findViewById(R.id.imageView_zhizhen_motorSpeed);
        img_view_zhizhen_car_speed=findViewById(R.id.imageView_zhizhen_carSpeed);
        textView_car_speed=findViewById(R.id.txtView_car_speed);
        textView_motor_speed=findViewById(R.id.txtView_motor_speed);



        im_btn_auto.setTag(R.id.on,getResources().getDrawable(R.mipmap.auto2));
        im_btn_auto.setTag(R.id.off,getResources().getDrawable(R.mipmap.auto));
        im_btn_on.setTag(R.id.on,getResources().getDrawable(R.mipmap.on2));
        im_btn_on.setTag(R.id.off,getResources().getDrawable(R.mipmap.on));
        im_btn_off.setTag(R.id.on,getResources().getDrawable(R.mipmap.off2));
        im_btn_off.setTag(R.id.off,getResources().getDrawable(R.mipmap.off));





        im_btn_auto.setOnClickListener(control_btnClicked);
        set_btn((View)im_btn_auto,false);
        im_btn_on.setOnClickListener(control_btnClicked);
        set_btn((View)im_btn_on,false);
        im_btn_off.setOnClickListener(control_btnClicked);
        set_btn((View)im_btn_off,true);








        im_btn_delay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                    List<String> datas=new ArrayList<String>();
                    for(int i=0;i<255;i++)
                    {
                        datas.add(String.valueOf(i/10f));
                    }
                    final NumberWheel numberWheel=new NumberWheel(context, getResources().getString(R.string.dialog_title_set_delay), datas,close_delay, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(which==-2)
                            {
                                Log.d(tag,"item:"+((NumberWheel)dialog).getSelectedItem());
                                close_delay=(int)(Double.parseDouble(((NumberWheel)dialog).getSelectedItem())*10);
                                sendAtCmd("ATSETDELAY"+close_delay);

                            }
                        }
                    });
                    numberWheel.show();


            }
        });
        im_btn_speed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<String> datas=new ArrayList<String>();
                for(int i=1000;i<10000;i+=50)
                {
                    datas.add(String.valueOf(i));
                }
                NumberWheel numberWheel=new NumberWheel(context, getResources().getString(R.string.dialog_title_set_speed), datas,(open_speed-1000)/50, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which==-2)
                        {
                            Log.d(tag,"item:"+((NumberWheel)dialog).getSelectedItem());
                            open_speed=Integer.parseInt(((NumberWheel)dialog).getSelectedItem());
                            sendAtCmd("ATSETRPM"+open_speed);
                        }



                    }
                });
                numberWheel.show();
            }
        });
        im_btn_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });








        reFreshScreen();
        img_view_background.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                Log.d(tag,"setOnSystemUiVisibilityChangeListener");
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        });






    }
    private void reFreshScreen()
    {
        img_view_background.post(new Runnable() {
            @Override
            public void run() {
                layoutAll();
                isLayoutCompleted=true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(tag,"onActivityResult");
    }








    @Override
    protected void onResume() {
        super.onResume();
        Log.d(tag,"onResume");
       getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        layoutAll();
       // sendAtCmd("ATRON");
    }





    private void layoutAll() {


                int back_bitmap_height;
                int back_bitmap_width;
                if(img_view_background.getHeight()==0) return;

                Log.d(tag,"runAble_image_view_back height:"+img_view_background.getHeight()+"---width:"+img_view_background.getWidth());

                //img_view_background.setImageDrawable(im_btn_auto.getDrawable());
                 Log.d(tag,"getscalex:"+img_view_background.getScaleX()+"---getscaley:"+img_view_background.getScaleY());






                Bitmap bitmap=BitmapFactory.decodeResource(getResources(),R.mipmap.background);

                Log.d(tag,"bitmap height:"+bitmap.getHeight()+"<--->"+"bitmap width:"+bitmap.getWidth());
                back_bitmap_height=bitmap.getHeight();
                back_bitmap_width=bitmap.getWidth();
               // img_view_background.getLayoutParams().width=bitmap.getWidth();
                //img_view_background.setLayoutParams(img_view_background.getLayoutParams());




                float scalex=back_bitmap_width/(float)img_view_background.getWidth();
                float scaley=back_bitmap_height/(float)img_view_background.getHeight();
                Log.d(tag,"scalex:"+scalex+"---scaley:"+scaley);
                int height;
                int width;
                float rateW=1920/(float)img_view_background.getWidth();
                float rateH=1080/(float)img_view_background.getHeight();



                ViewGroup.LayoutParams layoutParams;


                //计算高度和宽度
                bitmap=BitmapFactory.decodeResource(getResources(),R.mipmap.calibration);
                height=(int)(bitmap.getHeight()/scaley);
                width=(int)(bitmap.getWidth()/scalex);
                layoutParams=img_view_calibration.getLayoutParams();
                layoutParams.height=height;
                layoutParams.width=width;
                //((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)((736/1920f)*img_view_background.getWidth());
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(185/rateH);
               ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(736/rateH);
                img_view_calibration.setLayoutParams(layoutParams);

                bitmap=BitmapFactory.decodeResource(getResources(),R.mipmap.dash_left);
                height=(int)(bitmap.getHeight()/scaley);
                width=(int)(bitmap.getWidth()/scalex);
                layoutParams=progressBar_water_temp.getLayoutParams();
                layoutParams.height=height;
                layoutParams.width=width;
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(736/rateH);
                progressBar_water_temp.setLayoutParams(layoutParams);

                layoutParams=progressBar_oil_temp.getLayoutParams();
                layoutParams.height=height;
                layoutParams.width=width;
                ((RelativeLayout.LayoutParams)layoutParams).rightMargin=(int)(736/rateH);
                progressBar_oil_temp.setLayoutParams(layoutParams);









                //仪表盘转速和速度表的圆心分别在(419,474),(1498,474)位置，直径310

                //计算高度和宽度
                bitmap=BitmapFactory.decodeResource(getResources(),R.mipmap.zhizhen);
                height=(int)(bitmap.getHeight()/scaley);
                width=(int)(bitmap.getWidth()/scalex);
                Log.d(tag,height+"+"+width);
                layoutParams=img_view_zhizhen_car_speed.getLayoutParams();
                layoutParams.height=height;
                layoutParams.width=width;
                //计算坐标
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(1472/rateW);
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(250/rateH);
                img_view_zhizhen_car_speed.setLayoutParams(layoutParams);

                img_view_zhizhen_car_speed.setPivotX(26/rateW);
                img_view_zhizhen_car_speed.setPivotY(224/rateH);

                img_view_zhizhen_car_speed.setRotation(-150);



                //计算高度和宽度
                layoutParams=img_view_zhizhen_motor_speed.getLayoutParams();
                layoutParams.height=height;
                layoutParams.width=width;
                //计算坐标
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(393/rateW);
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(250/rateH);
                img_view_zhizhen_motor_speed.setLayoutParams(layoutParams);

                img_view_zhizhen_motor_speed.setPivotX(26/rateW);
                img_view_zhizhen_motor_speed.setPivotY(224/rateH);
                img_view_zhizhen_motor_speed.setRotation(-180);



                //下面是imageButton的布局,分两组，
                bitmap=BitmapFactory.decodeResource(getResources(),R.mipmap.auto);
                layoutParams=im_btn_on.getLayoutParams();
                height=(int)(bitmap.getWidth()/scalex);
                width=(int)(bitmap.getHeight()/scaley);
                layoutParams.width=width;
                layoutParams.height=height;
                //计算坐标
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)((862/rateW));
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(885/rateH);
                im_btn_on.setLayoutParams(layoutParams);



                layoutParams=im_btn_auto.getLayoutParams();
                layoutParams.width=width;
                layoutParams.height=height;
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(1044/rateW);
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(885/rateH);
                im_btn_auto.setLayoutParams(layoutParams);



                layoutParams=im_btn_off.getLayoutParams();
                layoutParams.width=width;
                layoutParams.height=height;
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(680/rateW);
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(885/rateH);
                im_btn_off.setLayoutParams(layoutParams);





                bitmap=BitmapFactory.decodeResource(getResources(),R.mipmap.after);
                layoutParams=im_btn_delay.getLayoutParams();
                height=(int)(bitmap.getWidth()/scalex);
                width=(int)(bitmap.getHeight()/scaley);
                layoutParams.width=width;
                layoutParams.height=height;
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(15/rateW);
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(830/rateH);
                im_btn_delay.setLayoutParams(layoutParams);
                layoutParams=im_btn_speed.getLayoutParams();
                layoutParams.width=width;
                layoutParams.height=height;
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(1650/rateW);
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(830/rateH);
                im_btn_speed.setLayoutParams(layoutParams);


                layoutParams=textView_motor_speed.getLayoutParams();
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(419/rateW-layoutParams.width/2);
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(474/rateH-layoutParams.height/2);
                textView_motor_speed.setLayoutParams(layoutParams);

                layoutParams=textView_car_speed.getLayoutParams();
                ((RelativeLayout.LayoutParams)layoutParams).leftMargin=(int)(1498/rateW-layoutParams.width/2);
                ((RelativeLayout.LayoutParams)layoutParams).topMargin=(int)(474/rateH-layoutParams.height/2);
                textView_car_speed.setLayoutParams(layoutParams);

















    }




}
