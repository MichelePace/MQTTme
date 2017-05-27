package org.pace.michele.mqttme;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.pace.michele.mqttme.MyItem;
import org.eclipse.paho.client.mqttv3.*;

public class MainActivity extends AppCompatActivity {

    File file;
    File fileSettings;
    final String path = "/itemStatus";
    final String pathSettings = "/settingStatus";


    Hashtable<Integer, MyItem> items = new Hashtable<Integer, MyItem>();
    private Hashtable<Integer, View> itemsView = new Hashtable<Integer, View>();
    Hashtable<String, Integer> topics = new Hashtable<String, Integer>();

    EditText messageToSend;

    boolean initialized = false;

    private int totalItems = 0;

    //Server settings
    Connection settings;

    //Intent contants
    static final int NEW_ITEM = 0;
    static final int MODIFY_ITEM = 1;
    static final int SERVER_SETTINGS = 2;

    boolean brokerSetted = false;

    static boolean main_activity_running = false;

    ServiceConnection mConnection;
    PushNotificationService mService;
    boolean mBound = false;

    //Notifications
    private int notificationID = 0;
    long[] vibration = {200, 350, 100, 350};
    Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        main_activity_running = true;

        startAndBoundService();

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                final Dialog d = new Dialog(MainActivity.this);
                d.setContentView(R.layout.add_item);
                d.setCancelable(true);

                TextView text = (TextView) d.findViewById(R.id.textItem);
                TextView range = (TextView) d.findViewById(R.id.rangeItem);
                TextView toggle = (TextView) d.findViewById(R.id.toggleItem);

