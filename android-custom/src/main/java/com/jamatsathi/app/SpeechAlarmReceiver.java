
package com.jamatsathi.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class SpeechAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String message = intent.getStringExtra("message");
        long scheduledAt = intent.getLongExtra("scheduled_at", 0);
long receivedAt = System.currentTimeMillis();

android.util.Log.i(
    "JAMAT_SATHI",
    "ALARM_TIMING: scheduled=" + scheduledAt +
    ", received=" + receivedAt +
    ", delay_ms=" + (receivedAt - scheduledAt)
);

        if (message == null || message.trim().isEmpty()) {
            return;
        }

        Intent serviceIntent = new Intent(
            context,
            PrayerSpeechService.class
        );

        serviceIntent.putExtra("message", message);
        serviceIntent.putExtra("scheduled_at", scheduledAt);

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
