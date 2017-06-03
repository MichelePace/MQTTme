package org.pace.michele.mqttme;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.Serializable;

/**
 * Created by michele on 24/05/17.
 */

public class MyMessage {
    private MqttMessage message;
    private String topic;

    MyMessage(String t, MqttMessage m){
        topic = t;
        message = m;
    }

    String getTopic(){
        return topic;
    }

    MqttMessage getMessage(){
        return message;
    }
}
