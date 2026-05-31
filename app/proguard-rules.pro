# Add project specific ProGuard rules here.
# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Gson
-keep class com.google.gson.** { *; }
-keep class pw.mng.nexoraid.api.** { *; }
-keep class pw.mng.nexoraid.provider.** { *; }
-keep class pw.mng.nexoraid.data.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Room
-dontwarn androidx.room.**
-keep class androidx.room.** { *; }

# Markwon
-keep class io.noties.markwon.** { *; }

# Security Crypto / Tink
-dontwarn com.google.errorprone.annotations.**