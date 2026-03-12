# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Production optimizations - ENABLE obfuscation for security
# Commented out the lines that disable optimization and obfuscation
# -dontoptimize
# -dontobfuscate

# Enable aggressive optimizations for production
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Keep FLIR SDK classes
-keep class com.flir.** { *; }
-dontwarn com.flir.**

# Keep Retrofit and Gson classes
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep database entities
-keep class com.solarsnap.app.database.entities.** { *; }
-keep class com.solarsnap.app.network.models.** { *; }

# Keep Room database classes
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
