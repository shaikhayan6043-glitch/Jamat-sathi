
package com.jamatsathi.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.media.AudioAttributes;
import android.os.Bundle;

import java.util.Locale;

public class PrayerSpeechService extends Service {

    private static final String CHANNEL_ID =
        "jamat_sathi_speech_channel";

    private static final int NOTIFICATION_ID = 777;
    private PowerManager.WakeLock wakeLock;

private void acquireSpeechWakeLock() {
    if (wakeLock == null) {
        PowerManager powerManager =
            (PowerManager) getSystemService(POWER_SERVICE);

        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "JamatSathi:PrayerSpeech"
        );
        wakeLock.setReferenceCounted(false);
    }

    if (!wakeLock.isHeld()) {
        wakeLock.acquire(2 * 60 * 1000L);
    }
}

private void releaseSpeechWakeLock() {
    if (wakeLock != null && wakeLock.isHeld()) {
        wakeLock.release();
    }
}
    private TextToSpeech textToSpeech;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(
            NOTIFICATION_ID,
            createNotification()
        );

        String message = intent == null
            ? null
            : intent.getStringExtra("message");

        if (message == null || message.trim().isEmpty()) {
            stopSelf(startId);
            releaseSpeechWakeLock();
            return START_NOT_STICKY;
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

        textToSpeech = new TextToSpeech(
            getApplicationContext(),
            status -> {
                if (status != TextToSpeech.SUCCESS) {
                    stopSelf(startId);
                    return;
                }

                int languageResult =
                    textToSpeech.setLanguage(
                        new Locale("hi", "IN")
                    );

                if (languageResult ==
                        TextToSpeech.LANG_MISSING_DATA
                    || languageResult ==
                        TextToSpeech.LANG_NOT_SUPPORTED) {
                    stopSelf(startId);
                    return;
                }

                textToSpeech.setAudioAttributes(
    new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
);
                textToSpeech.setSpeechRate(0.90f);

                textToSpeech.setOnUtteranceProgressListener(
                    new UtteranceProgressListener() {

                        @Override
                        public void onStart(String utteranceId) {
                        }

                        @Override
                        public void onDone(String utteranceId) {
                            stopSelf(startId);
                        }

                        @Override
                        public void onError(String utteranceId) {
                            stopSelf(startId);
                        }
                    }
                );
                
            Bundle speechParams = new Bundle();
                speechParams.putFloat(
                    TextToSpeech.Engine.KEY_PARAM_VOLUME,
                    1.0f
                );

                textToSpeech.speak(
                    message,
                    TextToSpeech.QUEUE_FLUSH,
                    speechParams,
                    "JAMAT_SATHI_PRAYER_" + startId
                );
            }
        );

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                new NotificationChannel(
                    CHANNEL_ID,
                    "JAMAT SATHI Voice Alert",
                    NotificationManager.IMPORTANCE_LOW
                );

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("JAMAT SATHI")
                .setContentText("नमाज़ का आवाज़ वाला अलर्ट")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setOngoing(true)
                .build();
        }

        return new Notification.Builder(this)
            .setContentTitle("JAMAT SATHI")
            .setContentText("नमाज़ का आवाज़ वाला अलर्ट")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }

       releaseSpeechWakeLock();
        super.onDestroy();
        
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
