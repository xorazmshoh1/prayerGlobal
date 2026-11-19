# tools

## build-quran-db: Tanzil matnidan `quran.db`

Alohida, Android'siz Kotlin/JVM Gradle loyiha. Tanzil'dan olingan `quran-uthmani.txt` va `quran-data.xml`
dan ilova ichidagi `quran.db` (Surah va Ayah jadvallari, TZ 7-bo'lim) ni yaratadi. Matn o'zgartirilmaydi:
har bir oyat baytma-bayt saqlanadi (faqat satr oxiridagi CR va fayl boshidagi BOM olib tashlanadi).

### 1. Tanzil fayllarini yuklab olish

Manba: <https://tanzil.net/download>. Kerakli ikki fayl:

- **quran-uthmani.txt** — Uthmani matni, format `sura|oyat|matn`
- **quran-data.xml** — sura nomlari, oyat soni, Makka/Madina, juz va sahifa chegaralari

Tanzil matnini ishlatish shartlari (manba ko'rsatish, tanzil.net havolasi, o'zgartirmaslik) tanzil.net'dagi
litsenziya bo'limida yozilgan — ulardan xabardor bo'ling. Fayllarni repoga **qo'shmang**.

### 2. quran.db yaratish

```bash
cd tools/build-quran-db
gradle run --args="--uthmani /yo'l/quran-uthmani.txt --data /yo'l/quran-data.xml \
  --out ../../feature-hifz/src/main/assets/quran.db --room-schema ../../feature-hifz/schemas/uz.xumoyun.feature.hifz.data.QuranDatabase/1.json"
```

`--room-schema` (yoki `--room-hash <hex>`) — Room'ning `identityHash` qiymati. U `feature-hifz/schemas/` dagi
JSON'da bo'ladi, ya'ni avval loyihani bir marta kompilyatsiya qiling (`./gradlew :feature-hifz:kspDebugKotlin`).
Bu qiymat yozilmasa `room_master_table` yaratilmaydi va Room bazani ochishda xato beradi.

`quran.db` `.gitignore` da: uni har bir build muhitida yarating (yoki CI'da Tanzil fayllaridan generatsiya qiling).
Fayl yo'q bo'lsa ilova buzilmaydi — mus'haf "Qur'on matni o'rnatilmagan" xabarini ko'rsatadi.

### 3. Testlar

```bash
cd tools/build-quran-db
gradle test                                   # sintetik testlar (format, sxema, hajm)
gradle test -Ptanzil.dir=/yo'l/tanzil-papka   # + haqiqiy Tanzil testlari
gradle test -Ptanzil.dir=... -Pquran.fatihaSha256=<hex>   # + Fotiha xeshi tekshiruvi
```

- Sintetik testlarning matni `test-S-O` kabi belgilar; ular Qur'on emas, faqat format va sxemani tekshiradi.
- Haqiqiy testlar (114 sura, 6236 oyat, juz 1–30, sahifa 1–604, matn baytma-bayt tengligi) fayl yo'q bo'lsa
  **SKIPPED: Tanzil fayli yo'q** deb chiqadi — ular yashirin o'tmaydi.
- Fotiha xeshi: 1-suraning 7 oyat matni UTF-8 baytlarida `"\n"` bilan ulangan (oxirida `"\n"` yo'q), SHA-256.
  Kutilgan qiymat TZ'da yo'q (`TODO(TZ-ochiq-savol)`): birinchi marta test hisoblangan qiymatni konsolga chiqaradi,
  uni ishonchli manbadan tekshirib `-Pquran.fatihaSha256` ga bering.

### Ochiq nuqtalar

- `quran-data.xml` ichidagi juz/sahifa elementlari (`<juz index sura aya>`, `<page index sura aya>`) formati
  sintetik fixture bo'yicha yozilgan; haqiqiy fayl bilan birinchi ishga tushirishda tekshiring (noto'g'ri bo'lsa
  xato matni atribut nomini ko'rsatadi).
- Matn yangilanganda ilova ichidagi `databases/quran.db` nusxasi avtomatik almashmaydi: `QuranDatabase.version`
  ni oshirish va eski nusxani o'chirish kerak (feature-hifz/README.md).
