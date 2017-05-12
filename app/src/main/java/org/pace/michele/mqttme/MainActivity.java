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

public class MainActivity extends AppCompatActivity {

    File file;
    final String path = "/itemStatus";


    Hashtable<Integer, MyItem> items = new Hashtable<Integer, MyItem>();
    Hashtable<Integer, View> itemsView = new Hashtable<Integer, View>();

    EditText messageToSend;

    boolean initialized = false;

    private int totalItems = 0;

    // MQTT parameters

    public static final String BROKER_URL = "tcp://m21.cloudmqtt.com:12721";
    private MqttAndroidClient client;
    MqttConnectOptions option;
    private final String clientId = "f803h2famjisdsv8pub";
    private final String MQTT_USER = "android";
    private final char[] MQTT_PASS = {'a','n','d','r','o','i','d'};

    static final int PARAMETERS_ACTIVITY = 0;

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


        initializeMQTT();
        mqtt_connect();

        // Initialize layout
        LinearLayout column = (LinearLayout) findViewById(R.id.left_column);

        column.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(!initialized) {
                    initialize();
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

        //if(requestCode != PARAMETERS_ACTIVITY) {

            switch (requestCode) {
                case (PARAMETERS_ACTIVITY): {
                    if (resultCode == ItemParametersActivity.RESULT_OK) {
                        MyItem newItem = (MyItem) data.getSerializableExtra("newItem");
                        createNewItem(newItem);
                    } else if (resultCode == ItemParametersActivity.RESULT_BACK) {
                        System.out.println("User pressed back button");
                    } else {
                        Snackbar.make(new View(this), "Something goes wrong...", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                    break;

                }

                default:
                    System.out.println("DEFAULT IN SWITCH");
                    break;
            }
        //}
    }


    /**
     *
     */
    void initializeMQTT(){

        MemoryPersistence memPer = new MemoryPersistence();
        client = new MqttAndroidClient(this.getApplicationContext(), BROKER_URL, clientId, memPer);
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
        option.setUserName(MQTT_USER);
        option.setPassword(MQTT_PASS);
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
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
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

        if(file.exists() && file.canRead()) {

            file = new File(this.getFilesDir() + path);
            try {
                FileInputStream input = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(input);
                Object obj=in.readObject();
                items=(Hashtable<Integer, MyItem>) obj;
                in.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println("+++++++++++++++++++ "+items.size());
        }else{
            System.out.println("File not exists");
        }


        /*int number = 3;*/

        LinearLayout leftColumn = (LinearLayout) findViewById(R.id.left_column);
        LinearLayout rightColumn = (LinearLayout) findViewById(R.id.right_column);

        int itemDimension = leftColumn.getWidth();

        initialized = true;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(itemDimension, itemDimension);
        params.setMargins(0,0,0,5);

        LayoutInflater inflater_one_user = MainActivity.this.getLayoutInflater();
        View item = null;

        /*for(int i = 0; i < number; i++){

            final int key = i;

            //IF TOGGLE_ITEM
            if(MyItem.TOGGLE_ITEM == 2) {
                item = inflater_one_user.inflate(R.layout.toggle_item, null);
                TextView itemName = (TextView) item.findViewById(R.id.name);

                itemName.setText("PROVA" + i);
                item.setLayoutParams(params);

                ToggleButton tb = (ToggleButton) item.findViewById(R.id.toggleButton);
                tb.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        togglePressed(key);
                    }
                });

            }else if(MyItem.RANGE_ITEM == 1){

                //IF RANGE_ITEM
                item = inflater_one_user.inflate(R.layout.range_item, null);
                TextView itemName = (TextView) item.findViewById(R.id.name);

                itemName.setText("PROVA" + i);
                item.setLayoutParams(params);

                SeekBar sb = ((SeekBar) item.findViewById(R.id.seekBar));
                sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        seekBarProgressChanged(key, progress);
                    }

                    public void onStartTrackingTouch(SeekBar seekBar) {
                        seekBarStartTracking(key);
                    }

                    public void onStokeyackingTouch(SeekBar seekBar) {
                        seekBarStokeyacking(key);
                    }
                });

                item.findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        buttonSetClicked(key);
                    }
                });

            }else if(MyItem.TEXT_ITEM == 2){

                //IF TEXT_ITEM
                item = inflater_one_user.inflate(R.layout.text_item, null);
                TextView itemName = (TextView) item.findViewById(R.id.name);
                TextView message = (TextView) item.findViewById(R.id.message);

                itemName.setText("PROVA" + i);
                item.setLayoutParams(params);

                item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        messageClicked(key);
                    }
                });

                message.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        messageClicked(key);
                    }
                });

            }

            if(item != null) {
                if (i % 2 == 0) {
                    leftColumn.addView(item);
                } else {
                    rightColumn.addView(item);
                }

                MyItem mi = new MyItem("", MyItem.RANGE_ITEM, item, "", "",0,true,"","");
                items.add(mi);
            }
        }*/
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
            client.publish(topic, payload, qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param key
     */
    void longClickToggle(int key){
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
                //modifyItem(key);
                d.dismiss();
            }
        });

        range.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //deleteItem(key);
                d.dismiss();
            }
        });

