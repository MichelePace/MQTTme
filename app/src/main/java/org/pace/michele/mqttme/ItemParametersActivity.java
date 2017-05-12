package org.pace.michele.mqttme;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class ItemParametersActivity extends AppCompatActivity {

    final static int RESULT_BACK = 0;
    final static int RESULT_OK = 1;

    //IDs
    int PREFIX = 11111;
    int POSTFIX = 11112;
    final static int MIN = 11113;
    final static int MAX = 11114;
    final static int PRESSED = 11115;
    final static int UNPRESSED = 11116;

    int itemType;
    MyItem newItem = new MyItem();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_parameters);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Saving...", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();

                saveAndFinish(itemType);
            }
        });

        Intent intent = getIntent();
        itemType = intent.getIntExtra("ItemID", -1); //if param ItemID is not found, returns -1

        LinearLayout paramLayout = (LinearLayout) findViewById(R.id.paramLayout);

        LinearLayout horizontalLayout = new LinearLayout(this);
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);

        TextView t = new TextView(this);
        EditText e = new EditText(this);
        TextView t2 = new TextView(this);
        EditText e2 = new EditText(this);

        switch(itemType){
            case MyItem.TEXT_ITEM:
                t.setText("Prefix:");
                t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                horizontalLayout.addView(t);

                e.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                e.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));
                e.setId(R.id.prefix);
                horizontalLayout.addView(e);

                t2.setText("Postfix:");
                t2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                horizontalLayout.addView(t2);

                e2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                e2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));
                e2.setId(R.id.postfix);
                horizontalLayout.addView(e2);

                paramLayout.addView(horizontalLayout);
                break;

            case MyItem.RANGE_ITEM:
                t.setText("Min:");
                t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                horizontalLayout.addView(t);

                e.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                e.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));
                e.setText("0");
                e.setId(R.id.min);
                horizontalLayout.addView(e);

                t2.setText("Max:");
                t2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                horizontalLayout.addView(t2);

                e2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                e2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));
                e2.setText("100");
                e2.setId(R.id.max);
                horizontalLayout.addView(e2);

                paramLayout.addView(horizontalLayout);
                break;

            case MyItem.TOGGLE_ITEM:
                t.setText("Pressed:");
                t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                horizontalLayout.addView(t);

                e.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                e.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));
                e.setText("on");
                e.setId(R.id.pressed);
                horizontalLayout.addView(e);

                t2.setText("Unpressed:");
                t2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                horizontalLayout.addView(t2);

                e2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                e2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0.25f));
                e2.setText("off");
                e2.setId(R.id.unpressed);
                horizontalLayout.addView(e2);

                paramLayout.addView(horizontalLayout);
                break;

            default:
                finish();
                break;
        }

    }


    void saveAndFinish(int itemType){

        String name;
        String pub;
        String sub;
        int qosId;
        int qos;
        boolean retained;

        switch(itemType) {
            case MyItem.TEXT_ITEM:

                name = ((EditText)findViewById(R.id.name)).getText().toString();
                pub = ((EditText)findViewById(R.id.pubTopic)).getText().toString();
                sub = ((EditText)findViewById(R.id.subTopic)).getText().toString();
                qosId = ((RadioGroup)findViewById(R.id.qos)).getCheckedRadioButtonId();
                qos = Integer.parseInt(((RadioButton)findViewById(qosId)).getText().toString());
                retained = ((CheckBox)findViewById(R.id.retained)).isChecked();
                String prefix = ((EditText)findViewById(R.id.prefix)).getText().toString();
                String postfix = ((EditText)findViewById(R.id.postfix)).getText().toString();

                newItem = new MyItem(name, MyItem.TEXT_ITEM, null, pub, sub, qos, retained, prefix,postfix);
                break;

            case MyItem.RANGE_ITEM:

                name = ((EditText)findViewById(R.id.name)).getText().toString();
                pub = ((EditText)findViewById(R.id.pubTopic)).getText().toString();
                sub = ((EditText)findViewById(R.id.subTopic)).getText().toString();
                qosId = ((RadioGroup)findViewById(R.id.qos)).getCheckedRadioButtonId();
                qos = Integer.parseInt(((RadioButton)findViewById(qosId)).getText().toString());
                retained = ((CheckBox)findViewById(R.id.retained)).isChecked();
                int min = Integer.parseInt(((EditText)findViewById(R.id.min)).getText().toString());
                int max = Integer.parseInt(((EditText)findViewById(R.id.max)).getText().toString());

                newItem = new MyItem(name, MyItem.RANGE_ITEM, null, pub, sub, qos, retained, min, max);
                break;

            case MyItem.TOGGLE_ITEM:

                name = ((EditText)findViewById(R.id.name)).getText().toString();
                pub = ((EditText)findViewById(R.id.pubTopic)).getText().toString();
                sub = ((EditText)findViewById(R.id.subTopic)).getText().toString();
                qosId = ((RadioGroup)findViewById(R.id.qos)).getCheckedRadioButtonId();
                qos = Integer.parseInt(((RadioButton)findViewById(qosId)).getText().toString());
                retained = ((CheckBox)findViewById(R.id.retained)).isChecked();
                String pressed = ((EditText)findViewById(R.id.pressed)).getText().toString();
                String unpressed = ((EditText)findViewById(R.id.unpressed)).getText().toString();

                newItem = new MyItem(name, MyItem.TOGGLE_ITEM, null, pub, sub, qos, retained, false, pressed, unpressed);
                break;
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("newItem", newItem);
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    @Override
    protected void onStop(){
        super.onStop();
        Intent resultIntent = new Intent();
        setResult(RESULT_BACK, resultIntent);
        finish();
    }

}
