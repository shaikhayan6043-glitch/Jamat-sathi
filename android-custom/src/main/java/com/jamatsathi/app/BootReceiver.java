package com.jamatsathi.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

public class BootReceiver extends BroadcastReceiver {

    private static final String ACTION =
        "com.jamatsathi.app.SPEAK_PRAYER";

    @Override
    public void onReceive(Context context, Intent receivedIntent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(
            receivedIntent.getAction())) {
            return;
        }

        try {
            String saved = context.getSharedPreferences(
                "JamatSathiAlarms",
                Context.MODE_PRIVATE
            ).getString("voice_alarms", "[]");

            JSONArray alarms = new JSONArray(saved);

            AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                    Context.ALARM_SERVICE
                );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !alarmManager.canScheduleExactAlarms()) {
                return;
            }

            long now = System.currentTimeMillis();

            for (int i = 0; i < alarms.length(); i++) {
                JSONObject item = alarms.getJSONObject(i);

                int id = item.getInt("id");
long at = item.getLong("at");
String message = item.optString("message", "");

if (
    id < 800000000 ||
    id >= 800000300 ||
    at <= now ||
    message.trim().isEmpty()
) {
    continue;
}

                Intent intent = new Intent(
                    context,
                    SpeechAlarmReceiver.class
                );

                intent.setAction(ACTION);
                intent.putExtra("message", message);

                PendingIntent pending =
                    PendingIntent.getBroadcast(
                        context,
                        id,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                            | PendingIntent.FLAG_IMMUTABLE
                    );

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    at,
                    pending
                );
            }

        } catch (Exception error) {
            android.util.Log.e(
                "JamatSathi",
                "Boot alarms restore failed",
                error
            );
        }
    }
}
