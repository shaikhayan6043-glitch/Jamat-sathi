
package com.jamatsathi.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

@CapacitorPlugin(name = "AnnouncementTranslation")
public class AnnouncementTranslationPlugin extends Plugin {

    @PluginMethod
    public void translateToHindi(PluginCall call) {
        String sourceText = call.getString("text", "");

        if (sourceText.trim().isEmpty()) {
            call.reject("अनुवाद के लिए टेक्स्ट खाली है।");
            return;
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.HINDI)
            .build();

        Translator translator = Translation.getClient(options);

        DownloadConditions conditions =
            new DownloadConditions.Builder().build();

        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener(unused ->
                translator.translate(sourceText)
                    .addOnSuccessListener(translatedText -> {
                        translator.close();
                        JSObject result = new JSObject();
                        result.put("text", translatedText);
                        call.resolve(result);
                    })
                    .addOnFailureListener(error -> {
                        translator.close();
                        call.reject("हिंदी अनुवाद नहीं हो पाया: "
                            + error.getMessage(), error);
                    })
            )
            .addOnFailureListener(error -> {
                translator.close();
                call.reject("हिंदी भाषा मॉडल डाउनलोड नहीं हुआ: "
                    + error.getMessage(), error);
            });
    }
}
