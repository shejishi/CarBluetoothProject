package com.egai.wukong.EventBusMessage;

public class BleEvent {

    private Object data;
    private int cmd;
    public final static int CMD_START_SCAN=0;
    public final static int CMD_STOP_SCAN=1;
    public final static int CMD_START_CONNECT=2;
    public final static int CMD_STOP_CONNECT=3;
    public final static int CMD_SEND_AT_CMD=4;
    public final static int BLE_EVENT_CONNECTED=5;
    public final static int BLE_EVENT_CONNECTION_FAIL=6;
    public final static int BLE_EVENT_CONNECTTION_LOSE=7;


    public BleEvent(int cmd, Object data)
    {
        this.cmd=cmd;
        this.data=data;
    }

    public int getCmd() {

        return cmd;
    }

    public Object getData() {
        return data;
    }


}
