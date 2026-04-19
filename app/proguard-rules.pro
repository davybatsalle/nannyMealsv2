# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*

# Firebase
-keepclassmembers class com.nannymeals.app.domain.model.** {
    *;
}
-keep class com.google.firebase.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn com.google.firebase.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# OpenCSV
-dontwarn com.opencsv.**

# iText PDF
-dontwarn com.itextpdf.**
