package org.pace.michele.mqttme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class AlarmActivity extends AppCompatActivity {

    private long[] vibration = {100, 600, 800};
    private Uri ringtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    Vibrator v;
    private MediaPlayer mp;

    private Handler mHandler;
    private int mInterval = 60000; //60 seconds
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            stop(STOPPED_FROM_TIMER);
        }
    };

    ServiceConnection mConnection;
    private boolean mBound = false;
    PushNotificationService mService;

    private String message = "";
    private String topic = "";

    private NotificationManager notificationManager;

    private int STOPPED_FROM_BUTTON = 0;
    private int STOPPED_FROM_TIMER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_alarm);

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop(STOPPED_FROM_BUTTON);
            }
        });

        Intent intent = getIntent();
        message = intent.getStringExtra("Message");
        topic = intent.getStringExtra("Topic");
        if(message != null && topic != null) {
            TextView t = (TextView) findViewById(R.id.message);
            t.setText(topic + ":\n" + message);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onAttachedToWindow() {
        hide();
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        //Call stop after mInterval seconds
        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, mInterval);

        vibrateAndRing();
    }


    /**
     *
     */
    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }


    /**
     *
     */
    private void vibrateAndRing(){

        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mp = MediaPlayer.create(AlarmActivity.this, ringtone);

        AudioManager audio = (AudioManager) AlarmActivity.this.getSystemService(Context.AUDIO_SERVICE);

        switch( audio.getRingerMode() ){
            case AudioManager.RINGER_MODE_NORMAL:
                v.vibrate(vibration, 0); // The '0' here means to repeat indefinitely
                mp.start();
                //Restart ringtone when finished
                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        System.out.println("+++++++ON COMPLETITION LISTENER");
                        mp.start();
                    }
                });
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                v.vibrate(vibration, 0); // The '0' here means to repeat indefinitely
                break;
        }



    }

    private void stop(int code){

        if(code == STOPPED_FROM_TIMER) {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification.Builder(this)
                    .setContentTitle("Alarm")
                    .setContentText(topic + ": " + message)
                    .setSmallIcon(R.drawable.ic_ico_notify)
                    .setContentIntent(pIntent)
                    .setAutoCancel(true)
                    .setVibrate(vibration)
                    .setSound(ringtone).build();

            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(1, notification);
        }

        mHandler.removeCallbacks(mRunnable);
        mHandler.removeCallbacksAndMessages(null);
        v.cancel();
        mp.stop();

        finish();
    }

}
