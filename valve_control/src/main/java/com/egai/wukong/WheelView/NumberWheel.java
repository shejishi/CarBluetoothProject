package com.egai.wukong.WheelView;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import com.egai.ble.valveControl.R;

import java.util.List;

public class NumberWheel extends AlertDialog {
    private final String tag="number view";


    private WheelView wv;
    public NumberWheel(@NonNull Context context,String title, List<String> items,int index,OnClickListener onClickListener) {
        super(context);
        View outerView = LayoutInflater.from(context).inflate(R.layout.wheel_view, null);
        wv= (WheelView) outerView.findViewById(R.id.wheel_view_wv);
        wv.setOffset(2);
        wv.setItems(items);
        wv.setSeletion(index);
        wv.setOnWheelViewListener(null);
        this.setView(outerView);
        this.setTitle(title);

        this.setButton(-2,context.getResources().getString(R.string.OK),onClickListener);
        this.setButton(-1,"取消", (OnClickListener) null);




    }
    public String getSelectedItem()
    {
        Log.d(tag,"index:"+wv.getSeletedIndex());
        return wv.getSeletedItem();
    }

    /**
     * Called when the dialog is starting.
     */
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(tag,"onStart");
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);



    }





}
