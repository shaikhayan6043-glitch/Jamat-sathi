import { Capacitor } from "@capacitor/core";
import { LocalNotifications } from "@capacitor/local-notifications";

// Capacitor को Android ऐप में उपलब्ध कराना
window.Capacitor = Capacitor;

// Notification plugin को ऐप के अंदर उपलब्ध कराना
window.LocalNotifications = LocalNotifications;

window.prayerNotificationsReady = Promise.resolve(
  LocalNotifications
);
