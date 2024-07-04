package com.efttt.lockall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ScreenTimeReceiver extends BroadcastReceiver {

    private static boolean screenOn = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            screenOn = true;
            Log.d("ScreenTimeReceiver", "Screen ON at: " + System.currentTimeMillis());
        } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            screenOn = false;
            Log.d("ScreenTimeReceiver", "Screen OFF at: " + System.currentTimeMillis());
        }
    }

    public static boolean isScreenOn() {
        return screenOn;
    }
}