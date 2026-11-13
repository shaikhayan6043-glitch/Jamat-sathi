
import { Capacitor, registerPlugin } from "@capacitor/core";
import { LocalNotifications } from "@capacitor/local-notifications";

// Android के लिए Capacitor उपलब्ध कराना
window.Capacitor = Capacitor;

// पुराने नोटिफिकेशन सिस्टम को उपलब्ध कराना
window.LocalNotifications = LocalNotifications;

window.prayerNotificationsReady = Promise.resolve(
  LocalNotifications
);

// नया हिंदी आवाज़ वाला Android प्लगइन
window.PrayerSpeech = registerPlugin("PrayerSpeech");
window.AnnouncementTranslation = registerPlugin("AnnouncementTranslation");
