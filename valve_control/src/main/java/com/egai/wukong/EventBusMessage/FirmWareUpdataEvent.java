package com.egai.wukong.EventBusMessage;

public class FirmWareUpdataEvent {

    public final static int cmd_process_change=0;
    public final static int cmd_updata_successful=1;
    public final static int cmd_updata_error=2;
    public final static int cmd_download_successful=3;

    private Object data;
    private int cmd_type;

    public FirmWareUpdataEvent(int cmd_type,Object data) {
        this.data = data;
        this.cmd_type=cmd_type;
        if (data==null)
        {
            this.data="";
        }
    }

    public int getCmd_type() {
        return cmd_type;
    }

    public Object getData() {
        return data;
    }
}
