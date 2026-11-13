
package com.jamatsathi.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

@CapacitorPlugin(name = "PrayerSpeech")
public class PrayerSpeechPlugin extends Plugin {

    private static final String ACTION =
        "com.jamatsathi.app.SPEAK_PRAYER";

      @PluginMethod
    public void requestBatteryOptimizationExemption(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PowerManager powerManager =
                    (PowerManager) getContext().getSystemService(
                        Context.POWER_SERVICE
                    );

                if (!powerManager.isIgnoringBatteryOptimizations(
                    getContext().getPackageName())) {

                    Intent intent = new Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    );

                    intent.setData(Uri.parse(
                        "package:" + getContext().getPackageName()
                    ));

                    getActivity().startActivity(intent);
                }
            }

            call.resolve();

        } catch (Exception error) {
            call.reject("Battery permission नहीं खुली।", error);
        }
    }
    @PluginMethod
    public void replaceAlarms(PluginCall call) {
        try {
            Context context = getContext();
            AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                    Context.ALARM_SERVICE
                );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !alarmManager.canScheduleExactAlarms()) {
                call.reject("Alarms & reminders की अनुमति नहीं मिली।");
                return;
            }

            // पुराने सभी JAMAT SATHI voice alarms हटाएँ
            for (int id = 800000000; id < 800000300; id++) {
                Intent oldIntent = new Intent(context,
                    SpeechAlarmReceiver.class);
                oldIntent.setAction(ACTION);

                PendingIntent oldPending =
                    PendingIntent.getBroadcast(
                        context,
                        id,
                        oldIntent,
                        PendingIntent.FLAG_NO_CREATE
                            | PendingIntent.FLAG_IMMUTABLE
                    );

                if (oldPending != null) {
                    alarmManager.cancel(oldPending);
                    oldPending.cancel();
                }
            }

            JSArray alarms = call.getArray("alarms");

            if (alarms == null) {
                call.reject("Voice alarms की सूची नहीं मिली।");
                return;
            }

            int scheduled = 0;
            long now = System.currentTimeMillis();

            for (int i = 0; i < alarms.length(); i++) {
                JSONObject item = alarms.getJSONObject(i);

                int id = item.getInt("id");
                long at = item.getLong("at");
                String message = item.getString("message");

                if (at <= now || message.trim().isEmpty()) {
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

                scheduled++;
            }

            JSObject result = new JSObject();
            result.put("scheduled", scheduled);
            call.resolve(result);

        } catch (Exception error) {
            call.reject(
                "Voice alarm सेट नहीं हुआ: " + error.getMessage(),
                error
            );
        }
    }
}
