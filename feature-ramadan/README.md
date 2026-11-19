# feature-ramadan

Ramazon menyusi (TZ 5, 6, 7, 8, 11-bo'limlar, 11-bosqich):

- `domain/` — sof Kotlin: `RamadanCalc` (imsok, iftor, Ramazon kuni, Xufton, qadr nomzodlari, 30 kunlik jadval),
  `FastCalc` (ro'za → qazo), `KhatmCalc` (kunlik juz), `RamadanReminderPlanner` (saharlik/iftor/qadr), portlar.
- `data/` — Room (`fast_log`, `khatm_plan`, DB v6), DataStore (`RamadanPrefs`), `RamadanRepository`.
- `ui/` — R1 bosh sahifa, R2 taqvim, R3 ro'za, R4 duolar, R5 xatm, R6 sozlamalar, Namoz tabidagi karta; ViewModel'lar;
  navigatsiya grafi `ramadanGraph`.
- `share/` — 1080×1920 PNG taqvim (Canvas), FileProvider orqali ulashish.
- `widget/` — Glance Ramazon vidjeti (Hilt EntryPoint).
- `notifications/` — "Saharlik" va "Iftor" kanallari; qadr eslatmasi "Islomiy sanalar" kanali ID'si bo'yicha.

Namoz vaqtlari `RamadanSchedulePort` (app `di/RamadanBridge.kt`), qazo `RamadanQadaPort` (QadaCounter FAST) orqali keladi.
Eslatmalarni app (`RamadanReminderScheduler`) AlarmManager'ga qo'yadi.
