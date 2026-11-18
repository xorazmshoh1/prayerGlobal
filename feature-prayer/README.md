# feature-prayer

Namoz yadrosi (TZ 2, 5, 6, 7, 11-bo'limlar, 2-bosqich):

- `domain/` — sof Kotlin (Android'siz): `PrayerCalculator` (adhan), `MethodTable` (assets/methods.json), `Schedule`,
  `PrayerBoard` (Namoz ekrani holati), `CityIndex` (assets/cities.json, GeoNames).
- `data/` — `PrayerSettings` (DataStore), `PrayerRepository`, `SavedLocation`/`LocationDao` (Room bazasi ilova modulida),
  `AndroidLocationProvider` (faqat ACCESS_COARSE_LOCATION).
- `ui/` — Namoz tabi (`PrayerRoute`), joylar va shahar qidiruvi (`LocationsScreen`), umumiy matn yordamchilari (`PrayerLabels`).

Bildirishnomalar va vidjet (TZ 8-bo'lim, 3-bosqich):

- `domain/AlarmPlanner` — sof Kotlin: hozirdan 2 kun ichidagi signallar, SOUND/SILENT/OFF va 0–30 daqiqa eslatma,
  aniq signal ruxsati bo'lmasa `setWindow` oynasi, barqaror request kodlari, vidjetni yangilash vaqti. `DailyReschedule` — kunlik ish vaqti.
- `notifications/` — `PrayerChannels` (5 namoz kanali + eslatmalar), `PrayerAlarmScheduler` (rejani AlarmManager'ga qo'yadi),
  `PrayerAlarmReceiver`, `PrayerRescheduleReceiver` (BOOT_COMPLETED, TIMEZONE_CHANGED, TIME_SET, MY_PACKAGE_REPLACED,
  SCHEDULE_EXACT_ALARM ruxsati), `DailyRescheduleWorker` (WorkManager), `ExactAlarms` / `NotificationAccess`.
- `widget/PrayerWidget` — Glance vidjeti: keyingi namoz, teskari sanoq (Chronometer), bugungi vaqtlar.
- `ui/NotificationSettingsScreen` — har vaqt uchun rejim va eslatma, ruxsat kartalari.
- Receiver va vidjet deklaratsiyalari `src/main/AndroidManifest.xml` da (ilova manifestiga birlashtiriladi).

Oylik jadval (10-bosqich) hozircha `app-namoz-hifz` da va shu modul API'sidan foydalanadi.
