# ============================================================
# StreamHub ProGuard / R8 rules
# Applied only to release builds (debug uses no minification).
# ============================================================

# --- Preserve attributes needed for reflection & serialization ---
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin metadata (coroutines, reflection) ---
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler {*;}
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory {*;}

# --- Retrofit ---
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# --- Gson model classes (must preserve field names for JSON deserialization) ---
# Keep all API DTOs — Gson reads fields by name via reflection.
-keep class com.streamhub.app.data.api.** { *; }
# Keep all domain models — Firestore uses reflection too.
-keep class com.streamhub.app.data.models.** { *; }

# Per-field @SerializedName safety net
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- Firebase Firestore ---
-keep class com.google.firebase.firestore.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.internal.** { *; }
-dontwarn com.google.android.gms.**

# --- OkHttp / Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# --- Coil ---
-dontwarn coil.**

# --- Media3 (has consumer rules, but be explicit) ---
-dontwarn androidx.media3.**

# --- BuildConfig (keep only non-secret build metadata) ---
-keepclassmembers class com.streamhub.app.BuildConfig {
    static final boolean DEBUG;
    static final boolean DEBUG_LOGGING;
    static final int VERSION_CODE;
    static final java.lang.String VERSION_NAME;
}

# --- Compose runtime (rarely needed but safe) ---
-keep class androidx.compose.runtime.** { *; }

# --- Remove logging in release ---
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
}

# --- ZXing ---
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# --- bcrypt (at.favre.lib:bcrypt) — used by AdminManager for PIN verification ---
-keep class at.favre.lib.crypto.bcrypt.** { *; }
-dontwarn at.favre.lib.crypto.bcrypt.**

# --- StateFlow data classes used by Compose ---
-keep class com.streamhub.app.data.SubtitleConfig { *; }
-keep class com.streamhub.app.data.HomeLayoutConfig { *; }
-keep class com.streamhub.app.data.PlayerSettings { *; }
-keep class com.streamhub.app.data.SpeedTestState { *; }
-keep class com.streamhub.app.data.SpeedTestState$* { *; }
-keep class com.streamhub.app.data.UpdateState { *; }
-keep class com.streamhub.app.data.UpdateState$* { *; }
-keep class com.streamhub.app.data.UpdateInfo { *; }
-keep class com.streamhub.app.data.DownloadedItem { *; }
-keep class com.streamhub.app.data.telegram.ProxyConfig { *; }
# --- TDLib (Telegram Database Library) ---
# TDLib JNI C++ code directly accesses Java classes and fields by reflection
-keep class org.drinkless.tdlib.** { *; }
-keepclassmembers class org.drinkless.tdlib.** { *; }
-dontwarn org.drinkless.tdlib.**

-keep class com.streamhub.app.data.telegram.PublicProxyItem { *; }
-keep class com.streamhub.app.data.telegram.ProxyType { *; }
-keep class com.streamhub.app.ui.theme.AppThemeAccent { *; }


