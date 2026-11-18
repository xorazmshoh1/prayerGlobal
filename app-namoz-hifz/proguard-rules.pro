# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class uz.xumoyun.**$$serializer { *; }
-keepclassmembers class uz.xumoyun.** { *** Companion; }
-keepclasseswithmembers class uz.xumoyun.** { kotlinx.serialization.KSerializer serializer(...); }

