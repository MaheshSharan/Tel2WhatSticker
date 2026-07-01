# ProGuard rules for Tel2What Sticker Converter

# Keep native methods (critical for JNI bridge to libwebp)
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Room database entities and DAOs (prevents runtime crashes)
-keep class com.maheshsharan.tel2what.data.local.** { *; }
-keep interface com.maheshsharan.tel2what.data.local.dao.** { *; }

# Keep Lottie animation classes
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# Keep Glide image loading classes
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# Keep ContentProvider for WhatsApp integration (critical for sticker export)
-keep class com.maheshsharan.tel2what.provider.StickerContentProvider { *; }

# Keep Telegram API models and parsers
-keep class com.maheshsharan.tel2what.data.network.model.** { *; }

# Keep conversion engine result types (used for pattern matching)
-keep class com.maheshsharan.tel2what.engine.StickerConversionResult { *; }
-keep class com.maheshsharan.tel2what.engine.StickerConversionResult$* { *; }

# Keep OkHttp platform classes
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep serialization classes
-keepattributes *Annotation*, InnerClasses
-keepattributes Signature, Exception

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
