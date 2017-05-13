package org.pace.michele.mqttme;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import static android.R.attr.key;
import static org.pace.michele.mqttme.ItemParametersActivity.RESULT_BACK;

/**
 * Created by gianvito on 13/05/17.
 */

public class SettingConnectionActivity extends AppCompatActivity {

    public Connection settings = new Connection();
    public Connection settingsIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_connection);

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
        settings.setAddress(((EditText)findViewById(R.id.edit_address)).getText().toString());
        settings.setport(Integer.parseInt(((EditText)findViewById(R.id.edit_port)).getText().toString()));
        settings.setUsername(((EditText)findViewById(R.id.edit_username)).getText().toString());
        settings.setPassword(((EditText)findViewById(R.id.edit_password)).getText().toString());
        settings.setClientId(((EditText)findViewById(R.id.edit_clientId)).getText().toString());
        Intent resultIntent = new Intent();
        resultIntent.putExtra("Connection", settings);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    void showOldSettings(){
        ((EditText)findViewById(R.id.edit_username)).setText(settingsIntent.getUsername());
        ((EditText)findViewById(R.id.edit_address)).setText(settingsIntent.getAddress());
        ((EditText)findViewById(R.id.edit_port)).setText(String.valueOf(settingsIntent.getPort()));
        ((EditText)findViewById(R.id.edit_clientId)).setText(settingsIntent.getClientId());
        ((EditText)findViewById(R.id.edit_password)).setText(settingsIntent.getPassword());
    }

    @Override
    protected void onStop(){
        super.onStop();
        Intent resultIntent = new Intent();
        setResult(RESULT_BACK, resultIntent);
        finish();
    }


}
