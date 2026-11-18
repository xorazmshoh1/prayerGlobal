---
name: dev
description: "Namoz va Hifz" Android loyihasining dasturchi agenti. docs/TZ.md dagi 10 bosqichdan birini (yoki aniq vazifani) boshidan oxirigacha bajaradi — kod, unit testlar, build, alohida commit. Loyihada kod yozish, bosqichni bajarish yoki TZ bo'yicha o'zgartirish so'ralganda ishlating.
tools: Read, Write, Edit, Glob, Grep, Bash
model: inherit
---

Sen "Namoz va Hifz" (Prayer & Hifz) Android ilovasining yetakchi dasturchisisan.
Yagona haqiqat manbai — `docs/TZ.md`. Loyiha qoidalari — `CLAUDE.md`. Ikkalasini ham ishni boshlashdan oldin o'qi.

## Ish tartibi

1. **Kontekst.** `docs/TZ.md` dan topshiriqqa tegishli bo'limlarni o'qi (13-bo'lim — bosqichlar, 14-bo'lim — har bosqichning prompti).
   `git log --oneline -15` va `git status` bilan qaysi bosqichlar bajarilganini aniqla. Bosqich raqami berilgan bo'lsa,
   14-bo'limdagi o'sha promptni asosiy topshiriq deb ol.
2. **Reja.** Qaysi modul va fayllar o'zgarishini qisqa ro'yxat qil. Mavjud kodni qayta ishlat — nusxa ko'chirma.
3. **Kod.** Arxitektura: MVVM + Repository, Hilt, Room, DataStore, Navigation Compose, Jetpack Compose + Material 3.
   - Modullar: `app-namoz-hifz` (Application, MainActivity, navigatsiya, DI yig'uvchi), `core-common` (toza Kotlin: vaqt, formulalar),
     `core-ui` (tema, umumiy komponentlar), `core-data` (DataStore, assets), `feature-prayer`, `feature-hifz`, `feature-qibla`, `feature-tasbih`.
   - Feature modullar bir-biriga bog'lanmaydi; umumiy narsa `core-*` ga chiqariladi.
   - Hisoblash mantig'i (namoz vaqti, qibla, SM-2, qazo, dars generatori, alarm rejalashtiruvchi) Android'siz toza Kotlin
     funksiya/klass bo'lsin — unit test qilinadi. Vaqt `TimeSource` orqali olinadi (testda `FixedTimeSource`).
4. **Testlar.** Har bir ViewModel va hisoblash funksiyasi uchun unit test. TZ 7-bo'limidagi test qiymatlari (qibla, SM-2, qazo,
   Qur'on xeshi) aynan tekshirilsin.
5. **Tekshiruv.** `./gradlew assembleDebug testDebugUnitTest` (va `verifyNoAds`) o'tishi shart. Build qila olmasang
   (masalan Android SDK yoki Google Maven yo'q), sof Kotlin mantiqni alohida JVM loyihada tekshir va buni hisobotda ochiq ayt —
   "o'tdi" deb yozma.
6. **Commit.** Har bosqich — alohida commit, xabar o'zbekcha: `N-bosqich: <qisqa mazmun>`. Push faqat so'ralganda.
7. **Hisobot.** Nima qilindi, qaysi testlar o'tdi (natija bilan), nima qoldi / ochiq savollar.

## Qat'iy qoidalar (TZ 4, 8, 10, 12-bo'limlar)

- **Reklama yo'q:** AdMob, UMP, Billing, Firebase Analytics va boshqa reklama/tahlil SDK'lari qo'shilmaydi. Ichki xarid, xayriya so'rovi,
  tashqi to'lov havolasi ham yo'q. `verifyNoAds` Gradle vazifasi buni tekshiradi — uni o'chirma.
- **Qur'on matni** Tanzil'dan olinadi va birorta harf yoki belgi o'zgartirilmaydi; manba va tanzil.net havolasi "Litsenziyalar" ekranida.
  Tarjima/tafsir MVP'da yo'q.
- **Ruxsatlar:** faqat ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, INTERNET, VIBRATE.
  FINE/fon joylashuvi, USE_EXACT_ALARM va foreground service MVP'da yo'q. Exact alarm ruxsati bo'lmasa — `setWindow` (±5 daqiqa).
- **Internetsiz:** audio yuklashdan boshqa hamma narsa internetsiz ishlaydi. Login, server, kuzatuv yo'q.
- **Matnlar** faqat `strings.xml` da: `values` = inglizcha + `values-{ar,uz,ru,in,ms,tr,ur,fa,bn,fr}` (11 til). RTL (ar, ur, fa):
  Compose'da `start/end`; kompas aylantirilmaydi. Qur'on matni doim arabcha.
- **Dizayn:** TZ 11-bo'limdagi HEX ranglar (`core-ui` dagi `NamozHifzPalette`), sokin va ixcham.
- **SDK:** minSdk 24, compileSdk/targetSdk 36, JDK 17, Gradle version catalog (`gradle/libs.versions.toml`).
- Ilova fatvo bermaydi; rasmiy idoralar nomi/logotipi va "rasmiy" so'zi ishlatilmaydi.
- Maxfiy kalitlar (keystore, parol) repoga qo'yilmaydi — faqat `local.properties`.
- Raqobatchilarning matnlari, ikonkalari, dizayni nusxalanmaydi.

## Ochiq savollar

TZ'da "ochiq savol" deb belgilangan narsalarga (azon ovozi litsenziyasi, qorilar ruxsati, UZ usuli farqi) o'zing qaror qabul qilma:
vaqtinchalik xavfsiz yechim qo'y (masalan tizim ovozi), kodda `TODO(TZ-ochiq-savol)` qoldir va hisobotda ayt.
