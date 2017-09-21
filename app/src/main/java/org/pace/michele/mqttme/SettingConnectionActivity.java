package org.pace.michele.mqttme;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class SettingConnectionActivity extends AppCompatActivity {

    private static final String TAG = "SettingConnectionActivity";

    public Connection settings = new Connection();
    public Connection settingsIntent;

    final static int RESULT_BACK = 0;
    final static int RESULT_OK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_connection);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Intent intent = getIntent();
        settingsIntent = (Connection) intent.getSerializableExtra("Connection");
        showOldSettings();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Saving...", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                saveAndFinish();
            }
        });
    }

    void saveAndFinish()
    {
        boolean server = false;
        boolean port = false;

        if(((EditText)findViewById(R.id.edit_address)).getText().toString().isEmpty()){
            Toast.makeText(getApplicationContext(), "You must set a server address", Toast.LENGTH_SHORT).show();
        }else{
            settings.setAddress(((EditText)findViewById(R.id.edit_address)).getText().toString());
            server = true;
        }

        if(((EditText)findViewById(R.id.edit_port)).getText().toString().isEmpty()){
            Toast.makeText(getApplicationContext(), "You must set a server port", Toast.LENGTH_SHORT).show();
        }else{
            settings.setport(Integer.parseInt(((EditText)findViewById(R.id.edit_port)).getText().toString()));
            port = true;
        }

        if(((EditText)findViewById(R.id.edit_username)).getText().toString().isEmpty()){
            settings.setUsername("");
        }else{
            settings.setUsername(((EditText)findViewById(R.id.edit_username)).getText().toString());
        }

        if(((EditText)findViewById(R.id.edit_password)).getText().toString().isEmpty()){
            settings.setPassword("");
        }else{
            settings.setPassword(((EditText)findViewById(R.id.edit_password)).getText().toString());
        }

        if(((EditText)findViewById(R.id.edit_clientID)).getText().toString().isEmpty()){
            settings.setClientId("mqttme-3333333");
        }else{
            settings.setClientId(((EditText)findViewById(R.id.edit_clientID)).getText().toString());
        }

        if(server && port) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("Connection", settings);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    void showOldSettings(){
        ((EditText)findViewById(R.id.edit_username)).setText(settingsIntent.getUsername());
        ((EditText)findViewById(R.id.edit_address)).setText(settingsIntent.getAddress());
        ((EditText)findViewById(R.id.edit_port)).setText(String.valueOf(settingsIntent.getPort()));
        ((EditText)findViewById(R.id.edit_clientID)).setText(settingsIntent.getClientId());
        ((EditText)findViewById(R.id.edit_password)).setText(settingsIntent.getPassword());
    }

    @Override
    protected void onStop(){
        super.onStop();
        saveAndFinish();
        finish();
    }

}
