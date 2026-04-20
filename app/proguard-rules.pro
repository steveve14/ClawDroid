# Add project specific ProGuard rules here.

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.clawdroid.core.model.** { *; }
-keep class com.clawdroid.core.data.db.entity.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }

# Markwon (commonmark)
-keep class org.commonmark.** { *; }
-dontwarn org.commonmark.**
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# SEC-L2: Hilt / Dagger — 방어적 keep 룰
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.** { *; }
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.InstallIn class *
-keep @dagger.Module class *
-keep @javax.inject.Singleton class *
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}
-dontwarn dagger.hilt.**
-dontwarn javax.inject.**

# SEC-H1: SQLCipher — 네이티브 심볼 보존
-keep class net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**
-keep class net.sqlcipher.** { *; }
-dontwarn net.sqlcipher.**

# Security crypto / Tink
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# RxJava
-dontwarn java.util.concurrent.Flow*

# Room
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**
