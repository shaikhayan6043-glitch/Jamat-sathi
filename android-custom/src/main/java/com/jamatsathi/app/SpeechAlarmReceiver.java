
package com.jamatsathi.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class SpeechAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("message");

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        Intent serviceIntent = new Intent(
            context,
            PrayerSpeechService.class
        );

        serviceIntent.putExtra("message", message);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } catch (Exception error) {
            android.util.Log.e(
                "JAMAT_SATHI",
                "Speech service start failed",
                error
            );
        }
    }
}
