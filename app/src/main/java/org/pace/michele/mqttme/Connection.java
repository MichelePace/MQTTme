package org.pace.michele.mqttme;

import java.io.Serializable;

/**
 * Created by gianvito on 13/05/17.
 */

public class Connection implements Serializable {

    public boolean connected;
    private String BROKER_ADDR;
    private int BROKER_port;
    private String MQTT_USER;
    private String MQTT_PASS;
    private String clientId;

    Connection(){}

    Connection(String name,String address,int port,String username,String password,String clientId,boolean connected)
    {
        this.BROKER_ADDR=address;
        this.BROKER_port=port;
        this.MQTT_USER=username;
        this.MQTT_PASS=password;
        this.clientId=clientId;
        this.connected=false;
    }

    void setAddress(String address){this.BROKER_ADDR=address;}

    void setport(int port){this.BROKER_port=port;}

    void setUsername(String username){this.MQTT_USER=username;}

    void setPassword(String password){this.MQTT_PASS=password;}

    void setClientId(String clientId){this.clientId=clientId;}

    String getAddress(){return this.BROKER_ADDR;}

    int getPort(){return this.BROKER_port;}

    String getUsername(){return this.MQTT_USER;}

    String getPassword(){return this.MQTT_PASS;}

    String getClientId(){return this.clientId;}

    String getBROKER_URL(){return "tcp://"+this.getAddress()+":"+this.getPort();}

}
