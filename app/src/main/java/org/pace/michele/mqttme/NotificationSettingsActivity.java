package org.pace.michele.mqttme;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashMap;

public class NotificationSettingsActivity extends AppCompatActivity {

    private static final String TAG = "NotificationSettingsActivity";

    private Hashtable<String, Integer> topics = new Hashtable<String, Integer>();
    private Hashtable<String, MyNotification> notifications = new Hashtable<String, MyNotification>();
    private boolean notify = false;

    private Hashtable<String, View> items = new Hashtable<String, View>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Intent intent = getIntent();
        Serializable temp = intent.getSerializableExtra("Topics");
        if(temp != null){
            topics = new Hashtable<String, Integer>((HashMap<String, Integer>)temp);
        }
        temp = intent.getSerializableExtra("Notifications");

        if(temp != null) {
            notifications = new Hashtable<String, MyNotification>((HashMap<String, MyNotification>) temp);
        }

        showOldSettings();
    }


    private void showOldSettings(){

        LinearLayout layout = (LinearLayout)findViewById(R.id.notificationLayout);

        LayoutInflater inflater = NotificationSettingsActivity.this.getLayoutInflater();
        View item = null;

        Enumeration<String> keys = topics.keys();

        ((Switch) layout.findViewById(R.id.switchAllNotifications)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                changeNotifyAll(isChecked);
            }
        });


        //If settings have been already changed
        if(notifications.containsKey("notifications")){

            notify = notifications.get("notifications").getNotify();
            ((Switch)layout.findViewById(R.id.switchAllNotifications)).setChecked(notify);

            for(int i = 0; i < topics.size(); i++) {

                final String key = keys.nextElement();

                item = inflater.inflate(R.layout.notification_setting_item, null);

                ((TextView) item.findViewById(R.id.topic)).setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                ((TextView) item.findViewById(R.id.topic)).setText(key);

                if(notifications.containsKey(key)) {
                    ((Switch) item.findViewById(R.id.n_switch)).setChecked(notifications.get(key).getNotify());
                    ((Spinner) item.findViewById(R.id.spinner)).setSelection(notifications.get(key).getType());
                    ((CheckBox) item.findViewById(R.id.checkbox_sm)).setChecked(notifications.get(key).getNotShowSame());
                }

                ((Switch)item.findViewById(R.id.n_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        changeNotify(isChecked, key);
                    }
                });

                ((Spinner)item.findViewById(R.id.spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        changeType(position, key);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) { }
                });

                ((CheckBox)item.findViewById(R.id.checkbox_sm)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        changeNotShowSame(isChecked, key);
                    }
                });

                Space space = new Space(this);
                space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40));

                layout.addView(space);
                layout.addView(item);
                items.put(key, item);
            }

        }else{

            notifications.put("notifications", new MyNotification(false, 0, false));

            for(int i = 0; i < topics.size(); i++) {
                item = inflater.inflate(R.layout.notification_setting_item, null);
                final String key = keys.nextElement();

                ((TextView) item.findViewById(R.id.topic)).setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
                ((TextView) item.findViewById(R.id.topic)).setText(key);

                ((Switch)item.findViewById(R.id.n_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        changeNotify(isChecked, key);
                    }
                });

                ((Spinner)item.findViewById(R.id.spinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        changeType(position, key);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

                Space space = new Space(this);
                space.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40));

                layout.addView(space);
                layout.addView(item);
                items.put(key, item);
            }
        }
    }


    /**
     *
     * @param isChecked
     */
    private void changeNotifyAll(boolean isChecked){
        if(isChecked){
            notifications.get("notifications").setNotify(isChecked);
        }else{
            notifications.get("notifications").setNotify(isChecked);
        }
    }


    /**
     *
     * @param isChecked
     * @param key
     */
    private void changeNotify(boolean isChecked, String key){
        if(notifications.containsKey(key)) {
            notifications.get(key).setNotify(isChecked);
        }else{
            notifications.put(key, new MyNotification(isChecked, 0, false));
        }
    }


    /**
     *
     * @param position
     * @param key
     */
    private void changeType(int position, String key){
        if(notifications.containsKey(key)) {
            notifications.get(key).setType(position);
        }else{
            notifications.put(key, new MyNotification(false, position, false));
        }
    }


    /**
     *
     * @param isChecked
     * @param key
     */
    private void changeNotShowSame(boolean isChecked, String key){
        if(notifications.containsKey(key)) {
            notifications.get(key).setNotShowSame(isChecked);
        }else{
            notifications.put(key, new MyNotification(false, 0, isChecked));
        }
    }


    private void saveAndFinish(){
        Intent resultIntent = new Intent();
        resultIntent.putExtra("Notifications", notifications);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    @Override
    public void onBackPressed() {
        saveAndFinish();
        super.onBackPressed();
    }

}