                text.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        addItem(MyItem.TEXT_ITEM);
                        d.dismiss();
                    }
                });

                range.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        addItem(MyItem.RANGE_ITEM);
                        d.dismiss();
                    }
                });

                toggle.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        addItem(MyItem.TOGGLE_ITEM);
                        d.dismiss();
                    }
                });
                d.show();
            }
        });

        settings = new Connection();

        // Initialize layout
        LinearLayout column = (LinearLayout) findViewById(R.id.left_column);

        column.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(!initialized) {
                    initialize();

                    if(mBound){

                    }

                }
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        main_activity_running = true;
    }


    /*@Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Snackbar.make(findViewById(android.R.id.content), "You have to allow to use external storage to use this app!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    this.finish();
                }
                return;
            }

            case 2: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    Snackbar.make(findViewById(android.R.id.content), "You have to allow to use external storage to use this app!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    this.finish();
                }
                return;
            }
        }
    }*/


    /**
     *
     */
    void startAndBoundService(){
        if(isMyServiceRunning(PushNotificationService.class)){
            System.out.println("++++++Service running");
        }else{
            System.out.println("++++++Service not running");
            Intent myIntent = new Intent(MainActivity.this, PushNotificationService.class);
            MainActivity.this.startService(myIntent);
        }

        /** Defines callbacks for service binding, passed to bindService() */
        mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className, IBinder binder) {
                // We've bound to PushNotificationService, cast the IBinder and get PushNotificationService instance
                PushNotificationService.LocalBinder localBinder = (PushNotificationService.LocalBinder) binder;
                mService = localBinder.getService();
                mBound = true;

                mService.setMainActivity(MainActivity.this);

                Vector<MyMessage> messages = mService.getMessages();
                MyMessage mMessage;
                for(int i = 0; i < messages.size(); i++){
                    mMessage = messages.get(i);
                    messageReceived(mMessage.getTopic(), mMessage.getMessage());
                }
                System.out.println(" +++ Bound to service");
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
                System.out.println(" +++ Not bound to service");
            }
        };

        // Bind to PushNotificationService
        Intent intent = new Intent(this, PushNotificationService.class);
        bindService(intent, mConnection, Context.BIND_ABOVE_CLIENT);
    }


    void clientConnection(boolean connected){
        if(connected){
            LinearLayout host = (LinearLayout)findViewById(R.id.host);
            Snackbar.make(host, "Connected", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }else{
            LinearLayout host = (LinearLayout)findViewById(R.id.host);
            Snackbar.make(host, "Not connected", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case (NEW_ITEM): {
                if (resultCode == ItemParametersActivity.RESULT_OK) {
                    MyItem newItem = (MyItem) data.getSerializableExtra("Item");
                    createNewItem(newItem);
                } else if (resultCode == ItemParametersActivity.RESULT_BACK) {
                    System.out.println("User pressed back button");
                } else {
                    Snackbar.make(new View(this), "Something goes wrong...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                break;
            }

            case (MODIFY_ITEM): {
                if (resultCode == ItemParametersActivity.RESULT_OK) {

                    MyItem item = (MyItem) data.getSerializableExtra("Item");
                    int key = data.getIntExtra("Key", -1);

                    ((TextView)itemsView.get(key).findViewById(R.id.name)).setText(item.getName());//Modify the item name in the view
                    items.put(key, item);//Replace the modified item

                    // Append subscription topic
                    if(topics.containsKey(item.getSubTopic())){
                        int n = topics.get(item.getSubTopic());
                        n++;
                        topics.put(item.getSubTopic(), n);
                    }else{
                        topics.put(item.getSubTopic(), 1);
                        if(mBound) {
                            mService.subscribe(item.getSubTopic(), item.getQoS());
                        }
                    }

                    if(item.getType() == MyItem.RANGE_ITEM){
                        ((SeekBar)itemsView.get(key).findViewById(R.id.seekBar)).setMax(item.getMax() - item.getMin());
                    }

                } else if (resultCode == ItemParametersActivity.RESULT_BACK) {
                    System.out.println("User pressed back button");
                } else {
                    Snackbar.make(new View(this), "Something goes wrong...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                break;
            }

            case (SERVER_SETTINGS):
                if (resultCode == ItemParametersActivity.RESULT_OK) {
                    settings = (Connection) data.getSerializableExtra("Connection");

                    if(mBound){
                        mService.setConnection(settings);
                        mService.initializeMQTT();
                        mService.mqtt_connect();
                        brokerSetted = true;
                    }

                    //Save settings on file
                    fileSettings = new File(this.getFilesDir() + pathSettings);
                    try {
                        FileOutputStream output= new FileOutputStream(fileSettings);
                        ObjectOutputStream out = new ObjectOutputStream(output);
                        out.writeObject(settings);
                        out.flush();
                        out.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                } else if (resultCode == ItemParametersActivity.RESULT_BACK) {
                    System.out.println("User pressed back button");
                } else {
                    Snackbar.make(new View(this), "Something goes wrong...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                break;
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
                    for(int i=0; i < items.size(); i++)
                    {
                        createNewItem(items.get(i));
                    }
                initialized = true;
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


    //Toggle events

    /**
     *
     * @param key
     */
    void togglePressed(int key, boolean isChecked){

        MyItem mi = items.get(key);
        String topic = mi.getPubTopic();
        boolean retained = mi.getRetained();
        int qos = mi.getQoS();

        String message;
        if(isChecked) message = mi.getPressed();
        else message = mi.getUnpressed();

        byte[] payload = message.getBytes();

        if(mBound){
            mService.publish(topic, payload, qos, retained);
        }else{
            Toast.makeText(getApplicationContext(), "Host not connected!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *
     * @param key
     */
    void longClickToggle(final int key){
        final Dialog d = new Dialog(MainActivity.this);
        d.setContentView(R.layout.long_press_dialog);
        d.setCancelable(true);

        TextView text = (TextView) d.findViewById(R.id.modify);
        TextView range = (TextView) d.findViewById(R.id.delete);
        TextView toggle = (TextView) d.findViewById(R.id.duplicate);

        text.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                modifyItem(key);
                d.dismiss();
            }
        });

        range.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                deleteItem(key);
                d.dismiss();
            }
        });

        toggle.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                duplicateItem(key);
                d.dismiss();
            }
        });
        d.show();
    }


    //SeekBar events

    /**
     *
     * @param key
     * @param progress
     */
    void seekBarProgressChanged(int key, int progress){
        if(items.size() > key) {
            View item = itemsView.get(key);
            MyItem mi = items.get(key);
            TextView tv = (TextView) item.findViewById(R.id.progress);
            int value = progress + mi.getMin();
            tv.setText(""+value);
        }
    }


    /**
     *
     * @param key
     */
    void seekBarStartTracking(int key){}


    /**
     *
     * @param key
     */
    void seekBarStopTracking(int key){}


    /**
     *
     * @param key
     */

    void buttonSetClicked(int key){
        MyItem mi = items.get(key);
        String topic = mi.getPubTopic();
        boolean retained = mi.getRetained();
        int qos = mi.getQoS();

        View item = itemsView.get(key);
        TextView t = (TextView) item.findViewById(R.id.progress);
        String message = t.getText().toString();

        byte[] payload = message.getBytes();

        if(mBound){
            mService.publish(topic, payload, qos, retained);
        }else{
            Toast.makeText(getApplicationContext(), "Host not connected!", Toast.LENGTH_SHORT).show();
        }

    }


    /**
     *
     * @param key
     */
    void longClickRange(final int key){
        final Dialog d = new Dialog(MainActivity.this);
        d.setContentView(R.layout.long_press_dialog);
        d.setCancelable(true);

        TextView text = (TextView) d.findViewById(R.id.modify);
        TextView range = (TextView) d.findViewById(R.id.delete);
        TextView toggle = (TextView) d.findViewById(R.id.duplicate);

        text.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                modifyItem(key);
                d.dismiss();
            }
        });

        range.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                deleteItem(key);
                d.dismiss();
            }
        });

        toggle.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                duplicateItem(key);
                d.dismiss();
            }
        });
        d.show();
    }


    //TextView events

    /**
     *
     * @param key
     */
    void messageClicked(int key){
        final Dialog d = new Dialog(this);
        d.setContentView(R.layout.message_dialog);
        d.setCancelable(true);

        d.show();

        final int p = key;

        messageToSend = (EditText) d.findViewById(R.id.message);

        Button send = (Button) d.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                // Dismiss keyboard
                EditText e = (EditText) d.findViewById(R.id.message);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(e.getWindowToken(), 0);

                sendText(p);
                d.dismiss();
            }
        });
        d.show();

        // Show keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }


    /**
     *
     * @param key
     */
    void sendText(int key){
        MyItem mi = items.get(key);
        String topic = mi.getPubTopic();
        boolean retained = mi.getRetained();
        int qos = mi.getQoS();

        View item = itemsView.get(key);
        TextView t = (TextView) item.findViewById(R.id.message);
        String message = String.valueOf(messageToSend.getText());
        t.setText(message);

        byte[] payload = message.getBytes();

        if(mBound){
            mService.publish(topic, payload, qos, retained);
        }else{
            Toast.makeText(getApplicationContext(), "Host not connected!", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     *
     * @param key
     */
    void longClickText(final int key){
        final Dialog d = new Dialog(MainActivity.this);
        d.setContentView(R.layout.long_press_dialog);
        d.setCancelable(true);

        TextView text = (TextView) d.findViewById(R.id.modify);
        TextView range = (TextView) d.findViewById(R.id.delete);
        TextView toggle = (TextView) d.findViewById(R.id.duplicate);

        text.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                modifyItem(key);
                d.dismiss();
            }
        });

        range.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                deleteItem(key);
                d.dismiss();
            }
        });

        toggle.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                duplicateItem(key);
                d.dismiss();
            }
        });
        d.show();
    }


    void messageReceived(String topic, MqttMessage message){
        Enumeration<Integer> keys = items.keys();
        MyItem mi;
        while (keys.hasMoreElements()){
            int key = keys.nextElement();
            mi = items.get(key);

            // If item subscribed to topic
            if(mi.getSubTopic().equals(topic)) {
                switch (mi.getType()) {
                    case MyItem.TEXT_ITEM:
                        String mess = mi.getPrefix() + message + mi.getPostfix();
                        ((TextView)itemsView.get(key).findViewById(R.id.message)).setText(mess);
                        break;

                    case MyItem.RANGE_ITEM:
                        ((TextView)itemsView.get(key).findViewById(R.id.progress)).setText(message.toString());
                        break;

                    case MyItem.TOGGLE_ITEM:
                        String press = mi.getPressed();
                        String unpress = mi.getUnpressed();
                        if(message.toString().equals(press)) {
                            ((ToggleButton) itemsView.get(key).findViewById(R.id.toggleButton)).setChecked(true);
                        }else if(message.toString().equals(unpress)){
                            ((ToggleButton) itemsView.get(key).findViewById(R.id.toggleButton)).setChecked(false);
                        }
                        break;
                }
            }
        }

    }


    // New item

    /**
     *
     * @param item
     */
    void addItem(int item){
        Intent myIntent = new Intent(MainActivity.this, ItemParametersActivity.class);
        myIntent.putExtra("Reason", NEW_ITEM);
        myIntent.putExtra("ItemID", item); //Optional parameters, sends thi item type
        MainActivity.this.startActivityForResult(myIntent, NEW_ITEM);
    }


    /**
     *
     * @param mi
     */
    void createNewItem(MyItem mi){

        LinearLayout leftColumn = (LinearLayout) findViewById(R.id.left_column);
        LinearLayout rightColumn = (LinearLayout) findViewById(R.id.right_column);

        int itemDimension = leftColumn.getWidth();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(itemDimension, itemDimension);
        params.setMargins(0,0,0,5);

        LayoutInflater inflater_one_user = MainActivity.this.getLayoutInflater();
        View item = null;


        int type = mi.getType();

        final int key = totalItems;

        //IF TOGGLE_ITEM
        if(type == MyItem.TOGGLE_ITEM) {
            item = inflater_one_user.inflate(R.layout.toggle_item, null);
            TextView itemName = (TextView) item.findViewById(R.id.name);

            itemName.setText(mi.getName());
            item.setLayoutParams(params);

            setListeners(item,key,mi);

        }else if(type == MyItem.RANGE_ITEM){

            //IF RANGE_ITEM
            item = inflater_one_user.inflate(R.layout.range_item, null);
            TextView itemName = (TextView) item.findViewById(R.id.name);

            itemName.setText(mi.getName());
            item.setLayoutParams(params);

            setListeners(item,key,mi);

        }else if(type == MyItem.TEXT_ITEM){

            //IF TEXT_ITEM
            item = inflater_one_user.inflate(R.layout.text_item, null);
            TextView itemName = (TextView) item.findViewById(R.id.name);
            TextView message = (TextView) item.findViewById(R.id.message);

            itemName.setText(mi.getName());
            item.setLayoutParams(params);
            setListeners(item,key,mi);
        }

        if(item != null) {
            if (totalItems % 2 == 0) {
                leftColumn.addView(item);
            } else {
                rightColumn.addView(item);
            }

            itemsView.put(key, item);
            items.put(key, mi);
        }

        // Append subscription topic
        if(topics.containsKey(mi.getSubTopic())){
            int n = topics.get(mi.getSubTopic());
            n++;
            topics.put(mi.getSubTopic(), n);
        }else{
            topics.put(mi.getSubTopic(), 1);
            if(mBound) {
                mService.subscribe(mi.getSubTopic(), mi.getQoS());
            }
        }

        totalItems++;
    }


    /**
     *
     * @param item
     * @param key
     * @param mi
     */
    void setListeners(View item, int key, MyItem mi) {
        final int listenKey=key;
        switch (mi.getType())
        {
            case MyItem.TEXT_ITEM:
                TextView message = (TextView) item.findViewById(R.id.message);

                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        messageClicked(listenKey);
                    }
                });

                message.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        messageClicked(listenKey);
                    }
                });

                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        longClickText(listenKey);
                        return false;
                    }
                });
                break;
            case MyItem.RANGE_ITEM:
                SeekBar sb = ((SeekBar) item.findViewById(R.id.seekBar));
                sb.setMax(mi.getMax() - mi.getMin());
                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seekBarProgressChanged(listenKey, progress);
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        seekBarStartTracking(listenKey);
                    }

                    public void onStopTrackingTouch(SeekBar seekBar) {
                        seekBarStopTracking(listenKey);
                    }
                });

                item.findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        buttonSetClicked(listenKey);
                    }
                });

                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        longClickRange(listenKey);
                        return false;
                    }
                });
                break;
            case MyItem.TOGGLE_ITEM:
                ToggleButton tb = (ToggleButton) item.findViewById(R.id.toggleButton);
                tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        togglePressed(listenKey, isChecked);
                    }
                });

                item.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        longClickToggle(listenKey);
                        return false;
                    }
                });
                break;
        }
    }


    /**
     *
     * @param key
     */
    void modifyItem(int key){
        MyItem item = items.get(key);

        // Delete subscription topic
        int n = topics.get(item.getSubTopic());

        if(n == 1){
            topics.remove(item.getSubTopic());
            if(mBound) {
                mService.unsubscribe(item.getSubTopic());
            }
        }else{
            n--;
            topics.put(item.getSubTopic(), n);
        }

        Intent myIntent = new Intent(MainActivity.this, ItemParametersActivity.class);

        //Optional parameters
        myIntent.putExtra("Reason", MODIFY_ITEM);
        myIntent.putExtra("Item", item);
        myIntent.putExtra("Key", key);
        myIntent.putExtra("ItemID", item.getType());
        MainActivity.this.startActivityForResult(myIntent, MODIFY_ITEM);
    }


    /**
     *
     * @param key
     */
    void deleteItem(int key){
        LinearLayout leftColumn = (LinearLayout) findViewById(R.id.left_column);
        LinearLayout rightColumn = (LinearLayout) findViewById(R.id.right_column);
        if (key % 2 != 0) {
            rightColumn.removeView(itemsView.get(key));
        } else {
            leftColumn.removeView(itemsView.get(key));
        }

        MyItem mi = items.get(key);

        // Delete subscription topic
        int n = topics.get(mi.getSubTopic());

        if(n == 1){
            topics.remove(mi.getSubTopic());
            if(mBound) {
                mService.unsubscribe(mi.getSubTopic());
            }
        }else{
            n--;
            topics.put(mi.getSubTopic(), n);
        }

        items.remove(key);
        itemsView.remove(key);
        mi = null;
        View item=null;
        if(key<totalItems)
        {
            for(int i=key+1;i<totalItems;i++)
            {
                mi=items.get(i);
                item=itemsView.get(i);
                items.remove(i);
                itemsView.remove(i);
                setListeners(item,i-1,mi);
                items.put(i-1,mi);
                itemsView.put(i-1,item);
                if ((i-1) % 2 == 0) {
                    rightColumn.removeView(item);
                    leftColumn.addView(item);
                } else {
                    leftColumn.removeView(item);
                    rightColumn.addView(item);
                }
            }
        }

        totalItems--;
    }


    /**
     *
     * @param key
     */
    void duplicateItem(int key){
        MyItem mi=items.get(key);
        createNewItem(mi);
    }


    /**
     *
     * @param serviceClass
     * @return
     */
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onStop(){
        super.onStop();
        file = new File(this.getFilesDir() + path);
        fileSettings = new File(this.getFilesDir() + pathSettings);
        try {
            FileOutputStream output= new FileOutputStream(file);
            ObjectOutputStream out = new ObjectOutputStream(output);
            out.writeObject(items);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        main_activity_running = false;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent myIntent = new Intent(MainActivity.this, SettingConnectionActivity.class);
            myIntent.putExtra("Connection", settings);
            MainActivity.this.startActivityForResult(myIntent, SERVER_SETTINGS);
            return true;

        }else if (id == R.id.action_reconnect) {

            if(brokerSetted) {
                if(mBound) {
                    mService.mqtt_connect();
                }else{
                    startAndBoundService();
                }
            }else{
                LinearLayout host = (LinearLayout)findViewById(R.id.host);
                Snackbar.make(host, "No broker parameters found", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
            return true;
        }



        return super.onOptionsItemSelected(item);
    }
}
