package org.pace.michele.mqttme;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

public class MyBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "MainActivity";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Intent myIntent = new Intent(context, PushNotificationService.class);
            context.startService(myIntent);
            Log.v(TAG, " +++ BOOT_COMPLETED intent");
        }

        if ("android.intent.action.USER_PRESENT".equals(intent.getAction())) {
            if(! isMyServiceRunning(PushNotificationService.class, context)) {
                Intent myIntent = new Intent(context, PushNotificationService.class);
                context.startService(myIntent);
                Log.v(TAG, " +++ USER_PRESENT intent - service started");
            }else{
                Log.v(TAG, " +++ USER_PRESENT intent - service already started");
            }
        }
    }

    /**
     *
     * @param serviceClass
     * @return
     */
    private boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}
