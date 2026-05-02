# 紫微演算法層全部保留（反射可能訪問 enum）
-keep class com.zwds.today.astro.** { *; }
-keep class com.zwds.today.domain.model.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room (KSP 一般已處理，保險起見)
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
