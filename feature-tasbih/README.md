# feature-tasbih

Tasbeh: hisoblagich, maqsad 33/99/100 (yoki ixtiyoriy), zikrlar, tebranish, nolga qaytarish va kunlik tarix (TZ 5, 6, 8, 11-bo'limlar).

- `domain/` — `Dhikr`, `TasbihCounter` (maqsadga yetish, halqa, davralar), `DailyHistory` (kunlik yig'indi). Android'siz.
- `data/` — `TasbihEntry` (Room jadvali `tasbih_entry`: id, date, dhikrKey, count, target), `TasbihDao`, `TasbihSettings` (DataStore).
  Reset yoki zikr almashganda yangi seans qatori ochiladi, eski qatorlar o'chmaydi.
- `ui/` — `TasbihViewModel` (seans va tarix), `TasbihScreen`.

Room bazasi (`NamozDatabase`) ilova modulida; jadval o'zgarishi `MIGRATION_1_2` (`app-namoz-hifz/.../data/db/Migrations.kt`) bilan ko'chiriladi.
