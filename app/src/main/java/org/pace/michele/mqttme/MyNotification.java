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

    MyNotification(boolean n, int t){
        notify = n;
        type = t;
    }

    public boolean getNotify(){
        return  notify;
    }

    public int getType(){
        return type;
    }

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
}
