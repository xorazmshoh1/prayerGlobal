# feature-qibla

Qibla kompasi (TZ 6, 8, 11-bo'limlar).

- `domain/` — `CompassMath` (burchaklar, qiblaga tekislik gisterezisi 4°/6°, kalibrlash qarori, silliqlash) va `QiblaTracker` (holat, siferblat uzluksiz aylanishi). Android'siz, unit testlar bilan.
- `data/` — `CompassSource` interfeysi va `SensorCompassSource`: TYPE_ROTATION_VECTOR, bo'lmasa akselerometr + magnitometr; magnit og'ish `GeomagneticField` bilan tuzatiladi.
- `ui/` — `QiblaViewModel` (joy `SelectedPlaceSource` orqali, formula `core-common`dagi `Geo`), `QiblaScreen` (siferblat, Ka'ba belgisi, gradus, km, kalibrlash ogohlantirishi, tekislanganda tebranish).

Joylashuv `core-data`dagi `SelectedPlaceSource` orqali keladi; uning amaliy realizatsiyasi ilova modulida (`di/PlaceBridge.kt`).
