# Loyiha: Prayer Times: Azan & Qibla (3-ilova)

Global namoz vaqtlari ilovasi: internetsiz hisoblash (adhan), qibla, tasbeh, oylik taqvim, bildirishnomalar, Glance vidjeti, 7 til (en, ar, id, tr, ur, uz, ru).

## Texnologiyalar
Kotlin 2.0, Jetpack Compose, Material 3, MVVM, Hilt, Room, DataStore,
Navigation Compose, Google Mobile Ads (AdMob), UMP, Jetpack Glance, adhan (Java) 1.2.1.
minSdk 24, compileSdk/targetSdk 34, Gradle version catalog (`gradle/libs.versions.toml`).

## Modullar
- `core-common` — TimeSource, DurationFormat, ThemeMode
- `core-ui` — Material 3 tema (AppPalette, AppTheme), umumiy komponentlar, AppLocale
- `core-data` — DataStore, AssetSource, CommonSettings (tema, til, onboarding)
- `core-ads` — AdsManager (UMP, banner, interstitial, rewarded, app open), chastota qoidalari
- `app-namoz` — ilovaning o'zi

Umumiy kod faqat `core-*` modullarida.

## Qoidalar
- Barcha matnlar `strings.xml` da: `values` = inglizcha, `values-{ar,in,tr,ur,uz,ru}`. RTL (ar, ur) qo'llab-quvvatlanadi, kompas aylantirilmaydi.
- Faqat AdMob TEST ID lari; haqiqiy ID lar `local.properties` dan (faqat release).
- Diniy ilova: interstitial va app open YO'Q, faqat banner (bosh sahifa, oylik taqvim) va ixtiyoriy rewarded (sozlamalar → "Mualliflarni qo'llab-quvvatlash"). AdMob'da qimor, spirtli ichimlik, tanishuv, astrologiya toifalarini bloklang.
- Har bir ViewModel va hisoblash funksiyasi uchun unit test.
- Har bir bosqichdan keyin build va testlar o'tishi shart, keyin alohida commit.

## Build (JDK 17)
```bash
JAVA_HOME="/c/Users/HP/.jdks/corretto-17.0.5" ./gradlew assembleDebug testDebugUnitTest
```

## Muhim fayllar
- `app-namoz/src/main/assets/methods.json` — davlat → hisoblash usuli va Asr mazhabi
- `app-namoz/src/main/assets/cities.json` — shaharlar (`tools/geonames-to-json.mjs` bilan GeoNames'dan qayta yaratiladi)
- `app-namoz/.../domain/PrayerCalculator.kt` — hisoblash; `UZBEKISTAN` usuli parametrlari rasmiy taqvim bilan solishtirib sozlanadi
- `docs/prayer-tables/` — 7 sinov shahri uchun oylik jadval (unit test yaratadi)

## Texnik topshiriq
To'liq TZ: docs/TZ.md
