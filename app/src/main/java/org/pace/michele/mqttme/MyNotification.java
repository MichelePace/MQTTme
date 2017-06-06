package org.pace.michele.mqttme;

import java.io.Serializable;

/**
 * Created by michele on 31/05/17.
 */

public class MyNotification implements Serializable{
    static public int NOTIFICATION = 0;
    static public int ALARM = 1;

    private boolean notify;
    private int type;
    private boolean notShowSame;

    MyNotification(boolean n, int t, boolean nts){
        notify = n;
        type = t;
        notShowSame = nts;
    }

    public boolean getNotify(){ return  notify; }

    public int getType(){ return type; }

    public boolean getNotShowSame(){ return notShowSame; }

    public void setNotify(boolean n){
        notify = n;
    }

    public void setType(int t){
        if(t != NOTIFICATION && t != ALARM) {
            type = NOTIFICATION;
        }else{
            type = t;
        }
    }

    public void setNotShowSame(boolean nts){
        notShowSame = nts;
    }
}
