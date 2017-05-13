package org.pace.michele.mqttme;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.pace.michele.mqttme.MyItem;
import org.eclipse.paho.client.mqttv3.*;

import static android.R.attr.type;
import static org.pace.michele.mqttme.R.id.clientId;

public class MainActivity extends AppCompatActivity {

    File file;
    File fileSettings;
    final String path = "/itemStatus";
    final String pathSettings = "/settingStatus";


    Hashtable<Integer, MyItem> items = new Hashtable<Integer, MyItem>();
    Hashtable<Integer, View> itemsView = new Hashtable<Integer, View>();

    EditText messageToSend;

    boolean initialized = false;

    private int totalItems = 0;

    /* MQTT parameters
    BROKER_URL = "tcp://m21.cloudmqtt.com:12721";
    clientId = "f803h2famjisdsv8pub";
    MQTT_USER = "android";
    MQTT_PASS = {'a','n','d','r','o','i','d'};*/

    private MqttAndroidClient client;
    MqttConnectOptions option;
    Connection settings;

    //Intent contants
    static final int NEW_ITEM = 0;
    static final int MODIFY_ITEM = 1;
    static final int SETTINGS=2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    if(settings.connected)
                    {
                        initializeMQTT();
                        mqtt_connect();
                    }else{
                        Toast.makeText(getApplicationContext(), "Host not connected!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        // ask for permission
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }else{

        }
    }


    @Override
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

                    if(item.getType() == MyItem.TOGGLE_ITEM){
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

            case (SETTINGS):
                if (resultCode == ItemParametersActivity.RESULT_OK) {
                    settings = (Connection) data.getSerializableExtra("Connection");
                    initializeMQTT();
                    mqtt_connect();

                } else if (resultCode == ItemParametersActivity.RESULT_BACK) {
                    System.out.println("User pressed back button");
                } else {
                    Snackbar.make(new View(this), "Something goes wrong...", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }
                break;
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
                //cause.printStackTrace();
                //mqtt_connect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                System.out.println("Topic: "+topic+", Message: "+message.toString());
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
                    try {
                        client.subscribe("/setTemperature", 1);
                        System.out.println("--Subscribed to /setTemperature");
                        settings.connected=true;
                        Toast.makeText(getApplicationContext(), "Host connected!", Toast.LENGTH_SHORT).show();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getApplicationContext(), "Fail to connect!", Toast.LENGTH_SHORT).show();
                    System.out.println("--Fail to connect");
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
                    Hashtable<Integer, MyItem> itemi=(Hashtable<Integer, MyItem>) obj;
                    in.close();
                    for(int i=0;i<itemi.size();i++)
                    {
                        createNewItem(itemi.get(i));
                    }
                initialized = true;
            }
            if(fileSettings.exists() && fileSettings.canRead()) {

                fileSettings = new File(this.getFilesDir() + pathSettings);

                FileInputStream input = new FileInputStream(fileSettings);
                ObjectInputStream in = new ObjectInputStream(input);
                Object obj=in.readObject();
                settings=(Connection) obj;
                in.close();
                settings.connected=true;
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
        try {
            if(settings.connected){
                client.publish(topic, payload, qos, retained);
            }else{
                Toast.makeText(getApplicationContext(), "Host not connected!", Toast.LENGTH_SHORT).show();
            }
        } catch (MqttException e) {
            e.printStackTrace();
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
    void seekBarStartTracking(int key){

    }


    /**
     *
     * @param key
     */
    void seekBarStopTracking(int key){

    }


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
        try {
            if(settings.connected){
                client.publish(topic, payload, qos, retained);
            }else{
                Toast.makeText(getApplicationContext(), "Host not connected!", Toast.LENGTH_SHORT).show();
            }
        } catch (MqttException e) {
            e.printStackTrace();
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
        try {
            if(settings.connected){
                client.publish(topic, payload, qos, retained);
            }else{
                Toast.makeText(getApplicationContext(), "Host not connected!", Toast.LENGTH_SHORT).show();
            }
        } catch (MqttException e) {
            e.printStackTrace();
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
        items.remove(key);
        itemsView.remove(key);
        MyItem mi=null;
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
     * @param obj
     * @return
     */
    long getObjectSize(MyItem obj){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray().length;
    }


    /**
     *
     * @param obj
     * @return
     */
    byte[] getObjectByteArray(MyItem obj){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }


    /**
     *
     * @param obj
     * @return
     */
    byte[] getObjectByteArray(Hashtable obj){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
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
            output= new FileOutputStream(fileSettings);
            out = new ObjectOutputStream(output);
            out.writeObject(settings);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            MainActivity.this.startActivityForResult(myIntent, SETTINGS);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
