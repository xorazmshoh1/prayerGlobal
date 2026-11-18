# TZ: Namoz va Hifz

*18-noyabr, 2026 · @Xumoyun*

## 1. Qisqacha

**Namoz va Hifz** (ishchi nom): namoz vaqtlari, azon eslatmasi, qibla va tasbehni Qur'on yodlash rejasi bilan birlashtirgan, reklamasiz va bepul Android ilova.

- **Kim uchun:** kunlik namoz o'qiydigan va Qur'on yodlayotgan (yoki takrorlayotgan) musulmonlar, maktab va madrasa talabalari, ota-onalar.
- **Bozor:** global; birinchi navbatda Indoneziya, Malayziya, O'zbekiston, Turkiya, Pokiston, arab davlatlari.
- **Pul ishlash:** yo'q. Reklama, obuna va ichki xaridlar yo'q. Ilova sadaqa sifatida tarqatiladi.
- **Asosiy va'da:** to'liq internetsiz ishlaydi, shaxsiy ma'lumot qurilmadan chiqmaydi.

**Qabul qilingan taxminlar**

1. G'oyalar ro'yxatidagi 1 (namoz vaqtlari, qibla, tasbeh) va 10 (Qur'on yodlash kuzatuvchisi) bitta ilovaga birlashtirildi.
2. Tahlil (Firebase Analytics) ishlatilmaydi; faqat Crashlytics qoladi (8-bo'limga qarang).
3. Hisob (login) va server yo'q; zaxira nusxa faqat foydalanuvchining o'z fayliga eksport orqali.
4. Tillar: 11 ta, asosiysi ingliz (9-bo'lim). Qur'on qismida arabcha matn va audio, tarjima yo'q.

## 2. Fakt va format

Ilova namoz vaqtini internetsiz, astronomik formula bilan hisoblaydi; har bir mamlakat uchun standart hisoblash usuli avtomatik tanlanadi, foydalanuvchi uni o'zgartira oladi.

| Mavzu | Qoida yoki fakt | Ilovadagi qaror | Manba |
|---|---|---|---|
| Hisoblash kutubxonasi | Adhan: yuqori aniqlikdagi, sinovdan o'tgan, tashqi bog'liqliksiz kutubxona; MIT litsenziya | adhan-kotlin ishlatiladi, litsenziya "Litsenziyalar" ekranida ko'rsatiladi | [batoulapps/Adhan](https://github.com/batoulapps/Adhan) |
| Hisoblash usullari | MWL, ISNA, Misr, Karachi, Umm al-Qura, Dubay, Moonsighting Committee, Singapur, Turkiya, Tehron va boshqalar | Mamlakat → usul jadvali (pastda), qo'lda tanlash ham bor | Adhan |
| Asr | Hanafiy (soya 2×) va Shofeiy (soya 1×) | Mamlakatga qarab standart, sozlamada o'zgartiriladi | — |
| Yuqori kengliklar | 48° dan shimolda Bomdod/Xufton hisobi buziladi | "Kechaning 1/7 qismi" qoidasi standart, sozlamada boshqasi | Adhan |
| Qibla | Ka'ba: 21.4225° N, 39.8262° E; buyuk aylana bo'yicha azimut | Formula 7-bo'limda, kompas sensori bilan | — |
| Qur'on matni | Tanzil matnini o'zgartirmasdan nusxalash va tarqatish mumkin; manba ko'rsatilishi va tanzil.net'ga havola berilishi shart | Uthmani matn ilova ichida, o'zgartirilmaydi | [tanzil.net/download](https://tanzil.net/download) |
| Oyatlar soni | 114 sura, 6236 oyat (Hafs, Kufa sanog'i) | Progress foizi shu songa nisbatan | Tanzil metama'lumoti |

**Mamlakat bo'yicha standart usul** (taxmin, mahalliy jadval bilan solishtirib tekshiriladi)

| Mamlakat | Usul | Asr |
|---|---|---|
| Indoneziya, Malayziya, Singapur, Bruney | Singapur parametrlari (Bomdod 20°, Xufton 18°) | Shofeiy |
| O'zbekiston, Qozog'iston, Qirg'iziston, Tojikiston | MWL (ochiq savol: O'zbekiston musulmonlari idorasi jadvali bilan farqi) | Hanafiy |
| Turkiya | Turkiya (Diyanet) | Hanafiy |
| Pokiston, Hindiston, Bangladesh | Karachi | Hanafiy |
| Saudiya Arabistoni | Umm al-Qura | Shofeiy |
| Misr | Misr | Shofeiy |
| AQSh, Kanada | ISNA | Shofeiy |
| Qolganlari | MWL | Shofeiy |

## 3. Raqobatchilar va farqimiz

Bozorda namoz ilovalari ko'p, lekin ularning aksariyati reklama yoki pullik obuna bilan ishlaydi; namoz vaqtini hifz rejasi bilan bitta reklamasiz ilovada beradigani kam.

| Ilova | Kuchli tomoni | Kamchiligi (biz uchun imkoniyat) |
|---|---|---|
| Tarteel | Tilovatni AI bilan eshitib, xatoni darhol ko'rsatadi; Google Play'da 10 mln+ o'rnatish | Asosiy hifz funksiyalari Premium'da; namoz vaqti yo'q |
| Prayly | Namoz vaqti, Qur'on, qibla, tasbeh, namoz kuzatuvchi bitta ilovada | Ichki xaridlar bor; hifz rejasi yo'q |
| Prayer Times - Qibla & Salah (ZeeDev) | 2 mln+ o'rnatish, vidjetlar | Daromadning ko'p qismi reklamadan |
| Naima | Bepul, reklamasiz, internetsiz | Hifz qismi yo'q (namoz vaqti, azon, qibla) |

**Farqimiz**

1. Namoz vaqti va hifz bitta ilovada: "Bomdoddan keyin 5 oyat" kabi rejani namoz vaqtiga bog'lab eslatadi.
2. Butunlay bepul: reklama, obuna, Premium va to'lov devori yo'q.
3. Sabaq, sabqi va manzil usuli (yangi dars, yaqindagi takror, eski takror) va intervalli takrorlash.
4. Internetsiz ishlaydi: matn ilova ichida, audio bir marta yuklab olinadi.
5. Maxfiylik: login, server va kuzatuv yo'q, ma'lumot qurilmada qoladi.

Raqobatchilarning matnlari, tarjimalari, ikonkalari va dizayni nusxalanmaydi.

## 4. Kontent

Qur'on matni Tanzil'dan o'zgartirilmasdan olinadi va ilova ichida saqlanadi; audio tilovat internetdan bir marta yuklab olinadi; zikr va interfeys matnlarini o'zimiz yozamiz.

| Kontent | Manba | Hajmi | Yangilash | Litsenziya va shart |
|---|---|---|---|---|
| Arabcha matn (Uthmani) | Tanzil | 114 sura, 6236 oyat, taxminan 1.5 MB (taxmin) | Tanzil yangilansa, ilova versiyasi bilan | O'zgartirmaslik, manbani ko'rsatish, tanzil.net'ga havola |
| Sura, juz, sahifa ma'lumoti | Tanzil quran-data | 114 sura, 30 juz, 604 sahifa | Kerak bo'lmaydi | Tanzil metama'lumoti litsenziyasi |
| Oyatma-oyat audio | EveryAyah yoki Quran Foundation API | MVP'da 3 qori; 64 kbps'da bitta qori taxminan 0.5–1 GB (taxmin) | Serverdan, sura bo'yicha yuklab olinadi | Ochiq savol: har bir qori uchun ruxsatni tekshirish |
| Arab shrifti | Amiri Quran | 1 shrift fayli | — | SIL OFL 1.1, tijoratda ham ruxsat |
| Tasbeh zikrlari | O'zimiz yozamiz ("Subhanalloh", "Alhamdulillah" kabi umumiy zikrlar) | 10–15 ta zikr | Ilova versiyasi bilan | O'zimizniki |
| Azon ovozi | Ochiq savol: litsenziyali yozuv yoki ruxsat bilan olingan yozuv | 2–3 ta ovoz, har biri 30 soniyadan kam | Ilova versiyasi bilan | Har bir fayl uchun yozma ruxsat |
| Shaharlar bazasi | GeoNames cities15000 | Taxminan 30 000 shahar | Yiliga 1 marta | CC BY 4.0 (taxmin, tekshirish kerak) |

**Huquqiy ogohlantirish**

- Tanzil matnida birorta harf yoki belgi o'zgartirilmaydi; matnni tekshiradigan unit test yoziladi (7-bo'lim).
- Tarjima va tafsir MVP'ga kirmaydi: ularning ko'pchiligi mualliflik huquqi bilan himoyalangan.
- "Litsenziyalar va manbalar" ekranida Tanzil, Adhan, Amiri, GeoNames va qorilar nomi ko'rsatiladi.
- Ilova fatvo bermaydi: hisoblash usuli tanlov ekranida "mahalliy masjid jadvali bilan solishtiring" degan eslatma bo'ladi.

## 5. Funksiyalar

MVP to'rtta yo'nalishni qamraydi: namoz vaqti va azon, qibla, tasbeh, hifz. Hammasi internetsiz ishlaydi, faqat audio birinchi marta yuklab olinadi.

### MVP

1. Joylashuvni aniqlash (taxminiy GPS) yoki shaharni ro'yxatdan qo'lda tanlash.
2. Kunlik 6 vaqt: Bomdod, Quyosh, Peshin, Asr, Shom, Xufton; keyingi namozgacha qolgan vaqt.
3. Oylik jadval: hijriy va milodiy sanalar bilan.
4. Hisoblash usuli, Asr mazhabi, yuqori kenglik qoidasi va har bir namozga ± daqiqa tuzatish.
5. Azon bildirishnomasi: har bir namoz uchun ovozli, faqat bildirishnoma yoki o'chiq; namozdan 0–30 daqiqa oldin eslatma.
6. Bosh ekran vidjeti: keyingi namoz va qolgan vaqt.
7. Qibla kompasi: daraja, yo'nalish strelkasi va kalibrlash ko'rsatmasi.
8. Tasbeh: hisoblagich, maqsad (33/99/100), tebranish, zikrlar ro'yxati, kunlik tarix.
9. Namoz kuzatuvchisi: 5 namozni belgilash, haftalik ko'rinish. Qazo hisoblagich: har bir namoz (Hanafiy mazhabida Vitr ham) uchun qolgan qazo soni; boshlang'ich sonni qo'lda kiritish yoki sana oralig'idan taxminiy hisoblash; "Qazo o'qildi" tugmasi sonni 1 ga kamaytiradi; kuzatuvchida "o'qilmadi" deb belgilangan namoz avtomatik qo'shiladi (sozlamada o'chiriladi); kunlik qazo maqsadi va tugash sanasi prognozi.
10. Mus'haf o'qish: sura va juz bo'yicha, oyat raqamlari bilan.
11. Hifz rejasi: maqsad (sura, juz yoki butun Qur'on), kunlik yangi oyatlar soni, tugash sanasi prognozi.
12. Kunlik dars ekrani: Sabaq (yangi oyatlar), Sabqi (oxirgi 7 kun takrori), Manzil (eski takror, intervalli takrorlash bilan).
13. Audio takrorlash: bitta oyat yoki oraliqni N marta takrorlash, tezlik 0.75×–1.25×.
14. Yodni tekshirish: matnni yashirish, so'zlarni birma-bir ochish, natijani "oson / qiyin / unutdim" deb baholash.
15. Progress: yodlangan oyatlar soni va foizi, juzlar xaritasi, ketma-ket kunlar soni.
16. Hifz eslatmasi tanlangan namozdan keyin (masalan, Bomdoddan 15 daqiqa keyin).
17. Zaxira nusxa: JSON faylga eksport va import.
18. Yorug', qorong'i va avtomatik tema; 11 til.

### 2-versiya

1. To'liq azon foreground service orqali (Play Console'da alohida deklaratsiya kerak).
2. Tilovatni mikrofon orqali tekshirish (qurilmadagi nutqni aniqlash modeli).
3. Tarjimalar (litsenziya olingandan keyin).
4. Wear OS ilovasi va qulf ekrani vidjeti.
5. Ramazon rejimi: saharlik va iftor taymeri.
6. Ustoz rejimi: o'quvchi progressini faylda ulashish (server yo'q).

