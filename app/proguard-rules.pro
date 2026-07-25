# ProGuard/R8 规则 - 逆向 Prompt 翻译器

# 保留 WebView 相关类
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# 保留 JavaScript 接口
-keepattributes JavascriptInterface

# 保留 WebView
-keep class android.webkit.** { *; }
-keep class android.webkit.WebView { *; }
-keep class android.webkit.WebViewClient { *; }
-keep class android.webkit.WebChromeClient { *; }

# 保留 AndroidX WebKit
-keep class androidx.webkit.** { *; }

# 移除 Log.d 和 Log.v（Release 版本）
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

# 优化
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# 混淆
-repackageclasses ''
-allowaccessmodification
-optimizationpasses 5

# 保留行号（用于调试）
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 保留注解
-keepattributes *Annotation*

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留 R 文件
-keep class **.R$* { *; }

# 保留 BuildConfig
-keep class com.reversetool.prompttranslator.BuildConfig { *; }

# 保留 MainActivity
-keep class com.reversetool.prompttranslator.MainActivity { *; }
