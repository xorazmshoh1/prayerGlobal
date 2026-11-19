# feature-hifz

Hifz rejasi, AyahProgress, SM-2, Sabaq/Sabqi/Manzil generatori va UI (TZ 5, 6, 7, 11-bo'limlar).

## Mus'haf (5-bosqich)

- `data/` — `quran.db` ning Room sxemasi (`QuranEntities.kt`: `surah`, `ayah`), `QuranDao`, `QuranDatabase`
  (`createFromAsset("quran.db")`), `RoomQuranSource` (domain `QuranSource` implementatsiyasi), Hilt moduli.
  Sxema `tools/build-quran-db` dagi `QuranDbBuilder` bilan bir xil bo'lishi shart.
- `domain/` — sof Kotlin: `QuranCatalog` (sura qidiruvi, arabcha/lotincha kalit, oyat belgisi ﴿١﴾,
  shrift 20–40 sp, sukut 26 sp) va modellar. Unit testlar: `src/test/.../domain/QuranCatalogTest.kt`.
- `ui/` — `MushafViewModel` (sura/juz ro'yxati, qidiruv), `ReaderViewModel` (sura yoki juz o'qish,
  shrift o'lchami `SavedStateHandle` da), `MushafScreen`, `ReaderScreen`, `QuranFont`.
- `MushafNavigation.kt` — `mushafGraph(navController)`; ilova `NamozRoot` ga ulangan, Hifz tabidagi tugma orqali ochiladi.

### quran.db

Fayl repoda yo'q (`.gitignore`). Yaratish: `tools/README.md`. Fayl `src/main/assets/quran.db` ga tushadi.
Fayl bo'lmasa mus'haf "Qur'on matni o'rnatilmagan" xabarini ko'rsatadi, ilova ishlayveradi.
Matn yangilanganda `QuranDatabase.version` ni oshiring (ilova ichidagi eski nusxa almashishi uchun).

### Amiri Quran shrifti

`src/main/res/font/amiri_quran.ttf` (Amiri Quran, SIL OFL 1.1) repoda YO'Q — uni qo'lda qo'shing
(<https://github.com/aliftype/amiri>). Fayl bo'lsa `QuranFont` uni avtomatik ishlatadi (resurs nomi bo'yicha),
bo'lmasa tizim serif shrifti ishlatiladi. Shrift qo'shilgach, Litsenziyalar ekraniga Amiri (OFL) yozilishi kerak (9-bosqich).

### Hozircha yo'q (keyingi bosqichlar)

- Hifz yadrosi (HifzPlan, AyahProgress, SM-2) va "Hifzga qo'shish" tugmasining haqiqiy ishi — 7–8-bosqichlar.
  Hozir tugma faqat callback va "keyingi yangilanishda" xabari.
- Hifz tabining kunlik dars ekrani — 8-bosqich.

## Bildirishnomalar

"Hifz eslatmasi" bildirishnoma kanali (`HifzNotifications.kt`) 3-bosqichda qo'shilgan; eslatmaning o'zi — 8-bosqich.
