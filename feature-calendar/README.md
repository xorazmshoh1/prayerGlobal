# feature-calendar

Taqvim tabi (TZ 5, 6, 7, 11-bo'limlar, 10-bosqich):

- `domain/` — sof Kotlin: `CalendarGrid` (7 ustunli oylik setka, hijriy+milodiy), `IslamicEvent` (9 islomiy sana),
  `IslamicReminderPlanner` (sanadan 1 kun oldin, 09:00), `DateConverter` (ikki yo'nalish), portlar (`PrayerTimetablePort`,
  `CalendarPrefsStore`, `EventPrefsStore`).
- `data/` — DataStore (`DataStoreCalendarPrefs`): asosiy tizim va islomiy sana sozlamalari.
- `ui/` — `CalendarScreen` (setka, 30 kunlik namoz jadvali, konvertor), `IslamicEventsScreen`, ViewModel'lar.
- `notifications/` — "Islomiy sanalar" kanali va bildirishnoma ko'rsatuvchi. Eslatma vaqtini app moduli
  (`IslamicReminderScheduler`) AlarmManager'ga qo'yadi.

Namoz vaqtlari feature-prayer'dan `PrayerTimetablePort` orqali keladi (ulash: app-namoz-hifz `di/CalendarBridge.kt`).
Hijriy hisob: `core-common/hijri` (sof) + `core-data` (`IcuHijriProvider`, Umm al-Qura, ±kun tuzatish).
