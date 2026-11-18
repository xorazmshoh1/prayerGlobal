# Loyiha: Namoz va Hifz

Reklamasiz, bepul, internetsiz Android ilova: namoz vaqtlari va azon (adhan), qibla, tasbeh, namoz kuzatuvchisi va qazo,
mus'haf (Tanzil matni), Qur'on yodlash rejasi (Sabaq / Sabqi / Manzil, SM-2), hijriy-milodiy taqvim va Ramazon menyusi
(saharlik/iftor, ro'za kuzatuvchisi, duolar, xatm). 11 til (en, ar, uz, ru, id, ms, tr, ur, fa, bn, fr).

To'liq TZ: `docs/TZ.md` (13-bo'lim — 12 ta ish bosqichi, 14-bo'lim — har bosqichning prompti).

## Dev agenti
- `/dev <N>` — TZ'ning N-bosqichini `dev` agenti (`.claude/agents/dev.md`) orqali bajaradi; `/dev` argumentsiz — keyingi bosqich.
- `/dev <matn>` — aniq vazifa.

## Texnologiyalar
Kotlin 2.0, Jetpack Compose, Material 3, MVVM + Repository, Hilt, Room, DataStore, Navigation Compose,
WorkManager, AlarmManager, Media3 ExoPlayer, Jetpack Glance, adhan 1.2.1, Play In-App Review (12-bosqich),
Firebase Crashlytics (9-bosqich).
minSdk 24, compileSdk/targetSdk 36, Gradle version catalog (`gradle/libs.versions.toml`).
**AdMob, UMP, Billing, Analytics — yo'q.**

## Modullar
- `app-namoz-hifz` — Application, MainActivity, navigatsiya (5 tab: Namoz, Taqvim, Hifz, Qibla, Tasbeh + Ramazon menyusi grafi), DI
- `core-common` — TimeSource, DurationFormat, ThemeMode, toza Kotlin formulalar
- `core-ui` — Material 3 tema (`NamozHifzPalette`, AppTheme), umumiy komponentlar, AppLocale
- `core-data` — DataStore, AssetSource, CommonSettings (tema, til, onboarding)
- `feature-prayer`, `feature-calendar`, `feature-hifz`, `feature-qibla`, `feature-tasbih`, `feature-ramadan` — funksiya modullari

Umumiy kod faqat `core-*` modullarida; feature modullar bir-biriga bog'lanmaydi.
Hozircha namoz, oylik jadval, qibla va tasbeh kodi `app-namoz-hifz` ichida — 2-, 4- va 10-bosqichlarda tegishli feature modullarga ko'chiriladi.

## Qoidalar
- Barcha matnlar `strings.xml` da: `values` = inglizcha, `values-{ar,uz,ru,in,ms,tr,ur,fa,bn,fr}`. RTL (ar, ur, fa) qo'llab-quvvatlanadi, kompas aylantirilmaydi.
- Reklama, ichki xarid, xayriya so'rovi yo'q (review-ktx va Crashlytics ruxsat). `verifyNoAds` Gradle vazifasi reklama/billing/analytics SDK'lari bog'liqliklarda bo'lsa build'ni to'xtatadi.
- Qur'on matni Tanzil'dan o'zgartirilmasdan; tarjima/tafsir yo'q.
- Ruxsatlar: COARSE_LOCATION, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, INTERNET, VIBRATE (FINE, USE_EXACT_ALARM yo'q).
- Har bir ViewModel va hisoblash funksiyasi uchun unit test.
- Har bir bosqichdan keyin build va testlar o'tishi shart, keyin alohida commit.

## Build (JDK 17)
```bash
./gradlew assembleDebug testDebugUnitTest verifyNoAds
```
Windows (lokal): `JAVA_HOME="/c/Users/HP/.jdks/corretto-17.0.5" ./gradlew ...`

## Muhim fayllar
- `app-namoz-hifz/src/main/assets/methods.json` — davlat → hisoblash usuli va Asr mazhabi (TZ 2-bo'lim)
- `app-namoz-hifz/src/main/assets/cities.json` — shaharlar (`tools/geonames-to-json.mjs` bilan GeoNames'dan qayta yaratiladi)
- `app-namoz-hifz/.../domain/PrayerCalculator.kt` — hisoblash
- `docs/prayer-tables/` — sinov shaharlari uchun oylik jadval (unit test yaratadi)
- `.github/workflows/ci.yml` — CI: build, unit testlar, reklama SDK tekshiruvi
