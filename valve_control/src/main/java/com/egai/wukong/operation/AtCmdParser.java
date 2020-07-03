package com.egai.wukong.operation;

import android.util.Log;

import com.egai.wukong.EventBusMessage.DataStream;
import com.egai.wukong.EventBusMessage.DeviceControlEvent;
import com.egai.wukong.EventBusMessage.SingleValue;


public class AtCmdParser {



    public static final String ATON="ATON";
    public static final String ATOFF="ATOFF";
    public static final String ATAUTO="ATAUTO";
    public static final String ATGETMODE="ATGETMODE";
    public static final String ATSETRPM="ATSETRPM";
    public static final String ATGETRPM="ATGETRPM";
    private static final String Tag="Parser";





    public static Object parse(String cmd)
    {
        String[] values=cmd.split(",|=");
        Log.d(Tag,"cmd:"+cmd);

        for (String s:values
             ) {
            Log.d("parse",s);

        }


        if(values.length>0)
        {
            switch (values[0])
            {
                case "$OBD-RT1":
                {
                int motoer_speed = 0;
                int car_speed = 0;
                try {
                    motoer_speed = Integer.parseInt(values[1]);
                    car_speed = Integer.parseInt(values[2]);
                } catch (Exception e) {

                }


                return new DeviceControlEvent(DeviceControlEvent.CMD_DATA_STREAM, new DataStream(car_speed, motoer_speed, -1, -1));
               }
                case "$OBD-RT2":
                {
                int warter_tmp=0;
                int IAT=0;
                try {
                    warter_tmp=Integer.parseInt(values[1]);
                    IAT=Integer.parseInt(values[2]);
                    if(warter_tmp<0)
                    {
                        warter_tmp=40+warter_tmp;
                    }
                    else
                    {
                        warter_tmp+=40;
                    }
                    if(IAT<0)
                    {
                        IAT=40+warter_tmp;
                    }
                    else
                    {
                        IAT+=40;
                    }

                }
                catch (Exception e)
                {

                }
                Log.d("parse",""+IAT+"=="+warter_tmp);
                return new DeviceControlEvent(DeviceControlEvent.CMD_DATA_STREAM, new DataStream(-1,-1,IAT,warter_tmp));
                }
                case "$STATA":
                    return new DeviceControlEvent(DeviceControlEvent.CMD_SINGLE_VALUE, new SingleValue(SingleValue.STATA,Integer.parseInt(values[1])*10+Integer.parseInt(values[2])));

                case "$RPM":
                    return new DeviceControlEvent(DeviceControlEvent.CMD_SINGLE_VALUE, new SingleValue(SingleValue.RPM,Integer.parseInt(values[1])));

                case "$Delay":


                    return new DeviceControlEvent(DeviceControlEvent.CMD_SINGLE_VALUE, new SingleValue(SingleValue.DELAY,Integer.parseInt(values[1])));
                case "$VER":
                    //示例::$VER=BleCtrlCentr_WireUse_Nov 12 2018_v1.0-beta

                    return new DeviceControlEvent(DeviceControlEvent.CMD_SINGLE_VALUE,new SingleValue(SingleValue.VER,values[1]));





            }




        }



        return null;
    }
}
