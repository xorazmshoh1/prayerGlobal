# feature-prayer

Namoz yadrosi (TZ 2, 5, 6, 7, 11-bo'limlar, 2-bosqich):

- `domain/` — sof Kotlin (Android'siz): `PrayerCalculator` (adhan), `MethodTable` (assets/methods.json), `Schedule`,
  `PrayerBoard` (Namoz ekrani holati), `CityIndex` (assets/cities.json, GeoNames).
- `data/` — `PrayerSettings` (DataStore), `PrayerRepository`, `SavedLocation`/`LocationDao` (Room bazasi ilova modulida),
  `AndroidLocationProvider` (faqat ACCESS_COARSE_LOCATION).
- `ui/` — Namoz tabi (`PrayerRoute`), joylar va shahar qidiruvi (`LocationsScreen`), umumiy matn yordamchilari (`PrayerLabels`).

Bildirishnomalar va vidjet (3-bosqich), oylik jadval (10-bosqich) hozircha `app-namoz-hifz` da va shu modul API'sidan foydalanadi.