## 6. Ekranlar

Ilovada pastki navigatsiyada 4 ta tab (Namoz, Hifz, Qibla, Tasbeh) va 9 ta ichki ekran bor. Hech bir ekranda reklama yo'q, shuning uchun reklama ustuni olib tashlandi.

| # | Ekran | Asosiy elementlar |
|---|---|---|
| 1 | Onboarding (3 qadam) | Til tanlash, joylashuv (GPS yoki shahar qidirish), bildirishnoma ruxsati, hisoblash usulini tasdiqlash |
| 2 | Namoz (tab) | Shahar va sana (hijriy + milodiy), keyingi namoz kartasi va teskari sanoq, 6 vaqt ro'yxati, har qatorda ovoz belgisi, namozni belgilash tugmasi |
| 3 | Oylik jadval | 30 kunlik jadval, oy almashtirish, bugungi qator ajratilgan |
| 4 | Hifz (tab) | Bugungi dars: Sabaq / Sabqi / Manzil kartalari, ketma-ket kunlar, umumiy progress halqasi, "Darsni boshlash" tugmasi |
| 5 | Dars rejimi | Oyat matni, audio pleyer (takror soni, tezlik), "Matnni yashirish" rejimi, baholash tugmalari (oson / qiyin / unutdim) |
| 6 | Hifz rejasi | Maqsad tanlash (sura, juz, butun Qur'on), kunlik oyatlar soni, eslatma vaqti, tugash sanasi prognozi |
| 7 | Progress | Juzlar xaritasi (30 katak), sura ro'yxati holati bilan, haftalik grafik |
| 8 | Mus'haf | Sura va juz ro'yxati, qidiruv, o'qish ekrani, oyatdan hifzga qo'shish |
| 9 | Qibla (tab) | Kompas siferblati, Ka'ba belgisi, gradus, masofa (km), kalibrlash ogohlantirishi |
| 10 | Tasbeh (tab) | Katta hisoblagich tugmasi, maqsad, zikr tanlash, nolga qaytarish, kunlik tarix |
| 11 | Namoz kuzatuvchisi va qazo | Hafta ko'rinishi, har kun uchun 5 katak, ketma-ket kunlar; Qazo bo'limi: har namoz uchun qolgan son va −/+ tugmalari, umumiy qolgan son, kunlik maqsad, tugash sanasi, "Taxminiy hisoblash" ustasi (boshlanish va tugash sanasi, mazhab) |
| 12 | Audio boshqaruvi | Qori tanlash, sura bo'yicha yuklash va o'chirish, egallangan joy (MB) |
| 13 | Sozlamalar | Til, tema, hisoblash usuli, Asr, tuzatishlar, bildirishnomalar, azon ovozi, zaxira (eksport/import), Litsenziyalar va manbalar, Maxfiylik siyosati |

## 7. Ma'lumotlar modeli

Qur'on matni o'qish uchungina ishlatiladigan alohida `quran.db` faylida (assets orqali), foydalanuvchi ma'lumotlari esa `user.db` Room bazasida saqlanadi. Namoz vaqtlari bazaga yozilmaydi, har safar hisoblanadi.

### quran.db (faqat o'qish, createFromAsset)

| Entity | Maydonlar |
|---|---|
| Surah | id (1–114), nameArabic, nameTranslit, ayahCount, revelation (MECCAN/MEDINAN), startAyahGlobal |
| Ayah | globalId (1–6236), surahId, numberInSurah, juz (1–30), page (1–604), textUthmani |

### user.db (Room)

| Entity | Maydonlar |
|---|---|
| LocationEntity | id, name, lat, lon, timezoneId, countryCode, isManual |
| PrayerSettings | methodKey, madhab (SHAFI/HANAFI), highLatRule, adjustFajr…adjustIsha (daqiqa), perPrayerAlertMode (SOUND/SILENT/OFF), preReminderMin |
| PrayerLog | date (LocalDate), prayer (FAJR…ISHA), status (PRAYED/MISSED/QADA), updatedAt |
| HifzPlan | id, scope (SURAH/JUZ/ALL), fromAyah, toAyah, dailyNewAyahs, reminderAfterPrayer, reminderOffsetMin, startDate, isActive |
| AyahProgress | ayahGlobalId (PK), state (NEW/LEARNING/MEMORIZED), easeFactor, intervalDays, repetitions, dueDate, lastRating, lastReviewedAt |
| ReviewSession | id, date, type (SABAQ/SABQI/MANZIL), ayahFrom, ayahTo, durationSec, avgRating |
| TasbihEntry | id, dhikrKey, count, target, date |
| AudioDownload | reciterKey, surahId, bytes, downloadedAt |

Qazo uchun qo'shimcha: **QadaCounter** — prayer (FAJR, DHUHR, ASR, MAGHRIB, ISHA, WITR), remaining, initialEstimate, dailyGoal, updatedAt; **QadaEvent** — id, prayer, delta (+1 yoki −1), source (MANUAL / AUTO_MISSED / WIZARD), createdAt (bekor qilish va tarix uchun).

### Formulalar

Qibla azimuti (φ — foydalanuvchi kengligi, λ — uzunligi, φk = 21.4225°, λk = 39.8262°):

```
θ = atan2( sin Δλ · cos φk,  cos φ · sin φk − sin φ · cos φk · cos Δλ ),   Δλ = λk − λ
```

Natija 0–360° ga keltiriladi: (θ + 360) mod 360. Ka'bagacha masofa haversine bilan, R = 6371 km.

Intervalli takrorlash (SM-2 soddalashtirilgan; baho q: oson = 5, qiyin = 3, unutdim = 1):

```
EF' = max(1.3, EF + 0.1 − (5 − q)(0.08 + 0.02(5 − q)))
```

- q < 3 bo'lsa: repetitions = 0, interval = 1 kun.
- Aks holda: 1-takror → 1 kun, 2-takror → 6 kun, keyin interval = round(oldingi interval × EF').
- interval ≥ 30 kun va oxirgi baho ≥ 4 bo'lsa, holat MEMORIZED.

Tugash sanasi = bugun + ceil(qolgan oyatlar / dailyNewAyahs) kun. Progress = MEMORIZED oyatlar / rejadagi oyatlar × 100%.

Qazo taxminiy hisobi: har bir namoz uchun boshlang'ich son = (tugash sanasi − boshlanish sanasi) kunlar soni; Vitr faqat Hanafiy mazhabida qo'shiladi. Qazo tugash sanasi = bugun + ceil(umumiy qolgan son / (dailyGoal × namozlar soni)) kun. Ilova fatvo bermaydi: usta oxirida "aniq sonni olimdan so'rang" eslatmasi chiqadi.

### Unit testlar

- Qibla: Toshkent (41.2995, 69.2401) → 240.3°; Jakarta (−6.2088, 106.8456) → 295.2°; London (51.5074, −0.1278) → 119.0° (±1°).
- Namoz vaqtlari: Adhan test ma'lumotlari (adhan-testdata) bilan ±1 daqiqa; har bir mamlakat standarti uchun 1 ta shahar.
- SM-2: q = 5 ketma-ket 3 marta → interval 1, 6, 17 kun (EF 2.6, 2.7, 2.8); q = 1 → interval 1, repetitions 0; EF hech qachon 1.3 dan kam emas.
- Qur'on: oyatlar soni 6236, suralar 114; Fotiha matnining SHA-256 xeshi Tanzil faylidagi bilan bir xil.
- Zaxira: eksport qilib import qilganda barcha jadvallar bir xil qoladi.
  - Qazo: 2020-01-01 dan 2020-12-31 gacha (366 kun) Shofeiy → har namozga 366, Vitr yo'q; Hanafiy → Vitr ham 366; remaining 0 bo'lganda −1 bosilsa 0 qoladi; 1830 qazo, kuniga 2 tadan (5 namoz) → 183 kun.

## 8. Texnik talablar

Ilova minSdk 24 va targetSdk 36 bilan quriladi: Google Play [31-avgust 2026 dan yangi ilova va yangilanishlardan Android 16 (API 36) talab qiladi](https://developer.android.com/google/play/requirements/target-sdk).

**Texnologiyalar:** Kotlin, Jetpack Compose + Material 3, MVVM + Repository, Hilt, Room, DataStore, Navigation Compose, WorkManager, AlarmManager, Media3 ExoPlayer, adhan-kotlin, Firebase Crashlytics. AdMob, UMP, Analytics va Billing ulanmaydi.

**Modullar:** `app-namoz-hifz`, `core-ui`, `core-data`, `core-common`, `feature-prayer`, `feature-hifz`, `feature-qibla`, `feature-tasbih`.

| Ruxsat | Nima uchun | Izoh |
|---|---|---|
| ACCESS_COARSE_LOCATION | Namoz vaqti va qibla uchun shahar darajasidagi joylashuv | Aniq (FINE) va fon joylashuvi so'ralmaydi; rad etilsa, shahar qo'lda tanlanadi |
| POST_NOTIFICATIONS | Azon va hifz eslatmalari (Android 13+) | Onboardingda tushuntirish bilan so'raladi |
| SCHEDULE_EXACT_ALARM | Azonni aniq daqiqada chalish | USE_EXACT_ALARM ishlatilmaydi: Play uni faqat soat va kalendar ilovalariga ruxsat beradi. Ruxsat berilmasa, setWindow bilan ±5 daqiqa |
| RECEIVE_BOOT_COMPLETED | Qayta yoqilgandan keyin alarmlarni tiklash | — |
| INTERNET | Faqat audio yuklash va Crashlytics | — |
| VIBRATE | Tasbeh va bildirishnoma | — |

**Bildirishnomalar va fon ishlari**

- Har bir namoz uchun alohida bildirishnoma kanali (5 ta) va "Hifz eslatmasi" kanali; azon ovozi kanal ovozi sifatida beriladi (30 soniyadan kam).
- Har kuni yarim tunda WorkManager keyingi 2 kunlik alarmlarni qayta hisoblaydi; vaqt mintaqasi, sana va joylashuv o'zgarganda ham qayta hisoblanadi.
- Audio yuklash: WorkManager, faqat Wi-Fi (sozlamada o'zgartiriladi), sura bo'yicha, uzilganda davom etadi.
- Kompas: TYPE_ROTATION_VECTOR, yo'q bo'lsa akselerometr + magnitometr; aniqlik past bo'lsa "8 shaklida aylantiring" ko'rsatmasi.

**Internetsiz ishlash:** namoz vaqtlari, qibla, tasbeh, mus'haf matni, hifz rejasi va progress internetsiz ishlaydi. Internet faqat audio yuklash uchun kerak.

**Unumdorlik:** sovuq ishga tushish 1.5 soniyadan kam; APK (AAB'dan) 25 MB dan kichik (taxmin); 16 KB sahifa hajmini qo'llab-quvvatlash.

**Crashlytics:** faqat xato hisobotlari, foydalanuvchi identifikatorisiz; sozlamada o'chirib qo'yish mumkin.

## 9. Lokalizatsiya

Interfeys 11 tilda, asosiy til ingliz; Qur'on matni har doim arabcha, tillar faqat interfeys va zikr tarjimasiga taalluqli.

| Til | Kod | RTL |
|---|---|---|
| Ingliz (asosiy) | en | yo'q |
| Arab | ar | ha |
| O'zbek (lotin) | uz | yo'q |
| Rus | ru | yo'q |
| Indonez | id | yo'q |
| Malay | ms | yo'q |
| Turk | tr | yo'q |
| Urdu | ur | ha |
| Fors | fa | ha |
| Bengal | bn | yo'q |
| Fransuz | fr | yo'q |

- **RTL:** `supportsRtl="true"`, Compose'da `start/end` ishlatiladi; ar, ur, fa uchun barcha ekranlarning skrinshot testi.
- **Sana:** hijriy sana `android.icu.util.IslamicCalendar` (Umm al-Qura varianti) bilan, ±1 kun qo'lda tuzatish; milodiy sana `DateTimeFormatter` bilan joriy locale'da.
- **Raqamlar:** arab tilida sharqiy arab raqamlari (٠–٩), fors va urduda fors raqamlari (۰–۹), boshqalarda lotin raqamlari; oyat raqamlari doim arabcha.
- **Vaqt formati:** qurilma sozlamasiga qarab 12 yoki 24 soat.
- **Namoz nomlari:** har til uchun mahalliy nom (masalan uz: Bomdod, Peshin, Asr, Shom, Xufton; id: Subuh, Dzuhur, Ashar, Maghrib, Isya).
- **Play sahifasi:** sarlavha (30 belgi), qisqa tavsif (80 belgi), to'liq tavsif va 4 ta skrinshot 11 tilda; tarjimani ona tilida so'zlashuvchi tekshiradi (ochiq savol: kim tekshiradi).

## 10. Reklama siyosati

Ilovada reklama yo'q va bo'lmaydi: banner, interstitial, rewarded va App Open ishlatilmaydi, reklama SDK'lari loyihaga qo'shilmaydi.

- Play Console'da "Contains ads" = **Yo'q**.
- Ichki xarid, obuna va xayriya so'rovi ham yo'q (Play'ning to'lov siyosatini buzmaslik uchun ilova ichida tashqi to'lov havolasi ham bo'lmaydi).
- CI'da tekshiruv: `com.google.android.gms:play-services-ads` yoki boshqa reklama kutubxonasi bog'liqliklar daraxtida bo'lsa, build xato bilan to'xtaydi.

## 11. Dizayn

Dizayn sokin va ixcham: to'q yashil asosiy rang, oltin urg'u, ko'p bo'sh joy, bezaksiz; matn va vaqtlar birinchi o'rinda.

| Element | Yorug' tema | Qorong'i tema |
|---|---|---|
| Asosiy (primary) | #0F5C4D | #6FCFB4 |
| Urg'u (accent) | #C9A227 | #E2C062 |
| Fon | #FAF8F3 | #101614 |
| Sirt (karta) | #FFFFFF | #1A2320 |
| Asosiy matn | #1B1F1D | #E8ECEA |
| Ikkilamchi matn | #5C6662 | #9AA6A1 |

**Shriftlar:** interfeys uchun Noto Sans (lotin, kirill), Noto Sans Arabic, Noto Nastaliq Urdu va Noto Sans Bengali; Qur'on uchun Amiri Quran. O'lchamlar: sarlavha 22 sp, keyingi namoz vaqti 40 sp, oddiy matn 16 sp, izoh 13 sp; Qur'on matni 26 sp (20–40 sp oralig'ida sozlanadi), qator oralig'i 1.8.

**Asosiy ekranlarning joylashuvi**

- **Namoz:** tepada shahar va ikkala sana; o'rtada katta karta: keyingi namoz nomi, vaqti va teskari sanoq; ostida 6 qator (nom chapda, vaqt o'ngda, ovoz belgisi oxirida); joriy namoz qatori primary rangda ajratilgan.
- **Hifz:** tepada progress halqasi (foiz o'rtada) va ketma-ket kunlar; ostida uchta karta (Sabaq, Sabqi, Manzil) oyat oralig'i va taxminiy daqiqalar bilan; pastda keng "Darsni boshlash" tugmasi.
- **Dars rejimi:** ekranning 70% qismida oyat matni (o'ngdan chapga, markazda); pastda pleyer paneli (takror soni, tezlik, oldinga/orqaga); eng pastda uchta baholash tugmasi.
- **Qibla:** markazda siferblat, Ka'ba belgisi aylana chetida; strelka qiblaga to'g'ri kelganda siferblat urg'u rangga o'tadi va qisqa tebranadi; ostida gradus va km.
- **Tasbeh:** ekranning yarmini egallagan dumaloq tugma, ichida son; tepada zikr matni (arabcha + transliteratsiya), ostida maqsad indikatori.

**Ikonka:** to'q yashil fonda oltin rangli sodda mehrob arkasi va uning ichida ochiq kitob silueti; matn va real masjid tasviri yo'q. Adaptiv ikonka va monoxrom (themed) varianti.

## 12. Google Play talablari

Ilova kam ma'lumot yig'adi, shuning uchun Data safety deklaratsiyasi qisqa bo'ladi. Eng uzun bosqich esa yopiq test: shaxsiy akkaunt uchun [12 tester 14 kun davomida uzluksiz](https://support.google.com/googleplay/android-developer/answer/14151465).

| Talab | Qaror |
|---|---|
| Data safety | Taxminiy joylashuv: qurilmada ishlatiladi, serverga yuborilmaydi. Crash loglari: Crashlytics'ga yuboriladi, uchinchi tomonga sotilmaydi, shifrlangan uzatiladi. Boshqa ma'lumot yig'ilmaydi |
| Maxfiylik siyosati | GitHub Pages'da statik sahifa (masalan `https://<username>.github.io/namoz-hifz/privacy`), 11 tilda; havola ilova sozlamalarida ham bo'ladi |
| Kontent reytingi | IARC so'rovnomasi; kutilgan natija: Everyone / 3+ |
| Maqsadli yosh | 13+ (bolalarga mo'ljallangan emas, shunda Families siyosati qo'llanmaydi) |
| Ruxsatlar deklaratsiyasi | SCHEDULE_EXACT_ALARM uchun ilova ichida tushuntirish; USE_EXACT_ALARM, fon joylashuvi va foreground service MVP'da yo'q |
| Nom va brend | Rasmiy idoralar (masalan, diniy idoralar, JAKIM, Kemenag, Diyanet) nomi va logotipi ishlatilmaydi; "rasmiy" so'zi yo'q |
| Reklama | "Contains ads" = Yo'q |
| Target API | targetSdk 36 |
| Yopiq test | 12+ tester, 14 kun uzluksiz; keyin production'ga ariza (taxminan 3 hafta rejalashtiriladi) |
| Litsenziyalar | "Litsenziyalar va manbalar" ekranida: Tanzil (havola bilan), Adhan (MIT), Amiri Quran (OFL), GeoNames, qorilar |

## 13. Ish bosqichlari

Ish 10 bosqichga bo'lingan, har biri bitta Claude Code sessiyasiga sig'adi va oxirida build hamda testlar o'tishi shart.

- [ ] 1. Loyiha skeleti: modullar, Hilt, Compose tema (11-bo'lim ranglari), navigatsiya, 4 ta bo'sh tab, CI (build + test + reklama SDK tekshiruvi)
- [ ] 2. Namoz yadrosi: adhan-kotlin, mamlakat → usul jadvali, PrayerSettings (DataStore), joylashuv va shahar qidiruvi, Namoz ekrani, oylik jadval, unit testlar
- [ ] 3. Bildirishnomalar: kanallar, AlarmManager, SCHEDULE_EXACT_ALARM bilan zaxira yo'l, boot va vaqt mintaqasi qabul qiluvchilari, kunlik WorkManager, vidjet
- [ ] 4. Qibla va tasbeh: kompas, formula va testlar, tasbeh ekrani va tarix
- [ ] 5. Qur'on bazasi: Tanzil'dan quran.db yaratish skripti, Room createFromAsset, mus'haf ekrani, Amiri Quran, matn butunligi testlari
- [ ] 6. Audio: qorilar ro'yxati, WorkManager yuklash, Media3 pleyer, oyat takrori va tezlik, audio boshqaruvi ekrani
- [ ] 7. Hifz yadrosi: HifzPlan, AyahProgress, SM-2, kunlik Sabaq/Sabqi/Manzil generatori, unit testlar
- [ ] 8. Hifz UI: Hifz tab, dars rejimi (matnni yashirish, baholash), reja ekrani, progress va juzlar xaritasi, namozdan keyingi eslatma
- [ ] 9. Namoz kuzatuvchisi va qazo hisoblagich, zaxira (eksport/import), onboarding, sozlamalar, Litsenziyalar ekrani, Crashlytics
- [ ] 10. Lokalizatsiya (11 til, RTL), accessibility, unumdorlik, release build (R8, imzo), Play sahifasi materiallari

## 14. Claude Code promptlari

Har bir promptdan oldin shu TZ'ni loyihaga `docs/TZ.md` qilib qo'ying; promptlar unga havola qiladi. Loyihada bu promptlarni `/dev <N>` buyrug'i (`.claude/commands/dev.md`) `dev` agentiga beradi.

### 1-bosqich: skelet

```
docs/TZ.md ni o'qi. Kotlin + Jetpack Compose (Material 3) Android loyihasini yarat: modullar app-namoz-hifz, core-ui, core-data, core-common, feature-prayer, feature-hifz, feature-qibla, feature-tasbih. minSdk 24, targetSdk 36, compileSdk 36. Hilt, Navigation Compose, DataStore ulansin. 11-bo'limdagi HEX ranglar bilan yorug' va qorong'i tema yarat. Pastki navigatsiyada 4 ta tab (Namoz, Hifz, Qibla, Tasbeh) bo'sh ekranlar bilan. GitHub Actions: assembleDebug, testDebugUnitTest va bog'liqliklarda reklama SDK (play-services-ads va boshqalar) bo'lsa xato beradigan tekshiruv. Reklama, Billing va Analytics qo'shma.
```

### 2-bosqich: namoz yadrosi

```
docs/TZ.md 2, 5, 7-bo'limlariga qara. feature-prayer modulida adhan-kotlin bilan namoz vaqti hisoblash repozitoriyasini yoz. Mamlakat kodi bo'yicha standart usul va Asr mazhabi 2-bo'limdagi jadvaldan. PrayerSettings DataStore'da (usul, mazhab, yuqori kenglik qoidasi, har namozga ±daqiqa). Joylashuv: ACCESS_COARSE_LOCATION, rad etilsa assets'dagi GeoNames shaharlar bazasidan qidiruv. Namoz ekrani va oylik jadval 6 va 11-bo'limdagidek. Unit testlar: har bir standart usul uchun 1 shahar, ±1 daqiqa.
```

### 3-bosqich: bildirishnomalar

```
docs/TZ.md 8-bo'limiga qara. 5 ta namoz kanali va Hifz kanali yarat. AlarmManager bilan azon: canScheduleExactAlarms() true bo'lsa setExactAndAllowWhileIdle, aks holda setWindow (±5 daqiqa) va sozlamaga o'tish uchun tushuntirish ekrani. USE_EXACT_ALARM ishlatma. BOOT_COMPLETED, TIMEZONE_CHANGED, TIME_SET da alarmlarni qayta o'rnat. Kunlik WorkManager keyingi 2 kunni hisoblasin. Keyingi namoz va qolgan vaqtni ko'rsatadigan Glance vidjeti. Testlar: alarm rejalashtiruvchi uchun fake clock bilan.
```

### 4-bosqich: qibla va tasbeh

```
docs/TZ.md 7-bo'limidagi qibla formulasi va haversine'ni core-common'da yoz, 7-bo'limdagi Toshkent, Jakarta, London testlari bilan. feature-qibla: TYPE_ROTATION_VECTOR (yo'q bo'lsa akselerometr + magnitometr), past aniqlikda kalibrlash ko'rsatmasi, 11-bo'limdagi joylashuv. feature-tasbih: hisoblagich, maqsad 33/99/100, tebranish, zikrlar ro'yxati, TasbihEntry Room jadvali va kunlik tarix.
```

### 5-bosqich: Qur'on bazasi

```
docs/TZ.md 4 va 7-bo'limlariga qara. tools/ papkasida Tanzil quran-uthmani.txt va quran-data.xml dan quran.db (Surah, Ayah jadvallari) yaratadigan Kotlin/JVM skript yoz; matnni hech qanday o'zgartirma. Room createFromAsset bilan ulab, mus'haf ekranini (sura/juz ro'yxati, qidiruv, o'qish, Amiri Quran shrifti, o'lcham 20–40 sp) yasa. Testlar: 114 sura, 6236 oyat, Fotiha SHA-256 xeshi manba fayl bilan bir xil.
```

### 6-bosqich: audio

```
docs/TZ.md 4, 5, 8-bo'limlariga qara. 3 ta qori konfiguratsiyasi (URL shabloni konfiguratsiyada, kodga qattiq yozilmasin). WorkManager bilan sura bo'yicha yuklash (standart: faqat Wi-Fi, uzilganda davom etadi), AudioDownload jadvali. Media3 ExoPlayer: oyat yoki oraliqni N marta takrorlash, tezlik 0.75–1.25. Audio boshqaruvi ekrani: yuklash, o'chirish, MB hisobi.
```

### 7-bosqich: hifz yadrosi

```
docs/TZ.md 5 va 7-bo'limlariga qara. feature-hifz: HifzPlan, AyahProgress, ReviewSession Room entity'lari. 7-bo'limdagi SM-2 formulasini toza Kotlin funksiyasi sifatida yoz. Kunlik dars generatori: Sabaq = keyingi dailyNewAyahs ta yangi oyat, Sabqi = oxirgi 7 kunda o'rganilganlar, Manzil = dueDate <= bugun bo'lgan MEMORIZED/LEARNING oyatlar (eng ko'pi 1 juz). Tugash sanasi prognozi. 7-bo'limdagi SM-2 testlari va generator uchun testlar.
```

### 8-bosqich: hifz UI

```
docs/TZ.md 6 va 11-bo'limlariga qara. Hifz tab (progress halqasi, ketma-ket kunlar, 3 karta), dars rejimi (matn, pleyer, matnni yashirish va so'zlarni birma-bir ochish, oson/qiyin/unutdim baholash), reja ekrani, progress ekrani (30 katakli juz xaritasi, haftalik grafik). Hifz eslatmasi: tanlangan namozdan X daqiqa keyin, 3-bosqichdagi alarm infratuzilmasi orqali.
```

### 9-bosqich: qolgan ekranlar

```
docs/TZ.md 5, 6, 12-bo'limlariga qara. Namoz kuzatuvchisi (PrayerLog, hafta ko'rinishi). Qazo hisoblagich: QadaCounter va QadaEvent jadvallari, -/+ tugmalari, bekor qilish, taxminiy hisoblash ustasi, kunlik maqsad va tugash sanasi; PrayerLog'da MISSED belgilansa QadaCounter +1 (sozlamada o'chiriladi). 7-bo'limdagi qazo testlarini yoz. Zaxira: barcha user.db jadvallari va DataStore'ni JSON'ga eksport/import (SAF orqali), round-trip testi bilan. 3 qadamli onboarding. Sozlamalar ekrani. Litsenziyalar va manbalar ekrani (Tanzil havolasi bilan, Adhan MIT, Amiri OFL, GeoNames, qorilar). Crashlytics: foydalanuvchi ID'siz, sozlamada o'chiriladi.
```

### 10-bosqich: lokalizatsiya va reliz

```
docs/TZ.md 9 va 12-bo'limlariga qara. Barcha matnlarni strings.xml ga chiqar va 11 tilga tarjima qil (en asosiy). RTL: ar, ur, fa uchun Compose skrinshot testlari. Hijriy sana IslamicCalendar bilan, ±1 kun tuzatish. Raqamlar locale bo'yicha. TalkBack yorliqlari, kontrast tekshiruvi. Release: R8, imzo konfiguratsiyasi (kalitlarni repoga qo'yma), AAB. Play sahifasi uchun 11 tilda matnlar fastlane/metadata papkasida.
```

### Reliz tekshiruvi

- [ ] Barcha unit va UI testlar o'tadi; CI reklama SDK tekshiruvi yashil
- [ ] 8 ta shaharda namoz vaqtlari mahalliy jadval bilan ±2 daqiqa ichida
- [ ] Azon telefon qayta yoqilgandan keyin, Doze rejimida va vaqt mintaqasi o'zgarganda chaladi
- [ ] Exact alarm ruxsati berilmaganda zaxira yo'l ishlaydi
- [ ] Internetsiz rejimda barcha funksiyalar ishlaydi (audio yuklashdan tashqari)
- [ ] Qur'on matni testlari o'tadi; Litsenziyalar ekranida Tanzil havolasi bor
- [ ] ar, ur, fa'da RTL to'g'ri; 11 tilda bo'sh yoki kesilgan matn yo'q
- [ ] Data safety, maxfiylik siyosati URL, kontent reytingi, "Contains ads = Yo'q" to'ldirilgan
- [ ] targetSdk 36, 16 KB sahifa hajmi tekshirilgan
- [ ] 12 tester bilan 14 kunlik yopiq test o'tkazilgan

## Manbalar

- [Adhan (MIT)](https://github.com/batoulapps/Adhan)
- [Tanzil: yuklab olish va foydalanish shartlari](https://tanzil.net/download)
- [Quran Foundation: dasturchilar shartlari](https://api-docs.quran.foundation/)
- [Amiri Quran (OFL 1.1)](https://github.com/aliftype/amiri)
- [Google Play: target API talablari](https://developer.android.com/google/play/requirements/target-sdk)
- [Android: aniq alarmlar](https://developer.android.com/develop/background-work/services/alarms/schedule)
- [Google Play: yangi shaxsiy akkauntlar uchun test talablari](https://support.google.com/googleplay/android-developer/answer/14151465)
- Tarteel, Prayly, Naima
