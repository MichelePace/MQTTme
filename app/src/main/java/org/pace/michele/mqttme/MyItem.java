package org.pace.michele.mqttme;

import android.view.View;

import java.io.Serializable;

/**
 * Created by michele on 02/05/17.
 */


public class MyItem implements Serializable{

    private static final long serialVersionUID = 1;

    public static final int RANGE_ITEM = 0;
    public static final int TOGGLE_ITEM = 1;
    public static final int TEXT_ITEM = 2;

    private String name;
    private int type;
    private String pubTopic;
    private String subTopic;
    private int QoS;
    private boolean retained;
    private boolean state;
    private int min;
    private int max;
    private String prefix;
    private String postfix;
    private String pressed;
    private String unpressed;


    MyItem (){}

    MyItem(String n, int t, String pub, String sub, int q, boolean r, String pre, String post){
        name = n;
        type = t;
        pubTopic = pub;
        subTopic = sub;
        QoS = q;
        retained = r;
        prefix = pre;
        postfix = post;
    }

    MyItem(String n, int t, String pub, String sub,int q, boolean r, boolean s, String press, String unpress){
        name = n;
        type = t;
        pubTopic = pub;
        subTopic = sub;
        QoS = q;
        retained = r;
        state = s;
        pressed = press;
        unpressed = unpress;
    }

    MyItem(String n, int t, String pub, String sub, int q, boolean r, int min, int max){
        name = n;
        type = t;
        pubTopic = pub;
        subTopic = sub;
        QoS = q;
        retained = r;
        this.min = min;
        this.max = max;
    }

    void setName(String n){
        name = n;
    }

    void setType(int t){
        type = t;
    }

    void setPubTopic(String pub){
        pubTopic = pub;
    }

    void setSubTopic(String sub){
        subTopic = sub;
    }

    void setQoS(int q){
        QoS = q;
    }

    void setRetained(boolean r){
        retained = r;
    }

    void setPrefix(String p){
        prefix = p;
    }

    void setPostfix(String p){
        postfix = p;
    }

    void setPressed(String p){
        pressed = p;
    }

    void setUnpressed(String u){
        unpressed = u;
    }

    void setState(boolean s){
        state = s;
    }

    void setMin(int min){
        this.min = min;
    }

    void setMax(int max){
        this.max = max;
    }

    String getName(){
        return name;
    }

    int getType(){
        return type;
    }

    String getPubTopic(){
        return pubTopic;
    }

    String getSubTopic(){
        return subTopic;
    }

    int getQoS(){
        return QoS;
    }

    boolean getRetained(){ return retained; }

    boolean setState(){ return state; }

    String getPrefix(){
        return prefix;
    }

    String getPostfix(){
        return postfix;
    }

    String getPressed(){
        return pressed;
    }

    String getUnpressed(){
        return unpressed;
    }

    int getMin(){
        return min;
    }

    int getMax(){
        return max;
    }

}
