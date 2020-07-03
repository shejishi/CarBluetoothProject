package com.egai.wukong.EventBusMessage;

public class DeviceControlEvent {
    private int CMD;
    private Object data;

    public  static final int  CMD_DATA_STREAM= 0;
    public static final  int CMD_CONNECT_LOSE=1;
    public static final int CMD_SINGLE_VALUE=2;

    public DeviceControlEvent(int CMD, Object data) {
        this.CMD = CMD;
        this.data = data;
    }

    public int getCMD() {
        return CMD;
    }

    public Object getData() {
        return data;
    }
}
