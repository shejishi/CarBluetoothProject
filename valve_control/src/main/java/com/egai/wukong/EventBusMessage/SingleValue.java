package com.egai.wukong.EventBusMessage;

public class SingleValue {

    public static final int STATA=0;
    public static final int RPM=1;
    public static final int DELAY=2;
    public static final int VER=3;

    private int which;
    private Object data;

    public SingleValue(int which, Object data) {
        this.which = which;
        this.data = data;
    }

    public int getWhich() {
        return which;
    }

    public Object getData() {
        return data;
    }
}
