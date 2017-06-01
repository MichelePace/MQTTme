package org.pace.michele.mqttme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class PushNotificationService extends Service {

    //MQTT
    private MqttAndroidClient client;
    private MqttConnectOptions option;
    private Connection settings;
    private boolean brokerSetted = false;

    //File settings
    File file;
    private File fileSettings;
    final String path = "/itemStatus";
    private final String pathSettings = "/settingStatus";

    Hashtable<Integer, MyItem> items = new Hashtable<Integer, MyItem>();
    Hashtable<String, Notification> notifications = new Hashtable<String, Notification>();

    //Notifications
    private int notificationID = 0;
    private long[] vibration = {200, 350, 100, 350};
    private Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    private NotificationManager notificationManager;
    private int notif_number = 0;
    StringBuffer notification_message = new StringBuffer();

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    //MainActivity istance to call methods
    private MainActivity mainActivity;

    //List of messages arrived while in background
    private Vector<MyMessage> messages;


    Timer timerObj;
    TimerTask timerTaskObj;

    /**
     *
     */
    public PushNotificationService() {
        messages = new Vector<MyMessage>();
    }


    public class LocalBinder extends Binder {
        public PushNotificationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PushNotificationService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        System.out.println(" ++++++++ onBind() called");
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        super.onUnbind(intent);
        System.out.println(" ++++++++ onUnbind() called");
        return true;
    }


    /**
     *
     * @return
     */
    public Vector<MyMessage> getMessages(){
        Vector<MyMessage> temp = messages;
        messages = new Vector<MyMessage>();
        if(notificationManager != null) {
            notificationManager.cancelAll();
        }
        notif_number = 0;
        notification_message = new StringBuffer();
        return temp;
    }


    /**
     *
     * @param ma
     */
    public void setMainActivity(MainActivity ma){
        mainActivity = ma;
    }


    /**
     *
     * @param s
     */
    public void setConnection(Connection s){
        settings = s;
    }


    /**
     *
     * @param notif
     */
    public void setNotifications(Hashtable<String, Notification> notif){
        notifications = notif;
    }

    /**
     *
     * @param topic
     * @param payload
     * @param QoS
     * @param retained
     */
    public void publish(String topic, byte[] payload, int QoS, boolean retained){
        try {
            client.publish(topic, payload, QoS, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void subscribe(String topic, int QoS){

        if(settings.connected) {
            try {
                client.subscribe(topic, QoS);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }


    public void unsubscribe(String topic){
        if(settings.connected) {
            try {
                client.unsubscribe(topic);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println(" +++ Service started");

        settings = new Connection();

        initialize();

        if(brokerSetted) {
            initializeMQTT();
            mqtt_connect();
        }
    }


    /**
     *
     */
    void initialize(){

        file = new File(this.getFilesDir() + path);
        fileSettings = new File(this.getFilesDir() + pathSettings);
        try {

            if(file.exists() && file.canRead()) {

                file = new File(this.getFilesDir() + path);

                FileInputStream input = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(input);
                Object obj=in.readObject();
                items = (Hashtable<Integer, MyItem>) obj;
                in.close();
            }

            if(fileSettings.exists() && fileSettings.canRead()) {

                FileInputStream input = new FileInputStream(fileSettings);
                ObjectInputStream in = new ObjectInputStream(input);
                Object obj=in.readObject();
                settings=(Connection) obj;
                in.close();

                brokerSetted = true;
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }


    /**
     *
     */
    void initializeMQTT(){

        MemoryPersistence memPer = new MemoryPersistence();
        client = new MqttAndroidClient(this.getApplicationContext(), settings.getBROKER_URL(), settings.getClientId(), memPer);
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("Connection lost");
                if(cause != null) {
                    cause.printStackTrace();
                }
                mqtt_connect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                System.out.println(" +++ Topic: "+topic+", Message: "+message.toString());
                messageReceived(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        option = new MqttConnectOptions();
        option.setCleanSession(false);
        option.setUserName(settings.getUsername());
        option.setPassword(settings.getPassword().toCharArray());
        option.setAutomaticReconnect(true);
        option.setConnectionTimeout(30);
    }


    /**
     *
     */
    void mqtt_connect(){

        System.out.println("--Trying to connect");

        try {
            client.connect(option, this.getApplicationContext(), new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    System.out.println(" +++ Connected");

                    //Subscribe to all topics
                    Enumeration<MyItem> e;
                    MyItem mi;

                    if(mainActivity != null) e = mainActivity.items.elements();
                    else e = items.elements();

                    while (e.hasMoreElements()) {

                        mi = e.nextElement();

                        try {
                            client.subscribe(mi.getSubTopic(), mi.getQoS());
                        } catch (MqttException e1) {
                            e1.printStackTrace();
                        }
                    }


                    settings.connected = true;

                    /*if(timerObj != null) {
                        timerObj.cancel();
                        timerObj.purge();
                    }*/

                    System.out.println(" +++ Subscribed");

                    if(MainActivity.main_activity_running && mainActivity != null){
                        mainActivity.clientConnection(true);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    settings.connected = false;

                    if(MainActivity.main_activity_running && mainActivity != null){
                        mainActivity.clientConnection(false);
                    }

                    System.out.println(" +++ Not connected ");

                    /*timerObj = new Timer();
                    timerTaskObj = new TimerTask() {
                        public void run() {
                            mqtt_connect();
                        }
                    };
                    timerObj.schedule(timerTaskObj, 0, 10000);*/

                    System.out.println(exception.getCause());
                    System.out.println(exception.getMessage());
                    exception.printStackTrace();
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     * @param topic
     * @param message
     */
    void messageReceived(String topic, MqttMessage message){

        String notification_title;
        notif_number++;

        if(notif_number == 1) {
            notification_title = "1 message";
        }else{
            notification_title = notif_number + " messages";
        }

        notification_message.append(topic + ": " + message.toString() + "\n");

        if(!MainActivity.main_activity_running) {

            // prepare intent which is triggered if the
            // notification is selected
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification.Builder(this)
                    .setContentTitle(notification_title)
                    .setContentText(notification_message)
                    .setSmallIcon(R.drawable.ic_ico_notify)
                    .setStyle(new Notification.BigTextStyle().bigText(notification_message))
                    .setContentIntent(pIntent)
                    .setAutoCancel(true)
                    .setVibrate(vibration)
                    .setSound(ringtone).build();

            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(notificationID, notification);

            MyMessage mMessage = new MyMessage(topic, message);
            messages.add(mMessage);

        }else{
            if(mainActivity != null){
                mainActivity.messageReceived(topic, message);
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        System.out.println(" +++ Service stopped");
    }
}
