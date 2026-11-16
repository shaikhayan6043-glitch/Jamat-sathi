
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
    public void checkBatteryOptimization(PluginCall call) {
        try {
            PowerManager powerManager =
                (PowerManager) getContext().getSystemService(
                    Context.POWER_SERVICE
                );

            boolean allowed = true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                allowed = powerManager.isIgnoringBatteryOptimizations(
                    getContext().getPackageName()
                );
            }

            JSObject result = new JSObject();
            result.put("allowed", allowed);
            call.resolve(result);

        } catch (Exception error) {
            call.reject("Battery permission जाँच नहीं हो सकी।", error);
        }
    }
    
    @PluginMethod
    public void openAppBatterySettings(PluginCall call) {
        try {
            Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            );
            intent.setData(Uri.parse(
                "package:" + getContext().getPackageName()
            ));
            getActivity().startActivity(intent);
            call.resolve();
        } catch (Exception error) {
            call.reject("ऐप की बैटरी सेटिंग नहीं खुली।", error);
        }
    }

    @PluginMethod
    public void openTextToSpeechSettings(PluginCall call) {
        try {
            Intent intent = new Intent("com.android.settings.TTS_SETTINGS");

            if (intent.resolveActivity(
                getContext().getPackageManager()
            ) == null) {
                intent = new Intent(
                    TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                );
            }

            getActivity().startActivity(intent);
            call.resolve();
        } catch (Exception error) {
            call.reject("Text-to-Speech सेटिंग नहीं खुली।", error);
        }
    }
 
    @PluginMethod
    public void checkHindiVoice(PluginCall call) {
        final TextToSpeech[] holder = new TextToSpeech[1];

        try {
            holder[0] = new TextToSpeech(
                getContext(),
                status -> {
                    TextToSpeech tts = holder[0];
                    boolean available = false;

                    try {
                        if (status == TextToSpeech.SUCCESS && tts != null) {
                            int result = tts.setLanguage(
                                new Locale("hi", "IN")
                            );

                            available =
                                result != TextToSpeech.LANG_MISSING_DATA &&
                                result != TextToSpeech.LANG_NOT_SUPPORTED;
                        }

                        JSObject response = new JSObject();
                        response.put("available", available);
                        call.resolve(response);

                    } catch (Exception error) {
                        call.reject("Hindi Voice जाँच नहीं हो सकी।", error);
                    } finally {
                        if (tts != null) {
                            tts.shutdown();
                        }
                    }
                }
            );
        } catch (Exception error) {
            call.reject("Text-to-Speech शुरू नहीं हो सका।", error);
        }
    }
    
        @PluginMethod
    public void speakAnnouncement(PluginCall call) {
        String title = call.getString("title", "");
        String message = call.getString("message", "");
        String text = title + "। " + message;

        if (text.trim().isEmpty()) {
            call.reject("ऐलान खाली है।");
            return;
        }

        final TextToSpeech[] holder = new TextToSpeech[1];

        holder[0] = new TextToSpeech(
            getContext(),
            status -> {
                TextToSpeech tts = holder[0];

                if (status != TextToSpeech.SUCCESS) {
                    if (tts != null) tts.shutdown();
                    call.reject("फोन की Text-to-Speech सुविधा शुरू नहीं हुई।");
                    return;
                }

                int result = tts.setLanguage(new Locale("hi", "IN"));

                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.shutdown();
                    call.reject("फोन में Hindi voice data उपलब्ध नहीं है।");
                    return;
                }

                tts.setSpeechRate(0.9f);

                tts.setOnUtteranceProgressListener(
                    new android.speech.tts.UtteranceProgressListener() {
                        @Override
                        public void onStart(String id) {
                        }

                        @Override
                        public void onDone(String id) {
                            tts.shutdown();
                        }

                        @Override
                        public void onError(String id) {
                            tts.shutdown();
                        }
                    }
                );

                tts.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "JAMAT_ANNOUNCEMENT"
                );

                call.resolve();
            }
        );
    }
    @PluginMethod
    public void replaceAlarms(PluginCall call) {
        try {
            Context context = getContext();

            AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(
                    Context.ALARM_SERVICE
                );

            JSArray alarms = call.getArray("alarms");

            if (alarms == null) {
                call.reject("Voice alarms की सूची नहीं मिली।");
                return;
            }

            // पहले पुराने सभी voice alarms हटाएँ
            for (int id = 800000000; id < 800000300; id++) {
                Intent oldIntent = new Intent(
                    context,
                    SpeechAlarmReceiver.class
                );

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

            // खाली सूची हो तो अनुमति के बिना भी पुराने अलर्ट हट सकेंगे
            if (
                alarms.length() > 0 &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                context.getSharedPreferences(
                    "JamatSathiAlarms",
                    Context.MODE_PRIVATE
                ).edit()
                    .putString("voice_alarms", "[]")
                    .apply();

                call.reject("Alarms & reminders की अनुमति नहीं मिली।");
                return;
            }

            context.getSharedPreferences(
                "JamatSathiAlarms",
                Context.MODE_PRIVATE
            ).edit()
                .putString("voice_alarms", alarms.toString())
                .apply();

            int scheduled = 0;
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