        toggle.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //duplicateItem(prt);
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
            TextView tv = (TextView) item.findViewById(R.id.progress);
            tv.setText(""+progress);
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
    void seekBarStokeyacking(int key){

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
        SeekBar sb = (SeekBar) item.findViewById(R.id.seekBar);
        String message = String.valueOf(sb.getProgress());

        byte[] payload = message.getBytes();
        try {
            client.publish(topic, payload, qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param key
     */
    void longClickRange(int key){
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
                //modifyItem(key);
                d.dismiss();
            }
        });

        range.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //deleteItem(key);
                d.dismiss();
            }
        });

        toggle.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //duplicateItem(prt);
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
            client.publish(topic, payload, qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param key
     */
    void longClickText(int key){
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
                //modifyItem(key);
                d.dismiss();
            }
        });

        range.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //deleteItem(key);
                d.dismiss();
            }
        });

        toggle.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //duplicateItem(prt);
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
        myIntent.putExtra("ItemID", item); //Optional parameters, sends thi item type
        MainActivity.this.startActivityForResult(myIntent, PARAMETERS_ACTIVITY);
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

            ToggleButton tb = (ToggleButton) item.findViewById(R.id.toggleButton);
            tb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    togglePressed(key, isChecked);
                }
            });

            item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    longClickToggle(key);
                    return false;
                }
            });

        }else if(type == MyItem.RANGE_ITEM){

            //IF RANGE_ITEM
            item = inflater_one_user.inflate(R.layout.range_item, null);
            TextView itemName = (TextView) item.findViewById(R.id.name);

            itemName.setText(mi.getName());
            item.setLayoutParams(params);

            SeekBar sb = ((SeekBar) item.findViewById(R.id.seekBar));
            sb.setMax(mi.getMax() - mi.getMin());
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    seekBarProgressChanged(key, progress);
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                    seekBarStartTracking(key);
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                    seekBarStokeyacking(key);
                }
            });

            item.findViewById(R.id.set).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buttonSetClicked(key);
                }
            });

            item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    longClickRange(key);
                    return false;
                }
            });

        }else if(type == MyItem.TEXT_ITEM){

            //IF TEXT_ITEM
            item = inflater_one_user.inflate(R.layout.text_item, null);
            TextView itemName = (TextView) item.findViewById(R.id.name);
            TextView message = (TextView) item.findViewById(R.id.message);

            itemName.setText(mi.getName());
            item.setLayoutParams(params);

            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    messageClicked(key);
                }
            });

            message.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    messageClicked(key);
                }
            });

            item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    longClickText(key);
                    return false;
                }
            });
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


    void salva() {
        /*itemStatus = new File(this.getFilesDir() + path);
        try {
            FileOutputStream file= new FileOutputStream(itemStatus);
            ObjectOutputStream out = new ObjectOutputStream(file);
            out.writeObject(items);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }


    @Override
    public void onStop(){
        super.onStop();

        /*itemStatus = new File(this.getFilesDir() + path);
        Enumeration e = items.keys();
        int k;
        try {
            FileOutputStream file = new FileOutputStream(itemStatus);
            ObjectOutputStream out = new ObjectOutputStream(file);

            while (e.hasMoreElements()){

                k = (int)e.nextElement();
                out.writeObject(items.get(k));
                System.out.println("Scritto bene++++++++++++++++++++++++++++++++++++++++++++++");

            }

            out.flush();
            out.close();

        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }*/

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
