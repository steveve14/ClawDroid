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
