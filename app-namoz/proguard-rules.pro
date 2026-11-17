# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class uz.xumoyun.**$$serializer { *; }
-keepclassmembers class uz.xumoyun.** { *** Companion; }
-keepclasseswithmembers class uz.xumoyun.** { kotlinx.serialization.KSerializer serializer(...); }

# AdMob SDK API 35 klasslariga murojaat qiladi (compileSdk 34 da yo'q).
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
